# Use r-base image
#FROM rocker/r-ver:latest
FROM bioconductor/bioconductor_docker:RELEASE_3_21

# # Proxy
# ENV http_proxy=http://sss-proxy.icp.ucl.ac.be:3128
# ENV https_proxy=http://sss-proxy.icp.ucl.ac.be:3128
# ENV ftp_proxy=http://sss-proxy.icp.ucl.ac.be:3128
# ENV no_proxy=192.168.51.61

# External libraries
RUN apt-get update && apt-get install -y \
    build-essential \
    libcurl4-openssl-dev \
    libssl-dev \
    libxml2-dev \
    libfontconfig1-dev \
    libfreetype6-dev \
    libpng-dev \
    libtiff5-dev \
    libjpeg-dev \
    zlib1g-dev \
    libharfbuzz-dev \
    libfribidi-dev \
    libglib2.0-dev \
    libglpk-dev \
    libgmp-dev \
    libmpfr-dev \
    libigraph-dev \
    wget curl git \
    && rm -rf /var/lib/apt/lists/*
RUN apt-get update && \
	apt-get install -y python3 python3-pip python3-venv
	
# Create a virtual environment in /opt/venv
RUN python3 -m venv /opt/venv

# Activate the virtual environment and install Python packages
RUN /opt/venv/bin/pip install --upgrade pip && \
	/opt/venv/bin/pip install anndata h5py numpy pandas s3fs minio

# Let reticulate know to use this Python
ENV RETICULATE_PYTHON="/opt/venv/bin/python"
ENV PATH="/opt/venv/bin:$PATH"

# Install CRAN packages
RUN R -e "install.packages('remotes', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('aws.s3', version = '0.3.21', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('Matrix', version = '1.7-3', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('Rtsne', version = '0.17', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('umap', version = '0.2.10.0', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('dplyr', version = '1.1.4', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('tidyr', version = '1.3.1', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('rjson', version = '0.2.23', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('BiocManager', version = '1.30.26', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('sofa', version = '0.4.0', repos = 'https://cloud.r-project.org')"
RUN R -e "remotes::install_version('reticulate', version = '1.43.0', repos = 'https://cloud.r-project.org')"

# Set environment so reticulate finds the venv
ENV RETICULATE_PYTHON=/opt/venv/bin/python

# Install Bioconductor packages  with verification
RUN R -e "\
 options(repos = BiocManager::repositories()); \
 BiocManager::install(c('alabaster.base','alabaster.matrix', 'alabaster.se'), ask=FALSE, update=FALSE, verbose=TRUE, version = '3.21'); \
 if (!all(c('alabaster.base', 'alabaster.matrix', 'alabaster.se') %in% installed.packages()[,'Package'])) stop('Alabaster packages failed')"
RUN R -e "\
 BiocManager::install('celldex', ask=FALSE, update=TRUE, dependencies = TRUE, version = '3.21'); \
 if (!requireNamespace('celldex', quietly = TRUE)) stop('celldex failed to install')"
RUN R -e "\
 BiocManager::install('igraph', ask=FALSE, update=TRUE, version = '3.21'); \
 if (!requireNamespace('celldex', quietly = TRUE)) stop('igraph failed to install')"	
RUN R -e "\
 BiocManager::install('bluster', ask=FALSE, update=TRUE, verbose=TRUE, version = '3.21'); \
 if (!requireNamespace('bluster', quietly=TRUE)) stop('bluster failed to install')"
RUN R -e "\
 BiocManager::install('scrapper', ask=FALSE, update=TRUE, verbose=TRUE, version = '3.21'); \
 if (!requireNamespace('scrapper', quietly=TRUE)) stop('scrapper failed to install')"
RUN R -e "\
 BiocManager::install('biomaRt', ask=FALSE, update=TRUE, verbose=TRUE, version = '3.21'); \
 if (!requireNamespace('biomaRt', quietly=TRUE)) stop('biomaRt failed to install')"  
RUN R -e "\
 BiocManager::install('scran', ask=FALSE, update=TRUE, verbose=TRUE, version = '3.21'); \
 if (!requireNamespace('scran', quietly=TRUE)) stop('scran failed to install')"
RUN R -e "\
 BiocManager::install('SingleR', ask=FALSE, update=TRUE, verbose=TRUE, version = '3.21'); \
 if (!requireNamespace('SingleR', quietly=TRUE)) stop('SingleR failed to install')"
RUN R -e "\
 BiocManager::install('SingleCellExperiment', ask=FALSE, update=TRUE, verbose=TRUE, version = '3.21'); \
 if (!requireNamespace('SingleCellExperiment', quietly=TRUE)) stop('SingleCellExperiment failed to install')"


# Copy files and set working directory
COPY scRNA-seq/post-processing/ app/
COPY utils/ app/utils/
WORKDIR /app

CMD ["Rscript", "post-processing.R"]
