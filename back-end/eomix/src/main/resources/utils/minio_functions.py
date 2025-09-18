from minio import Minio
from minio.error import S3Error
import argparse
from minio.commonconfig import ENABLED
from minio.versioningconfig import VersioningConfig

def upload(source_file, bucket_name, destination_file):
    client = Minio("host.docker.internal:9000",
                   access_key="YOU_MINIO_ACCESS_KEY",
                   secret_key="YOUR_MINIO_SECRET_KEY",secure=False)
    

    found = client.bucket_exists(bucket_name=bucket_name)
    if not found:
        client.make_bucket(bucket_name=bucket_name)
        client.set_bucket_versioning(bucket_name=bucket_name, config = VersioningConfig(ENABLED))
    else:
        print("Bucket",bucket_name,"already exists")
    client.fput_object(
        bucket_name=bucket_name,object_name=destination_file, file_path=source_file
    )
    objects = client.list_objects(bucket_name=bucket_name, include_version=True)
    for obj in objects:
        if obj.is_latest == True :
            last_version_id = obj.version_id
        
    
    print(
        source_file, "successfully uploaded as object",
        destination_file,"to bucket", bucket_name
    )
    #return(last_version_id)

def download(source_file, bucket_name, destination_file, version_id):
    client = Minio("host.docker.internal:9000",
                   access_key="YOU_MINIO_ACCESS_KEY",
                   secret_key="YOUR_MINIO_SECRET_KEY",secure=False)
    

    found = client.bucket_exists(bucket_name=bucket_name)
    if not found:
        print("Bucket",bucket_name,"doesn't exists")
    
    client.fget_object(
        #bucket_name=bucket_name,object_name=destination_file, file_path= destination_file, extra_query_params={"Date":"2025-01-2029 16:10:51.572354"}
        bucket_name=bucket_name,object_name=source_file, file_path= destination_file, version_id= version_id
    )
    
    print(
        source_file, "successfully download as object",
        destination_file,"to bucket", bucket_name
    )


if __name__ == "__main__" :
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="function")

    parser_upload = subparsers.add_parser('upload',help = 'Upload file onto bucket minio')
    parser_upload.add_argument("-i", "--inputfile", help = "File to upload into the bucket ", required=True)
    parser_upload.add_argument('-b','--bucket',help = "Bucket name", required=True)
    parser_upload.add_argument('-n','--namefile',help = "file name into the bucket", required=True)

    parser_download = subparsers.add_parser('download',help = 'Download file from minio bucket')
    parser_download.add_argument("-i", "--inputfile", help = "File to download into the bucket ", required=True)
    parser_download.add_argument('-b','--bucket',help = "Bucket name", required=True)
    parser_download.add_argument('-n','--namefile',help = "file name to store", required=True)
    parser_download.add_argument('-v','--version_id',help= "version id of the file to download", required = True)

    args = parser.parse_args()

    if args.function == 'upload':
        version = upload(source_file= args.inputfile, bucket_name= args.bucket, destination_file= args.namefile)
    elif args.function == "download":
        download(source_file= args.inputfile, bucket_name=args.bucket, destination_file=args.namefile, version_id = args.version_id)