from minio import Minio
import os
import json
import glob


client = Minio("host.docker.internal:9000",
                   access_key="YOU_MINIO_ACCESS_KEY",
                   secret_key="YOUR_MINIO_SECRET_KEY",secure=False)

jsonfile = os.environ.get("JSON_FILE_PATH")

def parse_json(jsonFile):
    # load json file
    with open(jsonFile, 'r') as jsonfile:
        python_dict = json.load(jsonfile)
    return(python_dict)

def run_multiqc(results):
    os.system("multiqc "+results+"/*/ /data/fastqc_results/* --outdir /data/multiqcReport --force")
    #multiqc.run("/data/fastqc_results/*", outdir = "/data/multiqcReport", force = True)

def upload_local_directory_to_minio(local_path, bucket_name, minio_path, client):
    assert os.path.isdir(local_path)
    for local_file in glob.glob(local_path + '/**'):
        local_file = local_file.replace(os.sep, "/") # Replace \ with / on Windows
        if not os.path.isfile(local_file):
            upload_local_directory_to_minio(
                local_file, bucket_name, minio_path + "/" + os.path.basename(local_file), client)
        else:
            remote_path = os.path.join(
                minio_path, local_file[1 + len(local_path):])
            remote_path = remote_path.replace(
                os.sep, "/")  # Replace \ with / on Windows
            client.fput_object(bucket_name, remote_path, local_file)

run_multiqc(results="/data/test_quant/")
dict_args = parse_json(jsonFile="/data/"+jsonfile)
upload_local_directory_to_minio(local_path = "/data/multiqcReport", bucket_name = "alignment", minio_path = dict_args["experimentName"]+"/results_alignment/multiqc", client = client)

