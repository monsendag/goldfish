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

    return(df[top])
}

##
# does the actual plotting work
# if k>0 we select the top k configurations in terms of type selected
irPlot <- function(dataset, type, k=0, f=3) {

    # determine axis by type
    yaxis <- ifelse(type=="Precision_Recall", "Precision", type)
    xaxis <- ifelse(type=="Precision_Recall", "Recall", "TopN")

    xtitle <- ifelse(xaxis=="TopN", "N", xaxis)


    df <- dataset$data
    name <- dataset$name


    legendpos <- ifelse(f==1, c(0,1), ifelse(f==2, c(1,1), c(1,0)))

    # only select top k if k >= 1
    if(k >= 1) {
      df <- get_top(df, type, k)
    }

    plot <- ggplot(df, aes_string(x=xaxis,y=yaxis, group="grp",  color="grp"))
    plot <- plot + stat_smooth(method = "loess",formula = y ~ x, size = 0.9, se=F, level=0.95)
    plot <- plot + scale_color_manual(    values=c("black", "black", "black", "black", "black", "grey50", "grey50", "grey50", "grey50", "grey50", "grey70", "grey70", "grey70", "grey70", "grey70", "grey50", "grey50", "grey50", "grey50", "grey50"))
    plot <- plot + scale_linetype_manual(values = c("solid", "dashed", "dotted", "dotdash", "twodash", "solid", "dashed", "dotted", "dotdash", "twodash"))
    #plot <- plot + scale_shape_manual(values=LETTERS[1:10])
    #plot <- plot + geom_point(size=4)
    #plot <- plot + geom_line(size=0.2)
    #plot <- plot + labs(title=name, color="Configurations")
    plot <- plot + scale_y_continuous(breaks = round(seq(0, 0.9, by = 0.1),1))
    #plot <- plot + scale_fill_grey()
    plot <- plot + theme_bw()
    plot <- plot + theme(
      text = element_text(size=13),
      legend.title = element_blank(),
      legend.text = element_text(colour="black", size = 13),
      legend.position = c(1,0),
      #legend.position="none",
      legend.justification = c(1,0),
      legend.background = element_rect(colour = NA, fill = "white")
    )
    plot <- plot + labs(x=xtitle)

  #  quartz()
   # plot

    file <- paste("~/Projects/goldfish/graphs/pdf/", paste(name, type, sep="-"), sep="")
    file <- gsub(" ", "-", file)
    file <- gsub("[.]", "", file)
    file <- paste(file, ".pdf", sep="")
    ggsave(file=file, plot=plot, width=10, height=8)
}


##
# strips datestamp and file ext from filename
# returns name of dataset
get_name <- function(filename) {
  # strip date prefix and file extension
  name <- substring(filename, 26, nchar(filename)-4)
  return(name)
}


##
# returns list(name=, data=) with name and actual dataset
get_dataset <- function(filename) {
  data <- fread(paste("~/Projects/goldfish/results/", filename, sep=""))

  data$grp <- ""

  for (i in 1:nrow(data)) {
    row <- data[i]
    newName <- gsub("K", row$KTL, row$Recommender)
    newName <- gsub("Threshold", paste("Threshold", row$KTL), newName)
    row$grp <- paste(newName, row$Similarity)
    data[i] <- row
  }

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

    dataset <- list(name=paste(evaluation, collapse=', '), data=data)

    irPlot(dataset, type=type)
}


##
# loops over vector of filenames
# plots Precision, Recall and Precision/Recall curves
plotAll <- function(datasets, type, k=10) {
    for(dataset in datasets) {
        irPlot(dataset, type, k)
    }
}
