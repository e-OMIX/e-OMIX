package com.example.eomix.service;

import com.example.eomix.exception.DockerException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

/**
 * Visualization service.
 *
 * @author Molka Anaghim FTOUHI
 */
public class VisualizationService {
    private static final Logger logger = LoggerFactory.getLogger(VisualizationService.class);

    /**
     * Private constructor to prevent instantiation.
     * This class is a utility class and should not be instantiated.
     */
    private VisualizationService() {
    }

    /**
     * Runs a visualization Docker container on a new port each time and returns a response entity.
     * <p>
     * This method starts a Docker container to visualize the specified experiment.
     * It constructs and executes a Docker run command with the given experiment name,
     * captures the container ID from the process output,
     * and determines the dynamically assigned host port for the Shiny app inside the container.
     * It then builds a URL to access the Shiny app and waits for the app to become ready before returning.
     *
     * @param experimentName the name of the experiment to visualize
     * @return a {@link ResponseEntity} containing the container ID and the Shiny app URL if successful;
     * otherwise, an error response with relevant error details
     * @implNote The method uses {@link ProcessBuilder} to execute Docker commands,
     * captures stdout and stderr, and parses port mappings from Docker.
     * It includes error handling for failures during container startup or port retrieval,
     * returning informative error responses in such cases.
     * @implSpec The method waits for the Shiny app to be accessible before returning,
     * ensuring users can connect immediately via the provided URL.
     */
    public static @NotNull ResponseEntity<Map<String, String>> runVisualizationDocker(String experimentName) {
        String[] command = getDockerRunCommand(experimentName);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            // Read the container ID from the output stream
            String containerId;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                if (logger.isInfoEnabled()) {
                    logger.info("Starting Docker container with command: {}", String.join(" ", command));
                }
                containerId = reader.readLine();
            }

            if (containerId == null || containerId.trim().isEmpty()) {
                // Handle error if container failed to start
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String error = errorReader.lines().collect(Collectors.joining("\n"));
                    logger.error("Docker run failed: {}", error);
                    throw new DockerException("Failed to start Docker container: " + error);
                }
            }

            containerId = containerId.trim();

            // Wait a brief moment for the port mapping to be established
            Thread.sleep(1000); // Small delay

            // Command to get the dynamically assigned port
            Process portProcess = new ProcessBuilder("docker", "port", containerId, "3838").start();
            String portMapping;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(portProcess.getInputStream()))) {
                logger.info("Retrieving port mapping for container {}", containerId);
                portMapping = reader.readLine();
                logger.info("Port mapping {}", portMapping);
            }

            if (portMapping == null || !portMapping.contains(":")) {
                logger.error("Could not determine port mapping for container {}", containerId);
                throw new DockerException("Could not determine port for container " + containerId);
            }

            String port = portMapping.split(":")[1];
            String shinyUrl = "http://localhost:" + port;
            logger.info("Container {} is accessible at {}", containerId, shinyUrl);

            //  Wait for the Shiny app to be ready
            waitForShinyApp(shinyUrl);

            // Prepare the response for the frontend
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("containerId", containerId);
            responseBody.put("shinyUrl", shinyUrl);
            logger.info("Started container {} on port {}", containerId, port);


            return ResponseEntity.ok(responseBody);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while starting visualization: {}", e.getMessage());
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "Visualization start interrupted: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        } catch (Exception e) {
            logger.error("Exception starting visualization: {}", e.getMessage());
            // Return an error response
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "Failed to start visualization: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /**
     * Polls the Shiny app URL until it's ready.
     * <p>
     * This method repeatedly attempts to connect to the Shiny app URL
     * until it receives a successful HTTP response.
     * It logs the attempts and waits for a short period between retries.
     *
     * @param shinyUrl the URL of the Shiny app to check
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws DockerException      if the Shiny app does not become ready within the timeout period
     * @implNote The method uses a timeout to prevent infinite loops,
     * ensuring that it will stop trying after a certain period.
     * @implSpec <ul>
     * <li>If the Shiny app does not become ready within the timeout period, a DockerException is thrown.</li>
     * <li> This method is crucial for ensuring that the Shiny app is fully initialized before the user attempts to
     * access it,
     * preventing errors or incomplete loading states.</li>
     * </ul>
     */
    private static void waitForShinyApp(String shinyUrl) throws InterruptedException {
        logger.info("Waiting for Shiny app to be ready at: {}", shinyUrl);
        long startTime = System.currentTimeMillis();
        long timeout = 60000; // 60-second timeout to prevent infinite loops

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new java.net.URI(shinyUrl).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(2000); // 2-second connection timeout
                connection.setReadTimeout(2000); // 2-second read timeout

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 400) {
                    logger.info("Shiny app is ready! (HTTP  {} )", responseCode);
                    return;
                }
            } catch (Exception e) {
                logger.debug("Connection attempt failed (attempt {}ms): {}", System.currentTimeMillis() - startTime,
                        e.getMessage());
            }
            sleep(2000); // Wait 2 seconds before retrying
        }
        throw new DockerException("Shiny app did not become ready within the timeout period.");
    }

    private static String @NotNull [] getDockerRunCommand(String experimentName) {
        String path = System.getProperty("visualization.docker.script.path", "src/main/resources/scRNA-seq/visualization/visualization_docker_run_script.sh");
        return new String[]{"sh", path, experimentName};
    }

    /**
     * Stops a running Docker container using the provided container ID and returns a response entity.
     * <p>
     * This method executes the `docker stop` command to gracefully stop the specified container.
     * It ensures that the container is stopped properly and returns a response indicating success or failure.
     *
     * @param containerId the ID of the Docker container to stop
     * @return a {@link ResponseEntity} with a status and message indicating whether the container was stopped
     * successfully or if an error occurred
     * @implNote The method uses a {@link ProcessBuilder} to run the Docker stop command,
     * handles exceptions and errors during the process execution,
     * and logs relevant information for debugging and error tracking.
     * @implSpec <ul>
     * <li>If the container fails to stop or an error occurs, the method returns an error response with appropriate
     * details.</li>
     * <li>This method is useful for cleaning up Docker resources after visualization sessions to prevent resource
     * leakage.</li>
     * <li>Captures and logs exceptions during the stop process for improved diagnostics.</li>
     * <li>Returns a response entity containing both an HTTP status code and a descriptive message about the
     * operation outcome.</li>
     * </ul>
     */

    public static @NotNull ResponseEntity<String> stopVisualizationDocker(String containerId) {
        try {
            // Simple, reliable stop command. --rm from the start command handles removal.
            Process process = new ProcessBuilder("docker", "stop", containerId.trim()).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Successfully stopped container: {}", containerId);
                return ResponseEntity.ok("Container stopped successfully.");
            } else {
                logger.error("Failed to stop container {} with exit code {}", containerId, exitCode);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to stop container.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while stopping container {}: {}", containerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Interrupted while stopping container" +
                    ".");
        } catch (Exception e) {
            logger.error("Exception stopping container {}: {}", containerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


}
