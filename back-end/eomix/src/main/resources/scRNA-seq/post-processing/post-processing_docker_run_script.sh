#!/bin/bash

# Start the post-processing docker
docker run \
	-e AWS_ACCESS_KEY_ID="YOU_MINIO_ACCESS_KEY" \
	-e AWS_SECRET_ACCESS_KEY="YOUR_MINIO_SECRET_KEY" \
 	-e AWS_S3_ENDPOINT="host.docker.internal:9000" \
 	-e AWS_S3_DISABLE_SSL='true' \
	-e AWS_DEFAULT_REGION='' \
	-e POSTPROCESSING_FOLDER_NAME=$1 \
	-e NOMENCLATURE="symbol" \
	-e METADATA="true" \
	-e REFERENCE_RNA='auto' \
	post-processing

