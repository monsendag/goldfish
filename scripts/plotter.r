#!/usr/bin/env Rscript

library(data.table)
library(ggplot2)

##
# returns top k evaluations for each group
get_top <- function(df, type, k) {

    if(type=="Precision/Recall") {
      top <- df[,sum(get("Precision") + get("Recall")), by=grp]
    }
    else if(type=="Recall") {
      top <- df[,sum(get("Recall")), by=grp]
    }
    else {
      top <- df[,sum(get("Precision")), by=grp]
    }

    top <- top[order(V1, decreasing=TRUE)]
    top <- head(top, k)

    koko <- df[top]
}


##
# does the actual plotting work
# if k>0 we select the top k configurations in terms of type selected
irPlot <- function(dataset, type, k=0) {

    # determine axis by type
    yaxis <- ifelse(type=="Precision_Recall", "Precision", type)
    xaxis <- ifelse(type=="Precision_Recall", "Recall", "TopN")

    df <- dataset$data
    name <- dataset$name

    # only select top k if k >= 1
    if(k >= 1) {
      df <- get_top(df, type, k)
    }

    plot <- ggplot(df, aes_string(x=xaxis,y=yaxis, group="grp", shape="grp", color="grp"))
    plot <- plot + stat_smooth(method = "loess",formula = y ~ x, size = 1, se=TRUE, level=0.95)
    plot <- plot + scale_shape_manual(values=LETTERS[1:10])
    plot <- plot + geom_point(size=4)
    plot <- plot + geom_line(size=0.2)
    plot <- plot + labs(title=name, shape="Configurations")
    plot <- plot + scale_y_continuous(breaks = round(seq(0, 0.9, by = 0.1),1))

    file <- paste("~/Projects/goldfish/graphs/", paste(name, type, sep="-"), ".svg", sep="")
    ggsave(file=file, plot=plot, width=10, height=8)
}


##
# strips datestamp and file ext from filename
# returns name of dataset
get_name <- function(filename) {
  # strip date prefix and file extension
  name <- substring(filename, 19, nchar(filename)-4)
  return(name)
}


##
# returns list(name=, data=) with name and actual dataset
get_dataset <- function(filename) {
  data <- fread(paste("~/Projects/goldfish/results/", filename, sep=""))
  # add grp for indexing
  data$grp <- paste(data$Recommender,data$KTL, data$Similarity)
  # set data index
  data <- data.table(data, key='grp')
  # filter out NaN
  data <- data[complete.cases(data), ]
  # set name
  name <- get_name(filename)
  dataset <- list(name=name, data=data)
}


##
# does some data structure conversion in order to
# plot data sets against each other on the same evaluation
comparePlot <- function(evaluation, type, datasets) {

    i <- 1
    for(x in datasets) {
      datasets[[i]]$data <- x$data[x$data$grp == evaluation, ]
      datasets[[i]]$data$grp <- x$name

      i <- i+1
    }

    # extract data tables from dataset
    data <- lapply(datasets, function(x) x$data)
    # merge data tables
    data <- rbindlist(data)

    dataset <- list(name=evaluation, data=data)
    irPlot(dataset, type=type)
}


##
# loops over vector of filenames
# plots Precision, Recall and Precision/Recall curves
plotAll <- function(files) {
    for(filename in files) {
        dataset <- get_dataset(filename)
        irPlot(dataset, type="Precision", k=10)
        irPlot(dataset, type="Recall", k=10)
        irPlot(dataset, type="Precision_Recall", k=10)
    }
}


# list of data set files to plot
files = c()
files[1] <- "2013-11-17-163634-VTT36k-memory-based-unclustered.csv"
files[2] <- "2013-11-27-222024-VTT36k-clusters-2.csv"
files[3] <- "2013-11-28-012024-VTT36k-clusters-3.csv"
files[4] <- "2013-11-28-061852-VTT36k-clusters-5.csv"
files[5] <- "2013-11-28-142459-VTT36k-clusters-7.csv"
files[6] <- "2013-11-29-070254-VTT36k-clusters-9.csv"
files[7] <- "2013-11-30-013920-VTT36k-clusters-11.csv"

plotAll(files)

#comparePlot(c("KNN 2 LogLikelihood"), "Precision", list(get_dataset(files[2]), get_dataset(files[5])))



