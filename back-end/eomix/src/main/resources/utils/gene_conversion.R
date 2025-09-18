library(aws.s3)
library(biomaRt)

# Function to convert Ensembl gene IDs to gene symbols ####
convertEnsemblToSymbol <- function(genes_to_convert,
                                   species = "mmusculus") {
  
  ## Create bucket if it does not exist ####
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
  
  ## Create conversion table  if it does not exist ####
  if (!aws.s3::object_exists(object = paste0("conversion_table_",  
                                             species_dataset_name, 
                                             ".csv"),
                             bucket = "gene-conversion",
                             use_https = FALSE, 
                             base_url = Sys.getenv("AWS_S3_ENDPOINT"),
                             region = "")) {
    
    ## List of Ensembl hosts to try
    biomart_host_list <- list("https://asia.ensembl.org/",
                              "https://useast.ensembl.org",
                              "https://uswest.ensembl.org")
    
    ## Attempt to connect to a valid host and download a mart
    mart <- NULL
    for (host in biomart_host_list) {
      try_result <- tryCatch(
        {
          biomaRt::useMart(
            biomart = "ENSEMBL_MART_ENSEMBL",
            host = host,
            dataset = species_dataset_name
          )
        },
        error = function(e) {
          message("Failed to connect to host: ", host)
          return(NULL)
        }
      )
      
      if (!is.null(try_result)) {
        mart <- try_result
        message("Connected successfully to host: ", host)
        break
      }
    }
    
    if (is.null(mart)) {
      stop("All Biomart hosts failed. Please check your network or dataset name.")
    }
    
    ## Create conversion table
    if (grepl("mmusculus", species)) {
      conversion_table <- getBM(attributes = c("entrezgene_id",
                                               "mgi_symbol",
                                               "ensembl_gene_id",
                                               "transcript_is_canonical",
                                               "chromosome_name",
                                               "gene_biotype"),
                                mart = mart)
    }
    if (grepl("hsapiens", species)) {
      conversion_table <- getBM(attributes = c("entrezgene_id",
                                               "hgnc_symbol",
                                               "ensembl_gene_id",
                                               "transcript_is_canonical",
                                               "chromosome_name",
                                               "gene_biotype"),
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
    upload(source_file = tmp_file,
           destination_file = csv_filename,
           bucket_name = "gene-conversion")
  }
  
  ## Load conversion table from bucket ####
  conversion_table <- aws.s3::s3read_using(
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
  
  ## Subset conversion table to gene that needs to be converted 
  conversion_table <- 
    conversion_table[conversion_table$ensembl_gene_id %in% genes_to_convert, ]
  conversion_table <- 
    conversion_table[match(genes_to_convert, conversion_table$ensembl_gene_id), ]
  
  # ## Filter conversion table ####
  # ## Remove patch, scaffold and GL sequences 
  # conversion_table <- 
  #   conversion_table[!grepl("CHR|PATCH|KI|GL", 
  #                           conversion_table$chromosome_name), ] 
  # 
  # ## Remove non canonical genes
  # conversion_table <- 
  #   conversion_table[!is.na(conversion_table$transcript_is_canonical), ]
  # 
  # ## Remove pseudogenes 
  # conversion_table <- 
  #   conversion_table[!grepl("pseudogene", conversion_table$gene_biotype), ]

  
  ## Convert Ensembl genes to symbol using the conversion table ####
  ### Deal with ambiguous gene names ####
  
  ## Add unmatched Ensembl IDs to the table ####
  unmatched_genes <- 
    genes_to_convert[!(genes_to_convert %in% conversion_table$ensembl_gene_id)]
  conversion_table$ensembl_gene_id[is.na(conversion_table$ensembl_gene_id)] <- 
    unmatched_genes ## Add unmatched Ensembl IDs to the table
  
  ## Replace unknown symbol by unmatched Ensemble IDs
  conversion_table$symbol[is.na(conversion_table$symbol) |
                            conversion_table$symbol == ""] <- 
    conversion_table$ensembl_gene_id[is.na(conversion_table$symbol) |
                                       conversion_table$symbol == ""]
  
  ## Replace symbols matched by multiple Ensemble IDs by the Ensembl IDs
  conversion_table$symbol[duplicated(conversion_table$symbol)] <- 
    conversion_table$ensembl_gene_id[duplicated(conversion_table$symbol)]
  
  return (conversion_table$symbol)
}
## TODO Cleanup