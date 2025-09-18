# Start from the rocker/shiny base image
FROM rocker/shiny:4.5.0

# Set proxy
# ENV http_proxy=http://sss-proxy.icp.ucl.ac.be:3128
# ENV https_proxy=http://sss-proxy.icp.ucl.ac.be:3128
# ENV ftp_proxy=http://sss-proxy.icp.ucl.ac.be:3128

# Install Python and venv support
RUN apt-get update && \
    apt-get install -y \
	python3 python3-pip python3-venv \
	libglpk40 libglpk-dev \
	&& rm -rf /var/lib/apt/lists/*
	
# Create a virtual environment in /opt/venv
RUN python3 -m venv /opt/venv

# Activate the virtual environment and install Python packages
RUN /opt/venv/bin/pip install --upgrade pip && \
    /opt/venv/bin/pip install anndata h5py numpy pandas s3fs

# Let reticulate know to use this Python
ENV RETICULATE_PYTHON="/opt/venv/bin/python"
ENV PATH="/opt/venv/bin:$PATH"

# Choose snapshot date for reproducibility
ENV R_REPOS=https://packagemanager.posit.co/cran/2024-08-21

# Install CRAN + Bioconductor packages reproducibly
RUN R -e "options(repos = c(CRAN = Sys.getenv('R_REPOS'))); \
          install.packages(c( \
			'remotes', \
          	'dplyr', \
          	'ggplot2', \
          	'ggpubr', \
          	'purrr', \
          	'shiny', \
          	'shinyjs', \
          	'purrr', \
          	'tibble', \
          	'tidyr', \
          	'igraph', \
          	'DT', \
          	'colorspace', \
			'BiocManager' \
			))"

RUN R -e "if (!requireNamespace('aws.s3', quietly = TRUE)) remotes::install_version('aws.s3', version = '0.3.21')"
  
RUN R -e "BiocManager::install(c('biomaRt','SingleCellExperiment'))"
RUN R -e "BiocManager::install(c('iSEE','iSEEu'))"

# Copy the entire app directory into the image
COPY scRNA-seq/visualization/ /srv/shiny-server/
COPY utils/ /srv/shiny-server/utils/
WORKDIR /srv/shiny-server/

# Adjust permissions
RUN chown -R shiny:shiny /srv/shiny-server

# Expose port 3838
EXPOSE 3838

