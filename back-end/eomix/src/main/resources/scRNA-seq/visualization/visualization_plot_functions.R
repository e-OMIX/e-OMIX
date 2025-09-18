# Visualization: functions to plot the data

# QC plots ####
## Violin plot for QC ####
createQcViolinPlot <- function(plot_data, 
                               nCount_range = NULL,
                               nFeature_range = NULL, 
                               mito_pc_max_threshold = NULL) {
  
  ## Automatically set ranges 
  if (is.null(nCount_range)) {
    nCount_df <- dplyr::filter(plot_data, metric_name == "nCount")
    nCount_range <- c(min(nCount_df$value), max(nCount_df$value))
    rm(nCount_df)
  }
  if (is.null(nFeature_range)) {
    nFeature_df <- dplyr::filter(plot_data, metric_name == "nFeature")
    nFeature_range <- c(min(nFeature_df$value), max(nFeature_df$value))
    rm(nFeature_df)
  }
  if(is.null(mito_pc_max_threshold)) {mito_pc_max_threshold = 50}
  
  ## Make sure the ranges value are properly ordered
  nCount_range <- nCount_range[order(nCount_range)]
  nFeature_range <- nFeature_range[order(nFeature_range)]
  
  ## Exclude out-of-range values
  library(dplyr)
  plot_data <- plot_data %>%
    dplyr::filter((metric_name == "nCount" 
                   & value > nCount_range[1] 
                   & value < nCount_range[2]) |
                    (metric_name == "nFeature" 
                     & value > nFeature_range[1] 
                     & value < nFeature_range[2]) |
                    (metric_name == "mitochondrial_genes_pc" 
                     & value < mito_pc_max_threshold)) %>%
    transform(metric_name = factor(metric_name, 
                                   levels = c("nCount",
                                              "nFeature",
                                              "mitochondrial_genes_pc")))
  
  ## Compute count per metric
  count_data <- plot_data %>%
    dplyr::group_by(metric_name) %>%
    dplyr::summarize(count = n(), .groups = "drop")
  
  
  options(repr.plot.width = 12, repr.plot.height = 6) ## Adapt plot size
  
  qc_violin_plot <- ggplot( plot_data, aes(x = metric_name, y = value)) +
    geom_violin(scale = "width",
                alpha = 0.75,
                show.legend = FALSE,
                mapping = aes(fill = "red")) +
    facet_wrap(~ metric_name,
               scales = "free",
               labeller = as_labeller(
                 c(mitochondrial_genes_pc = "Percentage",
                   nCount = "Count",
                   nFeature = "Count")),
               strip.position = "left") +
    
    stat_summary(fun.data = "mean_sdl",
                 fun.args = list(mult = 1),
                 geom = "pointrange",
                 color = "black") + 
    theme_bw() +
    theme(text = element_text(),
          strip.background = element_blank(),
          strip.placement = "outside",
          strip.text = element_text(face = "bold"),
          strip.text.x = element_text(face = "bold"),
          axis.title.x = element_blank(),
          axis.title.y = element_blank())
  return(qc_violin_plot)
}

## Violin plots for QC with iSEE ####
createQcViolinPlotiSEE <- function(se, nrow = NULL, ncol = NULL) {
  
  if (!class(se) == "SummarizedExperiment" &
      !class(se) == "SingleCellExperiment") {
    stop("se must be a SummarizedExperiment or SingleCellExperiment object.") 
  } 
  
  se <- sce_object
  
  metadata <- colData(se)
  
  ## Calculate nFeature
  if ("n_genes" %in% names(metadata)) {
    nFeature <- metadata$n_genes
  } else {
    if (!exists("features")) features <- rownames(se)
    if (!exists("cells")) cells <- colnames(se)
    if (!exists("count_matrix")) count_matrix <- counts(se)
    nFeature <- colSums(count_matrix != 0)
  }
  
  ## Calculate nCount (aka count depth)
  if ("n_count" %in% names(metadata)) {
    nCount <- metadata$n_count
  } else {
    if (!exists("features")) features <- rownames(se)
    if (!exists("cells")) cells <- colnames(se)
    if (!exists("count_matrix")) count_matrix <- counts(se)
    nCount <- rowSums(count_matrix) 
  }
  
  ## Calculate percentage of mitochondrial genes
  if ("n_mitochondrial_genes" %in% names(metadata)) {
    mitochondrial_genes_pc <- metadata$n_mitochondrial_genes
  } else {
    if (!exists("features")) features <- rownames(se)
    if (!exists("cells")) cells <- colnames(se)
    if (!exists("count_matrix")) count_matrix <- counts(se)
    #mitochondrial_genes_pc <- getMitochondrialGenesPc(uri)
    
    if(all(grepl("^ensg", features, ignore.case = TRUE))) {
      species  <-  "hsapiens"
      features <- convertEnsemblToSymbol(features, species = species)
    } 
    if(all(grepl("^ensm", features, ignore.case = TRUE))) {
      species <- "mmusculus"
      features <- convertEnsemblToSymbol(features, species = species)
    } 
    
    # Identify mitochondrial genes
    mitochondrial_genes <- grep(pattern = "^mt-", 
                                features,
                                value = TRUE, 
                                ignore.case = TRUE)
    mitochondrial_genes_idx <- which(
      features %in% mitochondrial_genes
    )
    
    # Calculate percentage of mitochondrial genes
    mitochondrial_genes_count_matrix <- count_matrix[, mitochondrial_genes_idx] 
    
    mitochondrial_genes_pc <- 
      rowSums(mitochondrial_genes_count_matrix)/rowSums(count_matrix)
  } ## TODO Functionalize this
  
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
  
  # Render plot
  library(ggplot2)
  createQcViolinPlot(plot_data = plot_data)
}


# # Dimensionality reduction ####
# createDimensionalityReductionDotPlot <- function(plot_data = data.frame(),
#                                                  color = as.character(),
#                                                  shape = as.character()) {
#   
#   dim_1_data <- plot_data[[colnames(plot_data)[1]]]
#   dim_2_data <- plot_data[[colnames(plot_data)[2]]]
#   if(is.na(color[1])) {
#     color_label  <-  ""
#     color  <-  "black"
#     } else {
#     color_label <- color
#     color = plot_data[[color]]
#     }
#   if(is.na(shape[1])) {
#     shape_label <- ""
#     shape <-  "1"
#     } else {
#     shape_label <- shape
#     shape = plot_data[[shape]]
#     }
#   
#   ggplot(plot_data, 
#          aes(x = dim_1_data, y = dim_2_data, color = color, shape = shape)) +
#     geom_point(alpha = 0.5) +
#     xlab(paste0(dim_1)) +
#     ylab(paste0(dim_2)) +
#     labs(color = color_label, shape = shape_label) +
#     scale_color_discrete_qualitative("Dynamic") +
#     coord_fixed(ratio = 1) +
#     theme_bw() +
#     theme(text = element_text(),
#           legend.title = element_text(face = "bold"),
#           legend.text = element_text(size = 5),
#           legend.key.spacing.y = unit(0.01, "cm"))
# }
# 
# 
# # Gene coexpression ####
# ## Multiple genes violin plot ####
# createGeneViolinPlot <- function(plot_data, 
#                                  layer = "counts") {
#   
#   ## Create plot
#   gene_violin_plot <- ggplot(plot_data, aes(x = feature_name, 
#                                             y = value, 
#                                             fill = feature_name)) +
#     geom_violin(
#       scale = "width", 
#       alpha = 0.75, 
#       linewidth = 0.01,
#       adjust = 5,
#       show.legend = FALSE) +
#     stat_summary(fun.data = "mean_sdl",
#                  fun.args = list(mult = 1),
#                  geom = "pointrange",
#                  color = "black") +
#     scale_fill_discrete_qualitative(palette = "Dynamic") +
#     xlab("Gene") +
#     ylab(tools::toTitleCase(layer)) +
#     theme_classic() +
#     theme(text = element_text(),
#           legend.title = element_blank(),
#           legend.position = "none")
#   return(gene_violin_plot)
# }
# 
# ## Panel of correlation plots ####
# createCorrelationPlotPanel <- function (plot_data) {
#   
#   # Generate a list of plots
#   plotted_gene_name <- colnames(plot_data)
#   
#   if (length(plotted_gene_name) < 2) {
#     warning("Less than 2 genes selected. Skipping correlation plot.")
#     return(NULL)  # Return NULL to avoid crashing Shiny
#   }
#   
#   combinations <- combn(plotted_gene_name, 2)
#   plots <- lapply(1:ncol(combinations), function(i)
#   {
#     selected_genes <- as.vector(combinations[, i])
#     print(paste(i, selected_genes, sep = ". "))
#     
#     ## Remove all zeroes 
#     plot_data_filtered <- dplyr::select(plot_data, selected_genes[1], 
#                                         selected_genes[2]) %>%
#       filterOutNoExpression(var1 = selected_genes[1], var2 = selected_genes[2])
#     print(head(plot_data_filtered))
#     
#     if (nrow(plot_data_filtered) == 0) {
#       warning("Filtered data is empty! Skipping this plot.")
#       return(NULL)
#     }
#     
#     assign(
#       paste0("plot_", i), 
#       ggplot(data = plot_data_filtered,
#              aes(x = plot_data_filtered[[1]],
#                  y = plot_data_filtered[[2]])) +
#         geom_point(size = 0.5, alpha = 0.75) +
#         geom_smooth(method = "lm", 
#                     se = TRUE,
#                     color = "darkred") +
#         xlab(selected_genes[1]) +
#         ylab(selected_genes[2]) +
#         theme_bw() +
#         theme(text = element_text(),
#               panel.grid.major = element_blank(),
#               panel.grid.minor = element_blank(),
#               aspect.ratio = 1)
#     )
#   }
#   )
#   
#   ## Determine optimal number of plots
#   if(ncol(combinations) <= 4) {
#     assign("optimal_nrow", (1))
#   } else {
#     if(ncol(combinations)%%2 == 0) {
#       assign("optimal_nrow", (2))
#     } else {
#       if(ncol(combinations)%%3 == 0) {
#         assign("optimal_nrow", (3))
#       }
#     }
#   }
#   
#   ## Arrange plots
#   ggarrange(plotlist = plots, 
#             nrow = optimal_nrow,
#             ncol = ncol(combinations)/optimal_nrow)
# } 
# 
# ## Heatmap of genes correlations ####
# createHeatmap <- function(plot_data, layer = "counts") {
#   ggplot(data = plot_data, aes(x = gene_1, y = gene_2, fill = correlation)) + 
#     geom_tile() +
#     scale_fill_continuous_diverging(palette = "Blue-Red 3") +
#     theme_bw() +
#     theme(text = element_text(),
#           axis.title = element_blank(),
#           axis.text.x = element_text(angle = 90, hjust = 1),
#           legend.title  = element_blank(),
#           legend.text = element_text(size = 10))
# }
# 
# # Cell types ####
# ## Number of cells per cell type box plot ####
# createCellTypeCountPlot <- function(plot_data) {
#   
#   cell_count_plot <- ggplot(plot_data, aes(x = count,
#                                            y = reorder(cell_type, -count), 
#                                            fill = cell_type)) +
#     geom_bar(stat = "identity", width = 0.75, color = "black") +
#     xlab("Cell counts") +
#     ylab("Cell type") +
#     scale_fill_discrete_qualitative("Dynamic") +
#     theme_bw() +
#     theme(aspect.ratio = 1/2,
#           text = element_text(),
#           legend.position = "none",
#           legend.key.spacing.y = unit(0.01, "cm"),
#           panel.grid = element_blank())
#   return(cell_count_plot)
# }
# 
# createCellTypeCountPlotiSEE_alt <- function (se, rows = NULL, columns = NULL) {
#   
#   plot_data <- createCellTypeCountPlot()
# }
# 
# createCellTypeCountPlotiSEE <- function(se, rows = NULL, columns = NULL) {
#   
#   if (!class(se) == "SummarizedExperiment" &
#       !class(se) == "SingleCellExperiment") {
#     stop("se must be a SummarizedExperiment or SingleCellExperiment object.") 
#   } 
#   
#   ## Get cell types
#   metadata <- colData(se)
#   cell_type_col_name <- grep(pattern = "cell.*type[s]?",
#                              names(metadata), 
#                              ignore.case = TRUE,
#                              value = TRUE)
#   cell_type_col_name <- 
#     cell_type_col_name[which.min(nchar(cell_type_col_name))]
#   cell_types <- as.factor(metadata[[cell_type_col_name]])
#   rm(metadata)
#   
#   ## Calculate cell type counts
#   cell_types_summary <- summary(cell_types)[order(summary(cell_types), 
#                                                   decreasing = TRUE)]
#   cell_types_summary <- data.frame(cell_type = names(cell_types_summary), 
#                                    count = cell_types_summary)
#   rm(cell_types, se)
#   row.names(cell_types_summary) <- NULL
#   plot_data <- cell_types_summary
#   
#   cell_count_plot <- ggplot(plot_data, aes(x = count,
#                                            y = reorder(cell_type, -count), 
#                                            fill = cell_type)) +
#     geom_bar(stat = "identity", width = 0.75) +
#     xlab("Cell counts") +
#     ylab("Cell type") +
#     scale_fill_discrete_qualitative("Dynamic") +
#     theme_bw() +
#     theme(aspect.ratio = 1/2,
#           text = element_text(),
#           legend.position = "none",
#           legend.key.spacing.y = unit(0.01, "cm"),
#           panel.grid = element_blank())
#   return(cell_count_plot)
# }
# 
# ## Count depth per cell type box plot ####
# createCellTypeCountDepthPlot <- function (plot_data) {
#   ggplot(plot_data,
#          aes(x = reorder(as.factor(cell_type), -count), 
#              fill = cell_type)) +
#     geom_boxplot(stat = "identity",
#                  width = 0.5,
#                  aes(lower = FirstQu,
#                      upper = ThirdQu,
#                      middle = Median,
#                      ymin = Min,
#                      ymax = Max)) +
#     xlab("Cell type") +
#     ylab("Count per cell") +
#     scale_fill_discrete_qualitative("Dynamic") + 
#     coord_flip() +
#     theme_bw() +
#     theme(legend.position = "none",
#           plot.background = element_blank(),
#           axis.text.x = element_text(angle = 0, size = 10),
#           axis.text.y = element_text(angle = 0, size = 10))
# }
# 
# ## Cell types marker genes plots ####
# createMarkerGenesPlot <- function(plot_data) {
#   
#   ## Find which cell types each genes is a marker for
#   marker_labels <- plot_data %>%
#     group_by(gene) %>%
#     slice_max(mean_expression, n = 1, with_ties = FALSE) %>%
#     select(gene, cell_type)
#   
#   ## Merge with plot_data
#   plot_data <- plot_data %>%
#     left_join(marker_labels, by = "gene",
#               suffix = c("", "_marker"),
#               relationship = "many-to-many")
#   
#   ## Render plot
#   ggplot(data = plot_data, aes(x = gene, y = cell_type)) +
#     geom_point(aes(size = fraction_cells, color = mean_expression)) +
#     scale_color_continuous_diverging(palette = "Blue-Red 3") +
#     xlab("Gene") +
#     ylab("Cell type") +
#     labs(size = "Fraction of cells", colour = "Mean expression") +
#     theme_bw() +
#     theme(axis.text.x = element_text(angle = 90, hjust = 1))
#   ## TODO: add geom_text adding the cell type each gene is marking
# }
# 
# 
