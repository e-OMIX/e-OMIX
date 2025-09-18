# CouchDB functions

# Import libraries ####
library(sofa)

# Functions ####
updateCouchdbStatus <- function (document_id, new_status){
  ## New connection to  e-OMIX CouchDB 
  couchdb_connection <- sofa::Cushion$new(host = "host.docker.internal",
                                          port = 5984,
                                          user = "YOUR_COUCHDB_USERNAME",
                                          pwd = "YOUR_COUCHDB_PASSWORD")
  print(paste0("CouchDB connection established. CouchDB v", 
               couchdb_connection$version()))
  
  ## Get and update the document
  doc <- sofa::doc_get(couchdb_connection, 
                       "experiment", 
                       docid = document_id)
  
  doc$status <- new_status
  
  sofa::doc_update(cushion  = couchdb_connection,
                   dbname = "experiment",
                   doc = doc,
                   docid = doc$`_id`,
                   rev = doc$`_rev`)
  
  ## Check if it was updated correctly
  updated_doc <- sofa::doc_get(couchdb_connection, 
                               "experiment", 
                               docid = postprocessing_parameters$couchDBId)
  print(paste0("Post-processing status is now set to: ", updated_doc$status))
  rm(doc, updated_doc, couchdb_connection)
}
