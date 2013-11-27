#!/usr/bin/env Rscript

library(data.table)
library(ggplot2)


irPlot <- function(df, type="Precision", n=10, selection="") {

    # filter out NaN
    df <- df[complete.cases(df), ]

    # add grp for indexing
    df$grp <- paste(df$Recommender,df$KTL, df$Similarity)
    # set index
    df <- data.table(df, key='grp')

    # filter out topN values
    top <- df[,sum(get(type)), by=grp]
    top <- top[order(V1, decreasing=T)]
    top <- head(top, n)
    top <- df[top$grp]

    if(nchar(selection) > 0) {
        data <- df[c(selection)]
    }
    else {
        data <- df[top]
    }

    plot <- ggplot(data, aes_string(x="TopN",y=type, group="grp", shape="grp", color="grp"))
    plot <- plot + geom_point()
    plot <- plot + geom_line(size=0.2)
    plot <- plot + stat_smooth(method = "loess",formula = y ~ x, size = 1, se=T, level=0.95)
    plot <- plot + labs(shape="Configurations")

    return(plot)
}

args <- commandArgs(trailingOnly = TRUE)

# read file
# 2013-11-17-163634-VTT36k-memory-based-unclustered.csv
df <- fread("~/projects/goldfish/results/2013-11-17-163634-VTT36k-memory-based-unclustered.csv")
#df <- fread("~/projects/goldfish/results/2013-11-20-010054-VTT36k-memory-based-clustered.csv")
#df <- fread("~/projects/goldfish/results/2013-11-27-154121-VTT36k.csv")

quartz()

irPlot(df, type="Precision")
