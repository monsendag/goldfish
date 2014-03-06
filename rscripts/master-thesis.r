#!/usr/bin/env Rscript

source('functions.r')

results_path <- '~/Projects/goldfish/results'
graphs_path <- '~/Projects/goldfish/graphs'

# list of data set files to plot
vtt <- list()

vtt$cluster7 <- "2013-11-28-142459-VTT36k-7 clusters.csv"

vtt <- lapply(vtt, get_dataset, folder=results_path)


#Threshold 0.4 TanimotoCoefficient
#irPlot(vtt$cluster7, "Precision_Recall", 10, 1)
#irPlot(vtt$cluster7, "Recall", 10)
#irPlot(vtt$cluster7, "Precision", 10)
#irPlot(vtt$unclustered, "Precision_Recall", 0)
#irPlot(vtt$unclustered, "Recall", 10)

irPlot(vtt$cluster7, type="Precision_Recall", k=10, legendPos='bottomLeft', save_path=graphs_path)


#comparePlot("Threshold 0.3 TanimotoCoefficient", "Precision_Recall", vtt)
#comparePlot("Threshold 0.35 TanimotoCoefficient", "Precision_Recall", vtt)
#comparePlot("2NN TanimotoCoefficient", "Precision_Recall", vtt)
#comparePlot("3NN TanimotoCoefficient", "Precision_Recall", vtt)

