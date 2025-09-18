package com.example.eomix.controller;

import com.example.eomix.service.VisualizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The type Visualization controller.
 */
@RestController
@RequestMapping("/public/visualization")
public class VisualizationController {
    private static final Logger logger = LoggerFactory.getLogger(VisualizationController.class);
    private final ExecutorService scriptExecutorService = Executors.newCachedThreadPool();
    private final RestTemplate restTemplate;

    /**
     * Instantiates a new Visualization controller.
     *
     * @param restTemplate the rest template
     */
    public VisualizationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Start visualization and return a completable future.
     * * This method starts the visualization process by running a Docker container with the specified experiment name.
     * * It uses the VisualizationService to run the Docker container and returns a completable future with the response entity.
     *
     * @param experimentName the experiment name
     * @return the completable future
     */
    @PostMapping("/start")
    public CompletableFuture<ResponseEntity<Map<String, String>>> startVisualization(@RequestBody String experimentName) {
        return CompletableFuture.supplyAsync(() -> VisualizationService.runVisualizationDocker(experimentName), scriptExecutorService);

    }

    /**
     * Stop visualization and return a completable future.
     * * This method stops the visualization process by stopping the Docker container with the specified containerId.
     * * It uses the VisualizationService to stop the Docker container and returns a completable future with the response entity.
     *
     * @param payload the payload that contains the containerId
     * @return the completable future
     */
    @PostMapping("/stop")
    public CompletableFuture<ResponseEntity<String>> stopVisualization(@RequestBody Map<String, String> payload) {
        String containerId = payload.get("containerId");
        if (containerId == null || containerId.trim().isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("containerId is required."));
        }

        return CompletableFuture.supplyAsync(() -> VisualizationService.stopVisualizationDocker(containerId), scriptExecutorService);
    }

    /**
     * Check shiny app status by URL and return a completable future.
     * * This method checks the status of a Shiny app by making a GET request to the provided URL.
     * * It returns a completable future with the response entity indicating whether the app is ready or not.
     *
     * @param url the url of the Shiny app to check
     * @return the completable future
     */
    @GetMapping("/status")
    public CompletableFuture<ResponseEntity<String>> checkShinyStatus(@RequestParam String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check the exact URL provided by the frontend
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return ResponseEntity.ok("READY");
                } else {
                    logger.warn("Shiny app returned non-2xx status: {}", response.getStatusCode());
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("NOT_READY");
                }
            } catch (Exception e) {
                // Connection refused, etc.
                logger.error("Error checking Shiny app status: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("NOT_READY" + " - " + e.getMessage());
            }
        }, scriptExecutorService);
    }
}
