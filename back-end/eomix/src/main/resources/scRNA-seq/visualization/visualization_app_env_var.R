# Environment variables to test outside Docker ####
analysis_name <- "Adamts19-14-08-2025_14-31-01"  # For example

Sys.setenv("AWS_ACCESS_KEY_ID" = "YOU_MINIO_ACCESS_KEY",
           "AWS_SECRET_ACCESS_KEY" = "YOUR_MINIO_SECRET_KEY",
           "AWS_DEFAULT_REGION" = "",
           "AWS_S3_ENDPOINT" = "http://host.docker.internal:9000",
           "AWS_S3_DISABLE_SSL" = "true",
           "ANALYSIS_NAME" = analysis_name)
# setwd(file.path("back-end",
#                 "eomix",
#                 "src",
#                 "main",
#                 "resources",
#                 "scRNA-seq",
#                 "visualization"))
rm(analysis_name)

source (
  "/Users/Jerome/Development/e-omix_app/back-end/eomix/src/main/resources/utils/gene_conversion.R"
  )

# runApp(app,
#        host = "127.0.0.1", port = 3838,
#        launch.browser	= TRUE)