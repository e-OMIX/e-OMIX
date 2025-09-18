#!/bin/bash

# Start the docker with the requested analysis name
docker run \
  -d \
  --rm \
  -e ANALYSIS_NAME=$1 \
  -e AWS_ACCESS_KEY_ID="YOU_MINIO_ACCESS_KEY" \
  -e AWS_SECRET_ACCESS_KEY="YOUR_MINIO_SECRET_KEY" \
  -e AWS_S3_ENDPOINT="host.docker.internal:9000" \
  -e AWS_S3_DISABLE_SSL=true \
  -p 0.0.0.0::3838 \
  visualization