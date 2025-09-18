package com.example.eomix.service;

import com.example.eomix.exception.ProcessInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The type Remote script executor.
 */
@Service
public class RemoteScriptExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RemoteScriptExecutor.class);

    /**
     * Execute remote python script.
     * This method executes a remote Python script on a specified server using SSH.
     * * It constructs a command to run the script with the provided JSON file path as an argument.
     * * It uses Runtime.exec() to execute the command and sets up threads to read the output and error streams of
     * the process.
     * * The method does not block the main thread and allows the process to run in the background.
     *
     * @param pathToJSONInMinio the path to json in minio
     * @throws IOException the io exception
     * @implNote The method logs the command being executed and captures both standard output and error output from
     * the Python script.
     * @implSpec If the process starts successfully, it checks the exit code after a short delay to ensure the script
     * is running. * This method is useful for executing long-running Python scripts remotely without blocking the
     * main application thread.
     */
    public void executeRemotePythonScript(String pathToJSONInMinio) throws IOException {
        String[] command = {"python3", "src/main/resources/scRNA-seq/alignment/wrapper_pipeline.py",
                pathToJSONInMinio};
        if (logger.isInfoEnabled()) {
            logger.info("Executing remote Python script with command: {}", String.join(" ", command));
        }
        Process process = Runtime.getRuntime().exec(command);
        // Setup threads to log output/errors (but don't block)
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[PYTHON OUTPUT] {}", line);
                }
            } catch (IOException e) {
                logger.error("Error reading process output", e);
            }
        }).start();

        new Thread(() -> {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    logger.error("[PYTHON ERROR] {}", line);
                }
            } catch (IOException e) {
                logger.error("Error reading process error", e);
            }
        }).start();

        // Check if process started successfully (quick check)
        try {
            // Small delay to detect immediate failures
            Thread.sleep(1000);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("Process failed immediately with exit code: " + exitCode);
            }
        } catch (IllegalThreadStateException e) {
            // Process is still running - this is good
            logger.info("Process is running successfully in background");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessInterruptedException("Thread was interrupted while waiting for process to start", e);
        } catch (IOException e) {
            throw new ProcessInterruptedException("Process failed to start or was interrupted", e);
        }
    }

}
