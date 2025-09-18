import os
import json
import time
from minio import Minio

# Get the environment variable using --env from docker 
minio_host = os.environ.get("MINIO_ENDPOINT", "host.docker.internal:9000").replace("http://", "")
minio_access = os.environ.get("MINIO_ACCESS_KEY", "")
minio_secret = os.environ.get("MINIO_SECRET_KEY", "")
jsonpath = os.environ.get("JSON_FILE_PATH")

def parse_json(jsonFile):
    # Function that parse a json file 
    # jsonFile correspond to the local path on the mounted volume
    # return a python dictionary
    with open(jsonFile, 'r') as jsonfile:
        python_dict = json.load(jsonfile)
    return(python_dict)

def fetch_json(json_env_path):
    # Function that use the json file path to download it from the minio server
    # json_env_path matches the path to the json file captured by the env
    # return the local path of the json file on the mounted volume
    local_path = f"/data/{json_env_path.split('/')[-1]}"
    print(f"⬇️  Downloading JSON from MinIO: {json_env_path} -> {local_path}")
    client = Minio(
        minio_host,
        access_key=minio_access,
        secret_key=minio_secret,
        secure=False
    )
    client.fget_object(bucket_name="alignment", object_name=json_env_path, file_path=local_path)
    return local_path


def run_fastqc(dict_json, client):
    # Function that run fastqc program on fastq files
    # first, the function will download fastq files from minio server
    # then, will create a directory fastqc_results on the mounted volume
    # finally, will run fastqc on fastq files
    # dict_json correspond to the python dictionnary from parse_json function
    # client correspond to the connection to the minio server
    print("Downloading file from minio server")
    for i in range(len(dict_json["samples"])):
        sub_dict_json= dict_json["samples"][i]
        for j in range(len(sub_dict_json["fq1Files"])):
            print(f"Downloading: {sub_dict_json['fq1Files'][j]} and {sub_dict_json['fq2Files'][j]}")
            client.fget_object(bucket_name = "alignment", object_name=dict_json["experimentName"]+"/"+sub_dict_json["fq1Files"][j], file_path="/data/"+sub_dict_json["fq1Files"][j])
            client.fget_object(bucket_name="alignment", object_name=dict_json["experimentName"]+"/"+sub_dict_json["fq2Files"][j], file_path="/data/"+sub_dict_json["fq2Files"][j])
        os.system("mkdir /data/fastqc_results")
        os.system("fastqc -t 6 /data/*.fastq* -o /data/fastqc_results/")

client = Minio(minio_host, access_key=minio_access, secret_key=minio_secret, secure=False)
jsonfile = fetch_json(jsonpath)
dict_args = parse_json(jsonfile)
run_fastqc(dict_args, client)
time.sleep(10)
