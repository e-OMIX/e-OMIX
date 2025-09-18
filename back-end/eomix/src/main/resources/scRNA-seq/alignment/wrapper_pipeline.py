import sys
import time
import subprocess
import json
from minio import Minio
import couchdb

def run_command(command):
    # Function that use subprocess package to run bash commands.
    # command argument needs the bash command that will be run
    # return result of command
    print(f"\n Running: {command}")
    result = subprocess.run(command,shell=True, capture_output=True, text=True)
    print(result.stdout)
    if result.stderr:
        print("", result.stderr)
    return result

def wait_for_service_completion(service_name):
    # Function that return the current state of the docker service every 5 seconds. 
    # service_name argument has to contains the docker service name that will be evaluated. 
    # return status of docker service.
    print(f"\n Waiting for service '{service_name}' to finish ...")
    while True :
        cmd = f"docker service ps {service_name} --no-trunc --format '{{{{.CurrentState}}}}' | head -n 1"
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        status = result.stdout.strip()
        print(f"Service status: {status}")
        if "Complete" in status or "Failed" in status or "Shutdown" in status:
            return(status)
            break
        time.sleep(5)
        

def update_status(doc_id, status):
    # Function that updates status on couchdb.
    # doc_id is the id of the document that will be updated.
    # status can be either "Done", "In_evaluation", "In_progress", or "Error".
    couch = couchdb.Server("http://YOUR_COUCHDB_USERNAME:YOUR_COUCHDB_PASSWORD@localhost:5984/")
    db = couch["experiment"]
    if doc_id in db :
        doc = db[doc_id]
        doc["status"]=status
        db.save(doc)
        print(f"Document {doc_id} updated successfully.")
    else:
        print(f"Document {doc_id} not found.")

def parse_json(jsonFile):
    # Function that parse a json file and return a python dictionnary.
    # jsonFile is the path throught the json file.
    # return python dictionnary
    with open(jsonFile, 'r') as jsonfile:
        python_dict = json.load(jsonfile)
    return(python_dict)

def main():
    # First step: connect to minio db and download json file locally. 
    # Second step: read the json file and convert it to python dictionnary.
    # Third step: create docker service fastqc_{exp_name} that run fastqc \ 
    #   see `fastqc_Sc.py` for more informations.
    # Fourth step : create docker service alignment_{exp_name} that run \
    #   alignment, see `aln_simpleaf.py` or `aln_cellranger.py` for more \ 
    #   informations.
    # Fifth step: create docker service multiqc_{exp_name} that run multiqc \
    #   see `multiqc_Sc.py` for more informations.
    # Sixth step: remove docker service and shared volume.
    if len(sys.argv) != 2:
        print("Usage: python test_wrapper.py /absolute/path/to/your.json")
        sys.exit(1)
    json_path = sys.argv[1]
    jsonfile = json_path.split("/")[-1]
    exp_name = json_path.split("/")[0]
    client = Minio(
            "localhost:9000",
            access_key="YOU_MINIO_ACCESS_KEY",
            secret_key="YOUR_MINIO_SECRET_KEY",
            secure=False
        )
    client.fget_object(bucket_name="alignment", object_name = json_path, file_path = "tmp/"+jsonfile)
    dict_python = parse_json(jsonFile = "tmp/"+jsonfile)
    update_status(doc_id= dict_python["couchDBId"], status="In_progress") 
    run_command(f"""
    docker service create \
      --name fastqc_{exp_name} \
      --restart-condition none \
      --mount type=volume,source=shared_{exp_name},target=/data \
      --env JSON_FILE_PATH={json_path} \
      --env MINIO_ENDPOINT=host.docker.internal:9000 \
      --env MINIO_ACCESS_KEY=YOU_MINIO_ACCESS_KEY \
      --env MINIO_SECRET_KEY=YOUR_MINIO_SECRET_KEY \
      eomix/fastqc_image:latest
    """)

    status=wait_for_service_completion(f"""fastqc_{exp_name}""")
    if "Failed" in status:
        update_status(doc_id=dict_python["couchDBId"], status="Error")
        sys.exit(1)

    run_command(f"""
    docker service create \
        --name alignment_{exp_name} \
        --restart-condition none \
        --mount type=volume,source=shared_{exp_name},target=/data \
        --env JSON_FILE_PATH={jsonfile} \
        --env MINIO_ENDPOINT=localhost:9000 \
        --env MINIO_ACCESS_KEY=YOU_MINIO_ACCESS_KEY \
        --env MINIO_SECRET_KEY=YOUR_MINIO_SECRET_KEY \
        eomix/simpleaf_image:latest
    """)
    
    status=wait_for_service_completion(f"""alignment_{exp_name}""")
    if "Failed" in status:
        update_status(doc_id=dict_python["couchDBId"], status = "Error")
        sys.exit(1)
    run_command(f"""
    docker service create \
        --name multiqc_{exp_name} \
        --restart-condition none \
        --mount type=volume,source=shared_{exp_name},target=/data \
        --env JSON_FILE_PATH={jsonfile} \
        --env MINIO_ENDPOINT=localhost:9000 \
        --env MINIO_ACCESS_KEY=YOU_MINIO_ACCESS_KEY \
        --env MINIO_SECRET_KEY=YOUR_MINIO_SECRET_KEY \
        eomix/multiqc_image:latest
    """)
    
    status=wait_for_service_completion(f"""multiqc_{exp_name}""")  
    
    if "Failed" in status:
        update_status(doc_id=dict_python["couchDBId"], status="Error")
        sys.exit(1)
    else:
        update_status(doc_id=dict_python["couchDBId"],status="Done")

    print("\nRemoving services")
    run_command(f"""
    docker service rm \
        fastqc_{exp_name} \
        alignment_{exp_name} \
        multiqc_{exp_name}
    """)
    
    print("\nRemoving volume")
    
    time.sleep(5)
    run_command(f"""
    docker volume rm shared_{exp_name}
    """)


    print("\n✅ Pipeline completed.")

if __name__ == "__main__":
    main()


