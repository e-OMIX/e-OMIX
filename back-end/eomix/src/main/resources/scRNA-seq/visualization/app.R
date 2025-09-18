# Data visualization with iSEE

# Libraries ####
library(ggplot2)
library(iSEE)
library(iSEEu)
library(aws.s3)
library(shiny)
library(SingleCellExperiment)
print("All packages are loaded.")

## Load scripts ####
source ("visualization_data_functions.R")
source ("visualization_plot_functions.R")
source ("utils/gene_conversion.R")

# Load the object from MinIO ####
## Import SingleCellExperiment file ####
analysis_name <- 
  Sys.getenv("ANALYSIS_NAME")
print(paste0("Analysis name  = ", analysis_name))

## Check if post-processin bucket exist
if (!(aws.s3::bucket_exists("post-processing", 
                            use_https = FALSE,
                            check_region = FALSE,
                            region = ""))) {
  stop("Post-processing bucket does not exist")
} else {
  print("Post-processing bucket reached")
}

## Get object
sce_object_address <- file.path(analysis_name, 
                                paste0(analysis_name,
                                       "_postprocessed.rds"))

sce_object <- s3readRDS(bucket = "post-processing",
                        object = sce_object_address,
                        use_https = FALSE,
                        check_region = FALSE,
                        region = "",
                        base_url = Sys.getenv("AWS_S3_ENDPOINT"),
                        key = Sys.getenv("AWS_ACCESS_KEY_ID"),
                        secret = Sys.getenv("AWS_SECRET_ACCESS_KEY"))
print("SingleCellExperiment object: OK")

## Cleanup
rm(sce_object_address)

## Landing page ####
## Example
# library(scRNAseq)
# all.data <- ls("package:scRNAseq")
# all.data <- all.data[grep("Data$", all.data)]
#
# lpfun <- createLandingPage(
#   seUI=function(id) selectInput(id, "Dataset:", choices=all.data),
#   seLoad=function(x) get(x, as.environment("package:scRNAseq"))()
# )

# ## Test Shiny with hello world ####
# library(shiny)
# 
# 
# ui <- fluidPage(
#   titlePanel("Hello world!"),
#   sidebarLayout(
#     sidebarPanel(
#       sliderInput("obs", "Number of observations:", min = 1, max = 1000, value = 500)
#     ),
#     mainPanel(
#       plotOutput("distPlot")
#     )
#   )
# )
# 
# server <- function(input, output) {
#   output$distPlot <- renderPlot({
#     hist(rnorm(input$obs))
#   })
# }
# options(shiny.trace = TRUE)
# app <- shinyApp(ui = ui, server = server)
# runApp(app, host = "0.0.0.0", port = 3838)         


## Test iSEE with mock data ####
# library(iSEE)
# library(SingleCellExperiment)
#
# app <- iSEE(mockSCE())

## Insert custom panel
QCVIOLINPLOT <- createCustomPlot(createQcViolinPlotiSEE, 
                                 fullName = "QC violin plots")
#QCVIOLINPLOT()

# Render and start app ####
app <- iSEE(
  sce_object,
  landingPage = createLandingPage(),
  #tour = defaultTour(),
  initial = list(
    ColumnDataTable(PanelWidth = 12L),
    
    ReducedDimensionPlot(PanelWidth = 4L,
                         DataBoxOpen = FALSE, 
                         Type = "PCA",
                         XAxis = 1L, YAxis = 2L, 
                         ColorBy="Column data",
                         ColorByColumnData ="cluster_louvain"),
    
    FeatureAssayPlot(PanelWidth = 4L,
                     DataBoxOpen = FALSE,
                     Assay = "logcounts",
                     ColorBy = "Column data"),
    
    ColumnDataPlot(PanelWidth = 4L, 
                   DataBoxOpen = FALSE),
    
    ComplexHeatmapPlot(PanelWidth = 4L),
    
    QCVIOLINPLOT(PanelWidth = 4L)
    
  ),
  appTitle = paste0("e-OMIX: ", analysis_name)
)



