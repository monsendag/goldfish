#!/usr/bin/env Rscript

source('functions.r')

# list of data set files to plot
vtt <- list()
#vtt$unclustered <- "2013-11-17-163634-VTT36k-unclustered.csv"
#vtt$cluster2 <- "2013-11-27-222024-VTT36k-2 clusters.csv"
#vtt$cluster3 <- "2013-11-28-012024-VTT36k-3 clusters.csv"
#vtt$cluster5 <- "2013-11-28-061852-VTT36k-5 clusters.csv"
vtt$cluster7 <- "2013-11-28-142459-VTT36k-7 clusters.csv"
#vtt$cluster9 <- "2013-11-29-070254-VTT36k-9 clusters.csv"
#vtt$cluster11 <- "2013-11-30-013920-VTT36k-11 clusters.csv"

#vtt$k2tanimoto.top250.1 <- "archive/2013-12-07-135343-VTT36k-k2-tanimoto-top250-1.csv"
#vtt$k2tanimoto.top250.2 <- "2013-12-07-140109-VTT36k-k2-tanimoto-top250-2.csv"
#vtt$k2tanimoto.user580.top250 <- "2013-12-07-152021-VTT36k-k2tanimoto-user580-top250.csv"
#vtt$k2tanimoto.user580.top250.2 <- "2013-12-07-152317-VTT36k-k2tanimoto-user580-top250-2.csv"
#vtt$threshold04.top200 <- "2013-12-09-133101-VTT36k-threhold04-top200.csv"

vtt <- lapply(vtt, get_dataset)

#df <- vtt$cluster7



vtt$cluster7$data <- vtt$cluster7$data[vtt$cluster7$data$grp != "Threshold 0.35 TanimotoCoefficient", ]
vtt$cluster7$data <- vtt$cluster7$data[vtt$cluster7$data$grp != "Threshold 0.4 TanimotoCoefficient", ]
#vtt$cluster7$data <- vtt$cluster7$data[vtt$cluster7$data$grp != "Threshold 0.3 TanimotoCoefficient", ]

#str(vtt$cluster7[ != "Threshold 0.4 LogLikelihood",])

#Threshold 0.4 TanimotoCoefficient
#irPlot(vtt$cluster7, "Precision_Recall", 10, 1)
#irPlot(vtt$cluster7, "Recall", 10)
#irPlot(vtt$cluster7, "Precision", 10)
#irPlot(vtt$unclustered, "Precision_Recall", 0)
#irPlot(vtt$unclustered, "Recall", 10)
irPlot(vtt$cluster7, "Precision_Recall", 10)


#comparePlot("Threshold 0.3 TanimotoCoefficient", "Precision_Recall", vtt)
#comparePlot("Threshold 0.35 TanimotoCoefficient", "Precision_Recall", vtt)
#comparePlot("2NN TanimotoCoefficient", "Precision_Recall", vtt)
#comparePlot("3NN TanimotoCoefficient", "Precision_Recall", vtt)

