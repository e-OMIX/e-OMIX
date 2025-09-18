from continuumio/miniconda3

ENV http_proxy "http://sss-proxy.icp.ucl.ac.be:3128"
ENV https_proxy "http://sss-proxy.icp.ucl.ac.be:3128"
ENV ftp_proxy "http://sss-proxy.icp.ucl.ac.be:3128"

RUN pip install minio glob2
RUN conda install -c bioconda -c conda-forge multiqc=1.28

WORKDIR /app
COPY multiqc_script.py /app

CMD ["python","multiqc_script.py"]


