import json
import os
from minio import Minio
import glob
import couchdb
import argparse

minio_host = os.environ.get("MINIO_ENDPOINT", "host.docker.internal:9000").replace("http://", "")
minio_access = os.environ.get("MINIO_ACCESS_KEY", "")
minio_secret = os.environ.get("MINIO_SECRET_KEY", "")
jsonfile = os.environ.get("JSON_FILE_PATH")

client = Minio(minio_host,
                access_key=minio_access,
                secret_key=minio_secret, secure=False)


def parse_json(jsonFile):
    # load json file
    with open(jsonFile, 'r') as jsonfile:
        python_dict = json.load(jsonfile)
    return(python_dict)

def check_index_exist(dict_args, client):
    dict_args["AnnotVersion"] = "v"+dict_args["annotation"].split(".")[2]
    dict_args["GenomeVersion"] = dict_args["genome"].split(".")[1]
    dict_args["Analysis_type"] = "Single_cell"
    dict_args["Precomputed_index"] = False
    path_object = dict_args["Analysis_type"]+"/"+dict_args["selectedAligner"]+"/"+dict_args["selectedOrganism"]+"/"+dict_args["AnnotVersion"]+"/index"
    # Get data of an object.
    dict_args["path2Index"] = path_object
    try :
        response = client.get_object("index",object_name=path_object+"/simpleaf_index_log.json")
        print(response)
        dict_args["Precomputed_index"] = True
        response.close()
        response.release_conn()
    except :
        print("No pre-computed index")
    return(dict_args)

def download_directory(dict_args, client):
    for item in client.list_objects("index",prefix=dict_args["path2Index"],recursive=True):
        client.fget_object(bucket_name = "index",object_name=item.object_name,file_path ="/data/index/"+item.object_name)
    dict_args["path2Index"] = "/data/index/"+dict_args["path2Index"]+"/index"
    return dict_args


def indexing(dict_args, client):
    experiment_name = dict_args["experimentName"].split("-")[1]
    outdir = dict_args["experimentName"]+"/results_alignment"+ experiment_name
    bucket_name = "genome-annotations"
    annotation = dict_args["selectedOrganism"]+"/annotation/"+dict_args["annotation"]
    genome = dict_args["selectedOrganism"]+"/genome/"+dict_args["genome"]
    response_annot = client.get_object(bucket_name=bucket_name,object_name=annotation)
    path_ant = response_annot.geturl()
    response_genome = client.get_object(bucket_name=bucket_name,object_name=genome)
    path_gen = response_genome.geturl()
    print(os.listdir("/data/"))
    print("Download annotation and genome")
    client.fget_object(bucket_name = bucket_name, object_name = annotation, file_path = "/data/"+dict_args["annotation"])
    client.fget_object(bucket_name = bucket_name, object_name = genome, file_path = "/data/"+dict_args["genome"])
    if os.path.splitext(dict_args["annotation"])[1] == ".gz":
        print("Dezipping annotation")
        os.system("gunzip /data/"+dict_args["annotation"])
    if os.path.splitext(dict_args["genome"])[1] ==".gz":
        print("Unziping genome")
        os.system("gunzip /data/"+dict_args["genome"])
    annot_wo_ext = os.path.splitext(dict_args["annotation"])[0]
    genome_wo_ext = os.path.splitext(dict_args["genome"])[0]
    os.system('simpleaf index -t 6 --output /data/'+dict_args["selectedAligner"]+'_index --fasta /data/'+dict_args["genome"]+' --gtf /data/'+dict_args["annotation"]+' --no-piscem')
    dict_args["path2Index"] = "/data/"+dict_args["selectedAligner"]+"_index"
    path_object = dict_args["Analysis_type"]+"/"+dict_args["selectedAligner"]+"/"+dict_args["selectedOrganism"]+"/"+dict_args["AnnotVersion"]+"/index"
    upload_local_directory_to_minio(local_path="/data/"+dict_args["selectedAligner"]+"_index", bucket_name = "index", minio_path = path_object, client = client)
    return dict_args

def download_files(dict_args, client):
    os.system("simpleaf set-paths --alevin-fry /opt/conda/bin/alevin-fry --salmon /opt/conda/bin/salmon")
    print("checking if index already exist")
    dict_args = check_index_exist(dict_args,client=client)
    if dict_args["Precomputed_index"] == True:
        print("Index already exist on minio server --> downloading it ")
        dict_args= download_directory(dict_args, client= client)
        # dict_args is returned because it is updated with the path through index
    else :
        print("No Index on the minio server --> Creating one")
        dict_args = indexing(dict_args=dict_args, client=client)
    print("Download Fastq files")
    for i in range(0,len(dict_args["samples"])):
       sub_dict_args=dict_args["samples"][i]
       for j in range(0,len(sub_dict_args["fq1Files"])):
            client.fget_object(bucket_name = "alignment", object_name= dict_args["experimentName"]+"/"+sub_dict_args["fq1Files"][j], file_path = "/data/"+sub_dict_args["sampleName"]+"/"+sub_dict_args["fq1Files"][j])
            client.fget_object(bucket_name = "alignment", object_name= dict_args["experimentName"]+"/"+sub_dict_args["fq2Files"][j], file_path = "/data/"+sub_dict_args["sampleName"]+"/"+sub_dict_args["fq2Files"][j])
            print("starting quantification")
            os.system("simpleaf --version")
            basename_fastq_file = sub_dict_args["fq1Files"][j].split(".")[0]
            print("Quantifying "+basename_fastq_file)
            if dict_args["selectedAligner"] == "Salmon":
                print("Use Salmon instead of piscem")
                #os.system("simpleaf quant -c dropseq -o /data/test_quant -t 6 -i "+dict_args["path2Index"]+" --reads1 "+fq1+" --reads2 "+fq2+" --resolution cr-like --expect-cells 5000 --t2g-map /data/tx_to_gene.csv --no-piscem --anndata-out")
                os.system("simpleaf quant -c dropseq -o /data/test_quant/"+basename_fastq_file+" -t 6 -i "+dict_args["path2Index"]+" --reads1 /data/"+sub_dict_args["sampleName"]+"/"+sub_dict_args["fq1Files"][j]+" --reads2 /data/"+sub_dict_args["sampleName"]+"/"+sub_dict_args["fq2Files"][j]+" --resolution cr-like-em --knee --no-piscem")
            else :
                print("Use Piscem")
                os.system("simpleaf quant -c dropseq -o /data/test_quant/"+basename_fastq_file+" -t 6 -i "+dict_args["path2Index"]+" --reads1 /data/"+sub_dict_args["sampleName"]+"/"+sub_dict_args["fq1Files"][j]+" --reads2 /data/"+sub_dict_args["sampleName"]+"/"+sub_dict_args["fq2Files"][j]+" --resolution cr-like-em --knee")
    print("finish quantification")

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

def update_status(doc_id, status):
    # Update status of the experiment on couchDB server
    # by default the status will be In evaluation and can be change for In progress, Error or Done
    couch = couchdb.Server("http://YOUR_COUCHDB_USERNAME:YOUR_COUCHDB_PASSWORD@host.docker.internal:5984/")
    db = couch["experiment"]
    if doc_id in db:
        doc = db[doc_id]
        doc["status"] = status
        db.save(doc)
        print(f"Document {doc_id} updated successfully.")
    else:
        print(f"Document {doc_id} not found.")

json_dict = parse_json(jsonFile= "/data/"+jsonfile)
print(json_dict)
#update_status(doc_id = json_dict["couchDBId"], status = "In_progress")
download_files(dict_args=json_dict, client=client)
upload_local_directory_to_minio(local_path="/data/test_quant", bucket_name = "alignment", minio_path = json_dict["experimentName"]+"/results_alignment", client = client)
#update_status(doc_id = json_dict["couchDBId"], status = "Done")
