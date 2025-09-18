FROM python:3.11-slim

ENV http_proxy "http://sss-proxy.icp.ucl.ac.be:3128"
ENV https_proxy "http://sss-proxy.icp.ucl.ac.be:3128"
ENV ftp_proxy "http://sss-proxy.icp.ucl.ac.be:3128"

RUN apt-get update && apt-get install -y \
	default-jre wget unzip dnf \
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/* 

RUN wget -O /tmp/fastqc.zip https://www.bioinformatics.babraham.ac.uk/projects/fastqc/fastqc_v0.12.1.zip \
	&& unzip /tmp/fastqc.zip -d /opt/ \
	&& chmod +x /opt/FastQC/fastqc \
	&& ln -s /opt/FastQC/fastqc /usr/local/bin/fastqc

RUN pip install minio==7.2.15

WORKDIR /app
COPY fastqc_script.py /app

CMD ["python", "fastqc_script.py"]

