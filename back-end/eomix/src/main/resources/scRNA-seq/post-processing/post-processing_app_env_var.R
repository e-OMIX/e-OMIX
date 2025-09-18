analysis_name <- "Adamts19-14-08-2025_14-31-01"

Sys.setenv(AWS_ACCESS_KEY_ID="YOU_MINIO_ACCESS_KEY")
Sys.setenv(AWS_SECRET_ACCESS_KEY="YOUR_MINIO_SECRET_KEY")
Sys.setenv(AWS_S3_ENDPOINT="host.docker.internal:9000")
Sys.setenv(AWS_S3_DISABLE_SSL='true' )
Sys.setenv(AWS_DEFAULT_REGION='' )
Sys.setenv(POSTPROCESSING_FOLDER_NAME = analysis_name)
