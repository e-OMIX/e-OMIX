## Assemble, filter and annotate the row matrix from alignment

# Setup ####
## Libraries ####
library(aws.s3)
library(biomaRt)
library(bluster)
library(celldex)
library(dplyr)
library(Matrix)
library(reticulate)
library(rjson)
library(Rtsne)
library(SingleCellExperiment)
library(SingleR)
library(scran)
library(scrapper)
library(tidyr)
library(umap)
source (file.path(getwd(), "utils", "couchdb_functions.R"))
print("Packages are loaded.")


## Import MinIO Python functions ####
reticulate::virtualenv_create("/opt/venv")
reticulate::use_virtualenv("/opt/venv", 
                           required = TRUE) ## Set Python environment
reticulate::source_python(file.path(getwd(), "utils", "minio_functions.py"))
print(reticulate::py_config()) 

## Import post-processing parameters ####
postprocessing_folder_name <- Sys.getenv("POSTPROCESSING_FOLDER_NAME")
print(paste0("Post-processing folder: ", postprocessing_folder_name))
postprocessing_parameters <- 
  aws.s3::s3read_using(function(path) {fromJSON(file = path, simplify = TRUE)}, 
                       bucket = "post-processing",
                       object = file.path(postprocessing_folder_name, 
                                          "post-processing_parameters.json"),
                       opts = list(region = "",
                                   check_region = FALSE,
                                   base_url = Sys.getenv("AWS_S3_ENDPOINT"),
                                   key = Sys.getenv("AWS_ACCESS_KEY_ID"),
                                   secret = Sys.getenv("AWS_SECRET_ACCESS_KEY"),
                                   use_https = FALSE))

## Alignment folder name
alignment_folder_name <- 
  postprocessing_parameters$alignmentExperiment$experimentName

### Minimum genes by cell ####
min_genes_by_cell <- as.integer(postprocessing_parameters$minGenesByCells)

### Minimum cells expressing a gene ####
min_cell_expressing_gene <- as.integer(postprocessing_parameters$minCellsExpressingGene)

### Number of Highly Variables Genes (HVG) ####
hvg <- as.integer(postprocessing_parameters$numHighVariableGenes)

# Dimensionality reduction
if (grepl("umap", postprocessing_parameters$dimensionReduction, 
          ignore.case = TRUE)) {
  umap <- 'true'
} else {umap <- 'false'}
if (grepl("tsne", postprocessing_parameters$dimensionReduction, 
          ignore.case = TRUE)) {
  tsne <- 'true'
} else {tsne <- 'false'}

# Clustering
if (grepl("louvain", postprocessing_parameters$clustering, ignore.case = TRUE)) {
  louvain <- 'true'
} else {louvain <- 'false'}
if (grepl("leiden", postprocessing_parameters$clustering, ignore.case = TRUE)) {
  leiden <- 'true'
} else {leiden <- 'false'}

### Gene nomenclature
nomenclature <- 'symbol'

## Metadata
metadata <- 'true'

## Cell annotation reference RNA dataset
reference_rna <- "auto"

print("Post-processing parameters: OK!")

# Change CouchDB status to 'In progress' ####
updateCouchdbStatus(document_id = postprocessing_parameters$couchDBId,
                    new_status = "In_progress")

# Import matrices ####
## Get metadata
metadata_name <-  sub("-.*", "", alignment_folder_name)
metadata_df <-
  aws.s3::s3read_using(function(x) {read.csv(file = x, sep = "\t")},
                       bucket = "post-processing",
                       object = paste0(postprocessing_folder_name, 
                                       "/",
                                       metadata_name, 
                                       ".csv"),
                       opts = list(use_https = FALSE, region = ""))

## Assemble a list of matrices 
mat_list <- list()
parameters_samples <- postprocessing_parameters$alignmentExperiment$samples
for (sample_list in 1:length(parameters_samples)) {
  mat_list <- 
    c(mat_list, 
      parameters_samples[[sample_list]]$fq1Files)
} 
mat_list <- 
  lapply(mat_list, function (x) {
    sub(".(fastq|fasta)(\\.gz)?$", "", x, ignore.case = TRUE)
  })

## Assemble the matrices and update metadata sheet
for (mat_name in mat_list) {
  sample_name <- sub("_.*", "", mat_name)
  mat_folder <- file.path(alignment_folder_name,
                          "results_alignment",
                          mat_name,
                          "af_quant",
                          "alevin")
  mat <- aws.s3::s3read_using(readMM,
                              bucket = "alignment",
                              object = file.path(mat_folder, 'quants_mat.mtx'),
                              opts = list(use_https = FALSE,
                                          region = "",
                                          check_region = FALSE))
  mat_rows <- aws.s3::s3read_using(function (x) read.delim(x, header = FALSE),
                                   bucket = "alignment",
                                   object = file.path(mat_folder, 
                                                      "quants_mat_rows.txt"),
                                   opts = list(use_https = FALSE,
                                               region = "",
                                               check_region = FALSE))
  mat_cols <- aws.s3::s3read_using(function (x) read.delim(x, header = FALSE),
                                   bucket = "alignment",
                                   object = file.path(mat_folder, 
                                                      "quants_mat_cols.txt"),
                                   opts = list(use_https = FALSE,
                                               region = "",
                                               check_region = FALSE))
  rownames(mat) <- as.vector(paste(sample_name, mat_rows[[1]], sep = "_"))
  colnames(mat) <- as.vector(mat_cols[[1]])
  mat <- as(mat, "CsparseMatrix") ## Convert to column-oriented sparse matrix
  
  ## Fill in metadata with cell names
  sample_name <- sub("_.*", "", mat_name)
  cells <- paste(sample_name, rownames(mat), sep = "_")
  expanded_metadata_df <- metadata_df %>%
    dplyr::filter(sample_id == sample_name) %>%
    dplyr::slice(rep(1L, length(cells))) %>% 
    dplyr::mutate(cell_id = cells)
  
  metadata_df <- metadata_df |>
    filter(sample_id != sub("_.*", "", mat_name)) |>
    bind_rows(expanded_metadata_df)
  
  ## Assemble the combined matrix
  if (!exists("combined_mat")) {
    combined_mat <- mat
  } else {
    combined_mat <- rbind(combined_mat, mat)
  }
  
  ## Cleanup
  rm(mat, mat_name, mat_folder, mat_rows, mat_cols, 
     sample_name, cells, expanded_metadata_df)
}
print("Matrices have been assembled.")

# Assemble SingleCellExperiment object ####
## Add count matrix (features as rows and cells as columns)
sce_object <- SingleCellExperiment(t(combined_mat))
assayNames(sce_object) <- "counts" ## Rename assay
mainExpName(sce_object) <- sub("-.*", "", postprocessing_folder_name)
rm(combined_mat)

## Convert gene names ####
if (nomenclature == "symbol") {
  source(file.path(getwd(), "utils", "gene_conversion.R"))
  parameter_species <- 
    postprocessing_parameters$alignmentExperiment$selectedOrganism
  if (parameter_species == "Mus_musculus") {selected_species <- "mmusculus"}
  if (parameter_species == "Homo_sapiens") {selected_species <- "hsapiens"}
  if (!(selected_species %in% c("mmusculus", "hsapiens"))) {
    warning("Gene names from selected organism cannot be converted.")
  } else {
    rownames(sce_object) <- convertEnsemblToSymbol(rownames(sce_object),
                                                   species = selected_species)
    rm(selected_species)
  }
}
rm(nomenclature)

## Add metadata as colData ####
sample_list <- lapply(mat_list, function(mat_name) {sub("_.*", "", mat_name)})
if (metadata == "true") {
  metadata_df <- metadata_df %>% 
    dplyr::filter(sample_id %in% unlist(sample_list))
  SummarizedExperiment::colData(sce_object) <-  DataFrame(metadata_df)
  print("Metadata have been added.")
} else {warning("No metadata in colData.")}

## Cleanup
rm(metadata, metadata_name, metadata_df, sample_list, mat_list)

# QC filters ####
## Minimum genes by cell #### 
min_genes_by_cell_filter <- 
  colSums(counts(sce_object) != 0) > as.numeric(min_genes_by_cell)
sce_object <- sce_object[, min_genes_by_cell_filter]

## Minimum cells expressing a gene ####
min_cell_expressing_gene_filter <- 
  rowSums(counts(sce_object) != 0) > as.numeric(min_cell_expressing_gene)
sce_object <- sce_object[min_cell_expressing_gene_filter, ]

## Remove cells with 0 library size  ####
lib_sizes <- colSums(counts(sce_object))
sce_object <- sce_object[, lib_sizes > 0]

## Log-normalization
sce_object <- computeSumFactors(sce_object, positive = TRUE) ## Add size factors 
logcounts(sce_object) <- 
  logcounts(logNormCounts(sce_object))
print("Raw counts have been log-normalized.")

## Cleanup
rm(min_genes_by_cell, min_genes_by_cell_filter,
   min_cell_expressing_gene, min_cell_expressing_gene_filter,
   lib_sizes)

# HVG filters ####
## Get HVG
gene_var <- 
  suppressWarnings(
    modelGeneVar(logcounts(sce_object))
    ) ## Suppress warning related to ties
hvg_selection <- getTopHVGs(gene_var , n = as.numeric(hvg))

## Apply HVG filter 
sce_object <- 
  sce_object[hvg_selection, ]

## Cleanup 
rm(gene_var, hvg, hvg_selection)
print("SingleCellExperiment object has been filtered")


# Dimensionality reductions ####
## PCA 
if (isTRUE(as.logical(tsne))) {
  pca_data <- prcomp(t(logcounts(sce_object)), retx = TRUE, center = TRUE, rank. = 50)
  reducedDim(sce_object, type = "PCA") <- pca_data$x
  print("PCA has been added")
}

## UMAP 
if (isTRUE(as.logical(umap))) {
  umap_data <- umap(t(logcounts(sce_object)))
  reducedDim(sce_object, type = "UMAP") <- umap_data$layout
  rm(umap, umap_data)
  print("UMAP has been added")
}

## t-SNE 
if (isTRUE(as.logical(tsne))) {
  tsne_data <- Rtsne(pca_data$x[,1:50], pca = FALSE)
  reducedDim(sce_object, type = "TSNE") <- tsne_data$Y
  rm(pca_data, tsne, tsne_data)
  print("t-SNE has been added")
}

# Clustering ####
## Louvain
if (isTRUE(as.logical(louvain))) {
  cluster_louvain <- clusterRows(t(logcounts(sce_object)), 
                                 NNGraphParam(cluster.fun = "louvain"), 
                                 full = FALSE)
  colData(sce_object)$cluster_louvain <- cluster_louvain
  rm(cluster_louvain)
  print("Louvain clustering has been added")
}
rm(louvain)

## Leiden 
if (isTRUE(as.logical(leiden))) {
  cluster_leiden <- clusterRows(t(logcounts(sce_object)), 
                                NNGraphParam(cluster.fun = "leiden"), 
                                full = FALSE)
  colData(sce_object)$cluster_leiden <- cluster_leiden
  rm(cluster_leiden)
  print("Leiden clustering has been added")
}
rm(leiden)


# Cell annotation ####
if (!reference_rna == "none"){
  if (reference_rna == "auto") {
    
    ## Reference RNA expression dataset ####
    if(!exists("parameter_species")) {
      parameter_species <- 
        postprocessing_parameters$alignmentExperiment$selectedOrganism
    }
    if (parameter_species == "Mus_musculus") {
      reference_rna <- as(celldex::MouseRNAseqData(), "SingleCellExperiment")
    }
    if (parameter_species == "Homo_sapiens") {
      reference_rna <- as(celldex::BlueprintEncodeData(), "SingleCellExperiment")
    } 
  }
  
  ## Annotation (fine and pruned) ####
  if ("cluster_leiden" %in% names(colData(sce_object))) {
    annotation_clusters <- colData(sce_object)$cluster_leiden
  } else if ("cluster_louvain" %in% names(colData(sce_object))) {
    annotation_clusters <- colData(sce_object)$cluster_louvain
  } else {annotation_clusters <- NULL} ## Get cluster from metadata
  
  cell_type_prediction_df <- SingleR::SingleR(test = sce_object,
                                              ref = reference_rna,
                                              assay.type.test = "logcounts",
                                              assay.type.ref = "logcounts",
                                              labels = reference_rna$label.fine, 
                                              clusters = annotation_clusters) 
  if (!is.null(annotation_clusters)) {
    cell_type <- cell_type_prediction_df$pruned.labels[annotation_clusters]
  } else {
    cell_type <- cell_type_prediction_df$pruned.labels
  }
  colData(sce_object) <- 
    cbind(colData(sce_object), cell_type) ## Add to the metadata
  
  ## Cleanup 
  rm(annotation_clusters, cell_type_prediction_df, cell_type, parameter_species)
}
rm(reference_rna)


# Add post-processed object to MinIO ####
## Create temporary file
rds_filename <- paste0(postprocessing_folder_name, "/", postprocessing_folder_name, "_postprocessed.rds")
tmp_file <- tempfile(fileext = ".rds")
saveRDS(sce_object, file = tmp_file) ## Write to temporary file
print("SingleCellExperiment has been saved has RDS file")

## Check if temporary file exists
if (file.exists(tmp_file)) {
  print(paste0("Temporary file was created successfully: ", tmp_file))
} else {stop ("Temporary file creation failed.")}

## Save in post-processing bucket
upload(source_file = tmp_file,
       destination_file = rds_filename,
       bucket_name = "post-processing")

## Cleanup
rm(sce_object, rds_filename, tmp_file, 
   postprocessing_folder_name, alignment_folder_name)


# Change status in CouchDB to Done ####
updateCouchdbStatus(document_id = postprocessing_parameters$couchDBId,
                    new_status = "Done")
## Final cleanup
rm(postprocessing_parameters)

