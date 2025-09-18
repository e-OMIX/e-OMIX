# Visualization: functions to create the datasets to plot

# Functions ####

## Function to get main information from objects ####

## Read from AnnData (.h5ad)
readFromH5ad <- function(uri) {
  
  ## Set environment variables for MinIO 
  # minio_setup(access_key_id = access_key_id_minio_eomix2,
  #             secret_access_key = secret_access_key_minio_eomix2,
  #             address = address_minio_eomix2) ## TODO Remove before building the Docker image
  
  ## Read H5AD
  if (grepl("^s3://", uri, ignore.case = TRUE)) {
    bucket_name <- str_extract(uri, "(?<=s3://)[^/]+")
  } else {
    bucket_name <- str_extract(uri, "^[^/]+")
  } ## For case when the address does not start with s3://
  
  if (grepl("^s3://", uri, ignore.case = TRUE)) { 
    object_address <- str_replace(uri, "^s3://[^/]+/", "")
  } else {  
    object_address <- sub("^[^/]+/", "", uri)
  } ## For case when the address does not start with s3://
  
  bucket <- get_bucket(
    bucket = bucket_name, ## Get MinIO bucket from URI
    use_https = FALSE, 
    check_region = FALSE,
    region = ""
  )
  
  anndata_object <- s3read_using(FUN = read_h5ad, 
                                 object = object_address, 
                                 bucket = bucket, 
                                 opts = list(use_https = FALSE, 
                                             check_region = FALSE,
                                             region = ""))
  return(anndata_object)
}

## Read from SingleCellExperiment (.RDS)
readFromSce <- function(uri) {
  
  ## Set environment variables for MinIO 
  # minio_setup(access_key_id = access_key_id_minio_eomix2,
  #             secret_access_key = secret_access_key_minio_eomix2,
  #             address = address_minio_eomix2)
  
  ## Read RDS
  if (grepl("^s3://", uri, ignore.case = TRUE)) {
    bucket_name <- str_extract(uri, "(?<=s3://)[^/]+")
  } else {
    bucket_name <- str_extract(uri, "^[^/]+")
  } ## For case when the address does not start with s3://
  
  if (grepl("^s3://", uri, ignore.case = TRUE)) { 
    object_address <- str_replace(uri, "^s3://[^/]+/", "")
  } else {  
    object_address <- sub("^[^/]+/", "", uri)
  } ## For case when the address does not start with s3://
  
  bucket <- get_bucket(
    bucket = bucket_name, ## Get MinIO bucket from URI
    use_https = FALSE,
    check_region = FALSE,
    region = ""
  )
  
  sce_object <- s3read_using(FUN = readRDS, 
                             object = object_address, 
                             bucket = bucket, 
                             opts = list(use_https = FALSE, 
                                         check_region = FALSE,
                                         region = ""))
  return(sce_object)
}

## Function to obtain feature names as they are formatted in the object
getFeatures <- function (uri = character()) {
  
  ## For AnnData objects:
  if (grepl(".h5ad$", uri, ignore.case = TRUE)) {
    anndata_object <- readFromH5ad(uri)
    features <- anndata_object$var_names
    rm(anndata_object)
  } 
  
  ## For SingleCellExperiment objects:
  if(grepl(".rds", uri, ignore.case = TRUE)) {
    sce_object <- readFromSce(uri)
    features <- rownames(sce_object)
    rm(sce_object)
  }
  
  ## For TileDB-SOMA objects:
  if(!grepl(".h5ad$", uri, ignore.case = TRUE) &&
     !grepl(".rds$", uri, ignore.case = TRUE)) {
    experiment <- SOMAExperimentOpen(uri, 
                                     tiledbsoma_ctx = context, 
                                     mode = "READ")
    ## TODO Re-activate the createMinioContext function
    features <- 
      experiment$
      ms$
      get("RNA")$
      var$
      read()$
      concat()$
      to_data_frame()$
      var_id
    rm(experiment)
  }
  return(features)
}

# ## Function to obtain feature names in the symbol format
# getFeaturesInSymbol <- function(uri) {
#   original_feature_names <- getFeatures(uri)
#   if(sum(
#     (grepl("^[A-Z0-9][A-Za-z0-9._()\\-]+$", original_feature_names)
#      & !startsWith(original_feature_names, "ENS"))
#     | 
#     grepl(
#       "rik$", original_feature_names, ignore.case = TRUE
#     ) 
#     |
#     grepl(
#       "^[mt]", original_feature_names, ignore.case = TRUE
#     )
#   )/length(original_feature_names) > 0.99) {
#     gc()
#     return(original_feature_names)
#   } else {
#     gc()
#     return(geneConverter(genes_to_convert = original_feature_names))
#   }
# }

## Function to obtain cell names as they are formatted in the object
getCells <- function(uri = character()) {
  
  ## For AnnData objects:
  if (grepl(".h5ad$", uri, ignore.case = TRUE)) {
    anndata_object <- readFromH5ad(uri)
    cells <- anndata_object$obs_names
    rm(anndata_object)
  }
  
  ## For SingleCellExperiment objects:
  if(grepl(".rds", uri, ignore.case = TRUE)) {
    sce_object <- readFromSce(uri)
    cells <- colnames(sce_object)
    rm(sce_object)
  }
  
  ## For TileDB-SOMA objects:
  if(!grepl(".h5ad$", uri, ignore.case = TRUE) &&
     !grepl(".rds$", uri, ignore.case = TRUE)) {
    cells <- SOMAExperimentOpen(uri, tiledbsoma_ctx = context, mode = "READ")$
      obs$
      read()$
      concat()$
      to_data_frame()$
      obs_id
  }
  return(cells)
}


## Function to obtain cell types as they are formatted in the object
getCellTypes <- function (uri) {
  
  ## Find where the cell types are stored (shortest name)
  metadata <- getMetadata(uri)
  cell_type_col_name <- grep(pattern = "cell.*type[s]?",
                             names(metadata), 
                             ignore.case = TRUE,
                             value = TRUE)
  
  ## Select the column with the shortest name as the main cell types column
  cell_type_col_name <- cell_type_col_name[which.min(nchar(cell_type_col_name))]
  
  ## Retrieve cell types
  return(unlist(metadata[cell_type_col_name]))
}

## Function to rename a vector of gene names in symbols (TileDB only)
# renameVariablesInSymbol <- function(uri, species) {
#   if (grepl(".h5ad$", uri, ignore.case = TRUE) |
#       grepl(".rds$", uri, ignore.case = TRUE)) {
#     stop("Renaming features is only possible for TileDB objects")
#   }
#   
#   SOMAExperimentOpen(uri, 
#                      tiledbsoma_ctx = context, 
#                      mode = c("WRITE"))$update_var(
#                        measurement_name = "RNA",
#                        values = data.frame(
#                          "var_id" = convertEnsemblToSymbol(getFeatures(uri),
#                                                            species = species)
# 
#                        )
#                      )
# }

## Function to obtain the count matrix from the object
getCountMatrix <- function(uri = character(), 
                           cell_selection = NULL,
                           feature_selection = NULL) {
  
  # For AnnData objects: 
  if (grepl(".h5ad$", uri, ignore.case = TRUE)) {
    anndata_object <- readFromH5ad(uri)
    
    ## Is there is no selection, select all cells and/or all features
    if (is.null(cell_selection)) {
      cell_selection <- getCells(uri)
    }
    if (is.null(feature_selection)) {
      feature_selection <- getFeatures(uri)
    }
    
    ## Convert feature selection in indices (if dataset not in symbols) 
    # if (is.character(feature_selection)) {
    #   features_in_dataset <- getFeaturesInSymbol(uri)
    #   feature_selection <- 
    #     unname(sapply(feature_selection, 
    #                   function (x) {
    #                     as.numeric(which(features_in_dataset == x))
    #                   }))
    # } ## Not necessary anymore
    
    count_matrix <- anndata_object$X
    count_matrix <- as(count_matrix, "CsparseMatrix")
    count_matrix <- count_matrix[cell_selection, feature_selection]
    #colnames(count_matrix) <- geneConverter(colnames(count_matrix))
    
  }
  
  ## For SingleCellExperiment objects:
  if (grepl(".rds$", uri, ignore.case = TRUE)) {
    sce_object <- readFromSce(uri)
    
    ## Is there is no selection, select all cells and/or all features
    if (is.null(feature_selection)) {
      feature_selection <- getFeatures(uri)}
    if (is.null(cell_selection)) {cell_selection <- getCells(uri)}
    
    ## Transpose and subset the count matrix
    count_matrix <- t(counts(sce_object))
    count_matrix <- as(count_matrix, "CsparseMatrix")
    count_matrix  <- count_matrix <- count_matrix[cell_selection, 
                                                  feature_selection]
  }
  
  ## For TileDB-SOMA objects: 
  if (!grepl(".h5ad$", uri, ignore.case = TRUE) &&
      !grepl(".rds$", uri, ignore.case = TRUE)) {
    experiment <- SOMAExperimentOpen(uri, 
                                     tiledbsoma_ctx = context, 
                                     mode = "READ")
    
    ## Convert cell and feature selections in indices (if needed) : 
    if(is.null(cell_selection)) {
      cell_selection_idx <- 
        seq_along(getCells(uri)) - 1 # -1 because TileDB SOMA is zero-based
    } else {
      if (is.character(cell_selection)) {
        cells_in_dataset <- getCells(uri)
        cell_selection_idx <- unname(sapply(cell_selection, 
                                            function (x) {
                                              as.numeric(
                                                which(cells_in_dataset == x) - 1)
                                            }))
      } else {
        if (is.integer(cell_selection)) {
          cell_selection_idx <- cell_selection
        } else {
          stop("Cell selection must be a character or integer vector")
        }
      }
    }
    
    if(is.null(feature_selection)) {
      feature_selection_idx <- 
        seq_along(getFeatures(uri)) - 1 # - 1 because TileDB-SOMA is zero-based
      
    } else { 
      if (is.character(feature_selection)) {
        features_in_dataset <- getFeatures(uri)
        feature_selection_idx <- 
          unname(sapply(feature_selection, 
                        function (x) {
                          as.numeric(which(features_in_dataset == x) - 1)
                        })) ## TODO Check if it works with getFeaturesInSymbol()
      } else {
        if (is.integer(feature_selection)) {
          feature_selection_idx <- feature_selection
        } else {
          stop("Feature selection must be a character or integer vector")
        }
      } 
    }
    
    ## Subset the count matrix and export as sparse matrix
    query <- SOMAExperimentAxisQuery$new(
      experiment = experiment,
      measurement_name = "RNA",
      obs_query = SOMAAxisQuery$new(coords = cell_selection_idx),
      var_query = SOMAAxisQuery$new(coords = feature_selection_idx)
    )
    count_matrix <- query$to_count_matrix(collection = "X", 
                                          layer_name = "counts",
                                          obs_index = "obs_id",
                                          var_index = "var_id")
    colnames(count_matrix) <- geneConverter(colnames(count_matrix))
  }
  
  return(count_matrix)
}


getMetadata <- function (uri = character(), categorical = FALSE) {
  
  ## For AnnData objects:
  if (grepl(".h5ad$", uri, ignore.case = TRUE)) {
    anndata_object <- readFromH5ad(uri)
    metadata <- anndata_object$obs
    rm(anndata_object)
  } 
  
  ## For SingleCellExperiment objects:
  if (grepl(".rds$", uri, ignore.case = TRUE)) {
    sce_object <- readFromSce(uri)
    metadata <- colData(sce_object)
    rm(sce_object)
  } 
  
  ## For TileDB-SOMA objects:
  if (!grepl(".h5ad$", uri, ignore.case = TRUE) &&
      !grepl(".rds$", uri, ignore.case = TRUE)) {
    experiment <- SOMAExperimentOpen(uri, tiledbsoma_ctx = context, mode = "READ")
    metadata <- 
      experiment$
      obs$
      read()$
      concat()$
      to_data_frame()
    rm(experiment)
  }
  
  ## Retrieve only categorical metadata, if specified
  if (isTRUE(categorical)) {
    metadata <- metadata[sapply(metadata, 
                                function(x) {(is.character(x) 
                                              | is.factor(x) 
                                              | is.numeric.Date(x) 
                                              | is.logical(x))})]
  }
  return(metadata)
}

getDimensionalityReductionMethods <- function(uri) {
  
  ## For AnnData objects:
  if (grepl(".h5ad$", uri, ignore.case = TRUE)) {
    anndata_object <- readFromH5ad(uri)
    dim_reduc_methods <- names(anndata_object$obsm)
  } 
  
  ## For SingleCellExperiment objects:
  if (grepl(".rds$", uri, ignore.case = TRUE)) {
    sce_object <- readFromSce(uri)
    dim_reduc_methods <- reducedDimNames(sce_object)
  }
  
  ## For TileDB-SOMA objects: 
  if (!grepl(".h5ad$", uri, ignore.case = TRUE) &&
      !grepl(".rds$", uri, ignore.case = TRUE)) {
    
    experiment <-  SOMAExperimentOpen(uri, 
                                      tiledbsoma_ctx = context, 
                                      mode = "READ")
    dim_reduc_methods <- experiment$
      ms$
      get("RNA")$
      obsm$
      to_data_frame()$
      name
  }
  return(dim_reduc_methods)
} 


## General use functions ####
### Function to convert Ensembl gene IDs to gene symbols ####
convertEnsemblToSymbol <- function(genes_to_convert,
                                   species = "mmusculus") {
  
  # Create bucket if it does not exist 
  if(!aws.s3::bucket_exists("gene-conversion", 
                            use_https = FALSE, 
                            base_url = Sys.getenv("AWS_S3_ENDPOINT"),
                            region = "")) {
    aws.s3::put_bucket(bucket = "gene-conversion", 
                       use_https = FALSE, 
                       base_url = Sys.getenv("AWS_S3_ENDPOINT"),
                       region = "")
  }
  
  species_dataset_name <- paste0(species, "_gene_ensembl")
  
  ## Create converstion table  if it does not exist
  if (!aws.s3::object_exists(object = paste0("conversion_table_",  
                                             species_dataset_name, 
                                             ".csv"),
                             bucket = "gene-conversion",
                             use_https = FALSE, 
                             base_url = Sys.getenv("AWS_S3_ENDPOINT"),
                             region = "")) {
    
    ## Download the gene conversion table
    biomart_host_list <- list("https://asia.ensembl.org/",
                              "https://useast.ensembl.org")
    
    mart <- useMart(biomart = "ENSEMBL_MART_ENSEMBL", 
                    host = biomart_host_list[[1]],
                    dataset = species_dataset_name) ## TODO: try different mirror if host not responding
    
    if (grepl("mmusculus", species)) {
      conversion_table <- getBM(attributes = c("entrezgene_id",
                                               "mgi_symbol",
                                               "ensembl_gene_id"),
                                mart = mart)
    }
    if (grepl("hsapiens", species)) {
      conversion_table <- getBM(attributes = c("entrezgene_id",
                                               "hgnc_symbol",
                                               "ensembl_gene_id"),
                                mart = mart)
    }
    
    assign(paste0("conversion_table_", species_dataset_name), 
           conversion_table)
    
    ## Place the conversion tables in a bucket
    csv_filename <- paste0("conversion_table_", 
                           species_dataset_name, 
                           ".csv")
    tmp_file <- tempfile(fileext = ".csv")
    write.csv(get(paste0("conversion_table_", 
                         species_dataset_name)),
              row.names = FALSE,
              quote = FALSE, 
              file = tmp_file) ## Write to temporary file
    
    aws.s3::put_object(file = tmp_file,
                       object = csv_filename,
                       bucket = "gene-conversion",
                       base_url = Sys.getenv("AWS_S3_ENDPOINT"),
                       region = "",
                       use_https = FALSE,
                       verbose = FALSE)
  }
  
  ## Load conversion table from bucket 
  conversion_table <- minio.s3::s3read_using(
    FUN = function(x) {read.csv(file = x)},
    object = paste0("conversion_table_", 
                    species,
                    "_gene_ensembl.csv"),
    bucket = "gene-conversion",
    opts = list(use_https = FALSE, 
                base_url = Sys.getenv("AWS_S3_ENDPOINT"),
                region = "")
  ) 
  
  ## Rename species-specific symbol name with "symbol"
  colnames(conversion_table)[grep("symbol", 
                                  colnames(conversion_table), 
                                  ignore.case = TRUE)] <- "symbol"
  
  ## Convert Ensembl genes to symbol using the conversion table
  conversion_table <- 
    conversion_table[conversion_table$ensembl_gene_id %in% genes_to_convert, ]
  conversion_table <- 
    conversion_table[match(genes_to_convert, conversion_table$ensembl_gene_id), ] 
  unmatched_genes <- 
    genes_to_convert[!(genes_to_convert %in% conversion_table$ensembl_gene_id)]
  conversion_table$ensembl_gene_id[is.na(conversion_table$ensembl_gene_id)] <- 
    unmatched_genes ## Deal with gene names not found in conversion table
  conversion_table$symbol[is.na(conversion_table$ensembl_gene_id)] <- 
    unmatched_genes
  conversion_table$symbol[is.na(conversion_table$symbol) |
                            conversion_table$symbol == ""] <- 
    conversion_table$ensembl_gene_id[is.na(conversion_table$symbol) |
                                       conversion_table$symbol == ""]
  
  return (conversion_table$symbol)
}

### Function to remove all genes with zero counts ####
filterOutNoExpression <- function (data, var1, var2) {
  data %>%
    dplyr::filter(!(!!sym(var1) == 0 & !!sym(var2) == 0))
}

## Functions to calculate the percentage of mitochondrial genes ####
getMitochondrialGenesPc <- function (uri) {
  
  ## Convert features to symbol if needed
  features <- getFeatures(uri)
  if(all(grepl("^ensg", features, ignore.case = TRUE))) {
    features <- convertEnsemblToSymbol(features, species = "hsapiens")
  }
  if(all(grepl("^ensm", features, ignore.case = TRUE))) {
    features <- convertEnsemblToSymbol(features, species = "mmusculus")
  }
  
  # Identify mitochondrial genes
  mitochondrial_genes <- grep(pattern = "^mt-", 
                              features,
                              value = TRUE, 
                              ignore.case = TRUE)
  mitochondrial_genes_idx <- which(
    features %in% mitochondrial_genes
  )
  
  # Calculate percantage of mitochondrial genes
  if (!exists("count_matrix")) {count_matrix <- getCountMatrix(uri)}
  mitochondrial_genes_count_matrix <- count_matrix[, mitochondrial_genes_idx] 
  return(rowSums(mitochondrial_genes_count_matrix)/rowSums(count_matrix))
}

## Functions that create a data frame for QC plot ####
createQcData <- function(uri) {
  
  metadata <- getMetadata(uri)
  
  ## Calculate nFeature
  if ("n_genes" %in% names(metadata)) {
    nFeature <- metadata$n_genes
  } else {
    if (!exists("feature_names")) feature_names <- getFeatures(uri)
    if (!exists("cell_names")) cell_names <- getCells(uri)
    if (!exists("count_matrix")) count_matrix <- getCountMatrix(uri) 
    nFeature <- colSums(count_matrix != 0)
  }
  
  ## Calculate nCount (aka count depth)
  if ("n_count" %in% names(metadata)) {
    nCount <- metadata$n_count
  } else {
    if (!exists("feature_names")) feature_names <- getFeatures(uri)
    if (!exists("cell_names")) cell_names <- getCells(uri)
    if (!exists("count_matrix")) count_matrix <- getCountMatrix(uri) 
    nCount <- rowSums(count_matrix) 
  }
  
  ## Calculate percentage of mitochondrial genes
  if ("n_mitochondrial_genes" %in% names(metadata)) {
    mitochondrial_genes_pc <- metadata$n_mitochondrial_genes
  } else {
    if (!exists("feature_names")) feature_names <- getFeatures(uri)
    if (!exists("cell_names")) cell_names <- getCells(uri)
    if (!exists("count_matrix")) count_matrix <- getCountMatrix(uri) 
    mitochondrial_genes_pc <- getMitochondrialGenesPc(uri)
  }
  
  ## Cleanup 
  if (exists("feature_names")) rm(feature_names)
  if (exists("cell_names")) rm(cell_names)
  if (exists("count_matrix")) rm(count_matrix)
  
  ## Assemble dataset 
  plot_data <- rbind(data.frame("metric_name" = rep("nCount", 
                                                    length(nCount)), 
                                "value" = nCount),
                     data.frame("metric_name" = rep("nFeature", 
                                                    length(nFeature)), 
                                "value" = nFeature),
                     data.frame("metric_name" = rep("mitochondrial_genes_pc", 
                                                    length(mitochondrial_genes_pc)),
                                "value" = mitochondrial_genes_pc))
  return(plot_data)
}

createQcDataiSEE <- function (se, nrow = NULL, col= NULL) {
  metadata <- colData(sce_object)
  
  ## Calculate nFeature
  if ("n_genes" %in% names(metadata)) {
    nFeature <- metadata$n_genes
  } else {
    if (!exists("features")) features <- rownames(sce_object)
    if (!exists("cells")) cells <- colnames(sce_object)
    if (!exists("count_matrix")) count_matrix <- counts(sce_object)
    nFeature <- colSums(count_matrix != 0)
  }
  
  ## Calculate nCount (aka count depth)
  if ("n_count" %in% names(metadata)) {
    nCount <- metadata$n_count
  } else {
    if (!exists("features")) features <- rownames(sce_object)
    if (!exists("cells")) cells <- colnames(sce_object)
    if (!exists("count_matrix")) count_matrix <- counts(sce_object)
    nCount <- rowSums(count_matrix) 
  }
  
  ## Calculate percentage of mitochondrial genes
  if ("n_mitochondrial_genes" %in% names(metadata)) {
    mitochondrial_genes_pc <- metadata$n_mitochondrial_genes
  } else {
    if (!exists("features")) features <- rownames(sce_object)
    if (!exists("cells")) cells <- colnames(sce_object)
    if (!exists("count_matrix")) count_matrix <- counts(sce_object)
    #mitochondrial_genes_pc <- getMitochondrialGenesPc(uri)
    
    if(all(grepl("^ensg", features, ignore.case = TRUE))) {
      features <- convertEnsemblToSymbol(features, species = "hsapiens")
    }
    if(all(grepl("^ensm", features, ignore.case = TRUE))) {
      features <- convertEnsemblToSymbol(features, species = "mmusculus")
    }
    
    # Identify mitochondrial genes
    mitochondrial_genes <- grep(pattern = "^mt-", 
                                features,
                                value = TRUE, 
                                ignore.case = TRUE)
    mitochondrial_genes_idx <- which(
      features %in% mitochondrial_genes
    )
    
    # Calculate percantage of mitochondrial genes
    if (!exists("count_matrix")) {count_matrix <- getCountMatrix(uri)}
    mitochondrial_genes_count_matrix <- count_matrix[, mitochondrial_genes_idx] 
    
    mitochondrial_genes_pc <- 
      rowSums(mitochondrial_genes_count_matrix)/rowSums(count_matrix)
  }
  
  ## Cleanup 
  if (exists("features")) rm(features)
  if (exists("cells")) rm(cells)
  if (exists("count_matrix")) rm(count_matrix)
  
  ## Assemble dataset 
  plot_data <- rbind(data.frame("metric_name" = rep("nCount", 
                                                    length(nCount)), 
                                "value" = nCount),
                     data.frame("metric_name" = rep("nFeature", 
                                                    length(nFeature)), 
                                "value" = nFeature),
                     data.frame("metric_name" = rep("mitochondrial_genes_pc", 
                                                    length(mitochondrial_genes_pc)),
                                "value" = mitochondrial_genes_pc))
  return(plot_data)
}

# calculateHvgNumbers <- function(uri, hvg_number) { ## TODO adapt for SCE or delete
#   
#   ## For TileDB-SOMA objects: 
#   if (grepl("^s3://", uri, ignore.case = TRUE)) {
#     
#     ## Get the data as a SingleCellExperiment
#     query <- SOMAExperimentAxisQuery$new(experiment = SOMAExperimentOpen(uri),
#                                          measurement_name = "RNA")
#     sce_object <- query$to_single_cell_experiment()
#     
#     ## Add log normalization if not there 
#     
#     layers_in_dataset <- SOMAExperimentOpen(uri)$ms$get("RNA")$X$names()
#     if (!("logcounts" %in% layers_in_dataset)) {
#       scesce_object <- logNormCounts(sce_object)
#     }
#     rm(layers_in_dataset)
#     
#     ## Model gene variance
#     dec.sce <- modelGeneVar(sce_object)
#     rm(sce)
#     hvgs_id <- getTopHVGs(dec.sce, n = hvg_number)
#   }
#   
#   if (grepl(".h5ad$", uri, ignore.case = TRUE)) {
#     anndata_object <- anndataR::read_h5ad(uri)
#     sce_object <- anndataR::to_SingleCellExperiment(
#       adata = anndata_object,
#       assays_mapping = setNames(anndata_object$layers_keys(), 
#                                 anndata_object$layers_keys()), 
#       colData_mapping = setNames(anndata_object$obs_keys(), 
#                                  anndata_object$obs_keys()),
#       rowData_mapping = setNames(anndata_object$var_keys(), 
#                                  anndata_object$var_keys()),
#       reduction_mapping = setNames(anndata_object$obsm_keys(),
#                                    anndata_object$obsm_keys()),
#       colPairs_mapping = setNames(anndata_object$obs_keys(),
#                                   anndata_object$obs_keys()),
#       rowPairs_mapping = setNames(anndata_object$varp_keys(),
#                                   anndata_object$varp_keys()),
#       metadata_mapping = setNames(anndata_object$uns_keys(),
#                                   anndata_object$uns_keys())
#     )
#     # basilisk::basiliskRun(
#     #   fun = function (object_to_convert) {
#     #     sce_object <- zellkonverter::AnnData2SCE(adata = object_to_convert)
#     #   },
#     #   env = zellkonverterAnnDataEnv(), 
#     #   object_to_convert = anndata_object
#     # )
#     # rm(anndata_object)
#     
#   }
# }

## Functions that create a data frame with the number of cells for each type
createCellTypeCountData <- function(uri) {
  cell_types <- as.factor(getCellTypes(uri))
  cell_types_summary <- summary(cell_types)[order(summary(cell_types), 
                                                  decreasing = TRUE)]
  cell_types_summary <- data.frame(cell_type = names(cell_types_summary), 
                                   count = cell_types_summary)
  rm(cell_types)
  row.names(cell_types_summary) <- NULL
  return(cell_types_summary)
}

## Functions to create a data frame with embeddings and metadata ####
getEmbeddings <- function(uri, method = character(), n_dim = 2) {
  
  ## Check whether there are n_dim > 1
  if (n_dim < 2) {
    stop("n_dim must be greater than 1")
  }
  
  ## For AnnData objects:
  if (grepl(".h5ad$", uri, ignore.case = TRUE)) {
    anndata_object <- readFromH5ad(uri)
    
    ## Check whether there are embeddings 
    if (length(anndata_object$obsm) == 0) {
      warning("No embeddings available")
      return(NULL)
    }
    
    ## Check method validity
    if (is.na(method[1])) {
      method <- names(anndata_object$obsm)[1] ## Use the first method by default
    } else {
      if (!(method %in% names(anndata_object$obsm))) {
        stop(paste("Method", method, "is not present"))
      } 
    }
    
    ## Retrieve embeddings
    embeddings_df <- as.data.frame(anndata_object$obsm[[method]])
    rm(anndata_object)
    if (dim(embeddings_df)[2] > n_dim) {
      embeddings_df <- embeddings_df[, 1:n_dim]
    }   ## Use n_dim only if there are more than two
    
    ## Rename dimensions
    names(embeddings_df) <- 
      paste0(method, "_", seq_len(ncol(embeddings_df)))
  }
  
  ## For SingleCellExperiment objects:
  if (grepl(".rds$", uri, ignore.case = TRUE)) {
    sce_object <- readFromSce(uri)
    
    ## Check whether there are embeddings 
    if (length(reducedDimNames(sce_object)) == 0) {
      warning("No embeddings available")
      return(NULL)
    }
    
    ## Check method validity
    if (is.na(method[1])) {
      method <- reducedDimNames(sce_object)[1] ## Use the first method by default
    } else {
      if (!(method %in% reducedDimNames(sce_object))) {
        stop(paste("Method", method, "is not present"))
      } 
    }
    
    ## Retrieve embeddings
    embeddings_df <- 
      as.data.frame(reducedDim(sce_object, type = method))
    
    rm(sce_object)
    if (dim(embeddings_df)[2] > n_dim) {
      embeddings_df <- embeddings_df[, 1:n_dim]
    }   ## Use n_dim only if there are more than two
    
    ## Rename dimensions
    names(embeddings_df) <- 
      paste0(method, "_", seq_len(ncol(embeddings_df)))
  }
  
  ## For TileDB-SOMA objects: 
  if (!grepl(".h5ad$", uri, ignore.case = TRUE) &&
      !grepl(".rds$", uri, ignore.case = TRUE)) {
    
    ## Check whether there are embeddings 
    if (!dir.exists(file.path(uri, "ms", "RNA", "obsm"))) {
      warning("No embeddings available")
      return(NULL)
    } 
    
    ## Retrieve embeddings
    experiment <- SOMAExperimentOpen(uri, 
                                     tiledbsoma_ctx = context, 
                                     "READ")
    embeddings_matrix <- experiment$
      ms$
      get("RNA")$
      obsm$
      get(method)$
      read()$
      sparse_matrix()$
      concat()
    if (dim(embeddings_matrix)[2] > n_dim) {
      embeddings_matrix <- embeddings_matrix[, 1:n_dim]
    }   ## Use n dimensions only if there are more than 2
    embeddings_df <- as.data.frame(as.matrix(embeddings_matrix))
    rm(experiment, embeddings_matrix)
    
    ## Rename dimensions
    names(embeddings_df) <- paste0(method, 
                                   "_", 
                                   seq_len(ncol(embeddings_df)))
  }
  
  ## Add categorical metadata 
  metadata <- getMetadata(uri, categorical = TRUE) 
  embeddings_df <- cbind(embeddings_df, metadata)
  return(embeddings_df)
}



## Function that create a long-format data frame for a subset of genes ####
## For gene expression violin plot
createLongGeneTable <- function(uri, 
                                feature_selection = NULL, 
                                layer = "counts") {
  
  ## Select all features if no selection is provided
  if (is.null(feature_selection)) {feature_selection = getFeatures(uri)}
  
  # if(!exists("subset_count_matrix")) {
  #   subset_count_matrix <- getCountMatrix(uri, 
  #                                         feature_selection = feature_selection)
  # } ## Only load the count matrix if it does not exist in the environment
  
  subset_count_matrix <- getCountMatrix(uri, 
                                        feature_selection = feature_selection)
  
  ## TODO: return an error if non of the requested genes could not be found 
  ## TODO: return a warning if some of the requested genes could not be found
  ## TODO: specify which genes could not be found
  
  ## Transform matrix in long data frame         
  subset_count_df <- as.data.frame(as.matrix(subset_count_matrix))
  long_subset_count_df <- 
    pivot_longer(subset_count_df,
                 cols = colnames(subset_count_df),
                 cols_vary = "slowest",
                 names_to = "feature_name",
                 values_to = "value")
  
  return(long_subset_count_df)
  rm(subset_count_matrix, subset_count_df, long_subset_count_df)
}

## Function that create the data set for gene expression violin plot ####
createGeneViolinPlotData <- function (uri, 
                                      feature_selection = NULL, 
                                      layer = "counts") {
  createLongGeneTable(uri = uri,
                      feature_selection = feature_selection, 
                      layer= layer)
}

## Function that create a co-expressing genes table  ####
## For correlation plots panel
createCoexpressingGeneDataFrame <- function (uri,
                                             feature_selection = NULL, 
                                             layer = "counts") {
  
  ## TODO: return an error if non of the requested genes could not be found 
  ## TODO: return a warning if some of the requested genes could not be found
  ## TODO: specify which genes could not be found
  
  ## Select all features if no selection is provided
  if (is.null(feature_selection)) {feature_selection = getFeatures(uri)}
  
  ## Create data frame set in long format         
  if(!exists("subset_count_matrix")) {
    subset_count_matrix <- getCountMatrix(uri, 
                                          feature_selection = feature_selection)
  }
  
  return(as.data.frame(as.matrix(subset_count_matrix)))
}

## Function to create data for correlation plots panel ####
createCorrelationPlotPanelData <- function (uri = as.character(),
                                            feature_selection = NULL,
                                            layer= "counts") {
  createCoexpressingGeneDataFrame(uri,
                                  feature_selection,
                                  layer)
}

## Function that creates a correlation table for a list of genes ####
## For Heatmap 
createCorrelationGeneTable <- function(uri = character(),
                                       feature_selection = NULL, 
                                       layer = "counts") {
  
  ## TODO: return an error if non of the requested genes could not be found 
  ## TODO: return a warning if some of the requested genes could not be found
  ## TODO: specify which genes could not be found
  
  if (is.null(feature_selection)) {feature_selection <- getFeatures(uri)}
  
  if(!exists("subset_count_matrix")) {
    subset_count_matrix <- getCountMatrix(uri = uri,
                                          feature_selection = feature_selection)
  }
  
  ## Function to calculate correlation matrix omitting pairs of zeroes
  filteredCor <- function(mat) {
    n <- ncol(mat)
    cor_matrix <- matrix(NA, n, n, dimnames = list(colnames(mat), 
                                                   colnames(mat)))
    
    for (i in 1:n) {
      for (j in i:n) {
        valid_rows <- !(mat[, i] == 0 & mat[, j] == 0) ## Select non-zero pairs
        if (sum(valid_rows) > 1) {  ## Ensure at least two values remain for correlation
          cor_matrix[i, j] <- cor(mat[valid_rows, i], 
                                  mat[valid_rows, j], 
                                  method = "pearson")
          cor_matrix[j, i] <- cor_matrix[i, j]  # Symmetric matrix
        }
      }
    }
    return(cor_matrix)
  }
  
  ## Calculate correlation matrix
  correlation_matrix <- filteredCor(as.matrix(subset_count_matrix))
  correlation_matrix <- rownames_to_column(as.data.frame(correlation_matrix),
                                           var = "gene_1")
  
  return(pivot_longer(as.data.frame(correlation_matrix), 
                      cols = -gene_1,
                      names_to = "gene_2", 
                      values_to = "correlation"))
}

## Function that summarizes number of counts (count depth) per cell types #### 
createCellTypeCountDepthData <- function(uri, layer = "counts") {
  
  ## Get data and rename it by cell types
  cell_types <- getCellTypes(uri)
  if(is.null(cell_types)) {stop("No cell types annotation found")}
  
  # if(!exists("count_matrix")) {
  #   count_matrix <- getCountMatrix(uri)
  #   } ## Load count matrix if it does not exist in the environment
  
  count_matrix <- getCountMatrix(uri) ## Load count matrix
  
  dimnames(count_matrix) <- list(cell_types, NULL)
  
  ## Calculate number of each cell types
  cell_type_summary_nCount_list <- tapply(rowSums(count_matrix),
                                          names(rowSums(count_matrix)),
                                          summary)
  # cell_type_summary_nFeatures_list <- tapply(colSums(count_matrix > 0),
  #                                         names(colSums(count_matrix > 0)),
  #                                         summary)
  
  ## Transform the list in a data frame
  cell_type_summary_nCount_df <- purrr::map_dfr(cell_type_summary_nCount_list,
                                                ~ as.list(.x), 
                                                .id = "name")
  
  ## Rename columns 
  colnames(cell_type_summary_nCount_df) <- c("cell_type",
                                             "Min",
                                             "FirstQu",
                                             "Median",
                                             "Mean",
                                             "ThirdQu",
                                             "Max")
  
  ## Add a column with the number of cells
  cell_count_df <- createCellTypeCountData(uri)
  cell_type_summary_nCount_df <- merge(cell_type_summary_nCount_df, 
                                       cell_count_df, 
                                       by = "cell_type")
  
  return(cell_type_summary_nCount_df)
}

## Function to calculate top marker genes for each cell type ####
createMarkerGenesData <- function (uri, n = 2) {
  
  # Load Data
  if(!exists("count_matrix")) {count_matrix <- getCountMatrix(uri)}
  cell_types <- getCellTypes(uri)
  n <- 2  # Number of top genes per cell type
  
  # Filter out mitochondrial genes
  mt_genes <- grep("^mt", colnames(count_matrix), ignore.case = TRUE)
  if (length(mt_genes) > 0) {
    count_matrix <- count_matrix[, -mt_genes]
  }
  
  # Convert sparse matrix to dense matrix (if necessary)
  if (!inherits(count_matrix, "dgCMatrix")) {
    count_matrix <- as(count_matrix, "CsparseMatrix")
  }
  
  # Identify the top n genes per cell type
  library(dplyr)
  top_genes_list <- unique(cell_types) %>%
    lapply(function(cell_type) {
      idx <- which(cell_types == cell_type)
      sub_matrix <- count_matrix[idx, ]
      
      total_expression <- colSums(sub_matrix)
      gene_expressing_cells <- colSums(sub_matrix > 0)
      
      mean_expression <- total_expression / pmax(gene_expressing_cells, 1) 
      top_genes <- names(sort(mean_expression, decreasing = TRUE)[1:n])  
      top_genes
    })
  
  ## Get all unique top-expressing genes across cell types
  top_genes_all <- unique(unlist(top_genes_list))
  
  ## Compute mean expression and fraction of expressing cells for all selected genes
  marker_count_matrix <- count_matrix[, top_genes_all, drop = FALSE]
  
  summary_data <- unique(cell_types) %>%
    lapply(function(cell_type) {
      idx <- which(cell_types == cell_type)
      sub_matrix <- marker_count_matrix[idx, ]
      
      total_expression <- colSums(sub_matrix)
      gene_expressing_cells <- colSums(sub_matrix > 0)
      
      mean_expression <- total_expression / pmax(gene_expressing_cells, 1)
      fraction_cells <- gene_expressing_cells / length(idx)
      
      data.frame(cell_type = cell_type, 
                 gene = top_genes_all, 
                 mean_expression = mean_expression[top_genes_all], 
                 fraction_cells = fraction_cells[top_genes_all])
    }) %>%
    bind_rows()
}

