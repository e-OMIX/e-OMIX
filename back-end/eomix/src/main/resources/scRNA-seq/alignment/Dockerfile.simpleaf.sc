FROM continuumio/miniconda3

ENV http_proxy "http://sss-proxy.icp.ucl.ac.be:3128"
ENV https_proxy "http://sss-proxy.icp.ucl.ac.be:3128"
ENV ftp_proxy "http://sss-proxy.icp.ucl.ac.be:3128"

RUN apt-get update && apt-get install -y jq
RUN conda install simpleaf==0.19.5 -c bioconda -c conda-forge
RUN pip install minio==7.2.15 glob2 couchdb==1.2

ENV ALEVIN_FRY_HOME=/opt/simpleaf_config

WORKDIR /app

COPY simpleaf_script.py /app

ENTRYPOINT ["python","simpleaf_script.py"]
