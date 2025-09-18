# MinIO-related functions

## Load libraries ####
library(tiledbsoma)

# Function to check if MinIO server is live ####
checkMinioServer <- function (address) {
  curl_result <- system2("curl", 
                         args = c("-Is", 
                                  paste0("http://", 
                                         address, 
                                         "/minio/health/live")),
                         stdout = TRUE, stderr = TRUE)
  server_up <- any(grepl("^HTTP/1.1 200", curl_result))
  
  if (server_up) {
    print("Server is up!")
    return(TRUE)
  } else {
    print("Server is down!")
    return(FALSE)
  }
}

# Function to create MinIO environment ####
createMinioEnv <- function (access_key_id, secret_access_key, address) {
  Sys.setenv("AWS_ACCESS_KEY_ID" = access_key_id,
             "AWS_SECRET_ACCESS_KEY" = secret_access_key, 
             "AWS_DEFAULT_REGION" = "",
             "AWS_S3_ENDPOINT" = address,
             "AWS_S3_DISABLE_SSL" = "true")
}

# Function to create a TileDB SOMA context for MinIO  ####
createMinioContext <- function (address, access_key_id, secret_access_key) {
  context <- SOMATileDBContext$new(config = NULL)
  context$set("vfs.s3.scheme", "http")
  context$set("vfs.s3.region", "")
  context$set("vfs.s3.use_virtual_addressing", "true")
  context$set("vfs.s3.verify_ssl", "false")
  context$set("vfs.s3.aws_access_key_id", access_key_id)
  context$set("vfs.s3.endpoint_override", address)
  context$set("vfs.s3.aws_secret_access_key", secret_access_key)
  return (context)
}
