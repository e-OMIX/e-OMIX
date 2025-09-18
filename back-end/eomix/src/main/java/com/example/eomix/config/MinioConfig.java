package com.example.eomix.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Minio config.
 */
@Configuration
public class MinioConfig {

    @Value("${minio.access.key}")
    private String accessKey;

    @Value("${minio.secret.key}")
    private String secretKey;

    @Value("${minio.url}")
    private String minioUrl;

    /**
     * Minio client .
     * * This method creates a MinioClient bean that can be used to interact with the MinIO server.
     * * It uses the access key, secret key, and MinIO server URL defined in the application properties.
     *
     * @return the minio client
     * @implNote The MinioClient is configured with the endpoint and credentials to connect to the MinIO server.
     * @implSpec The MinioClient bean can be injected into other components to perform operations like uploading, downloading, and managing files in the MinIO storage.
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}
