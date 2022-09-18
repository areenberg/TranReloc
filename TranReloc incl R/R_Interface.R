rm(list = ls())

#Example script for using
#R as an interface for the TranReloc program

#Assumes the script runs in Rstudio

#----------------------------
# PREAMPLE
#----------------------------

library(rlist)
library(rstudioapi)

#set working directory
setwd(dirname(getActiveDocumentContext()$path))


#----------------------------
# FUNCTIONS
#----------------------------

cleanServiceTime <- function(){
  unlink(paste(getwd(),"/Parameters/RentalTime",sep=""),recursive=TRUE)
  dir.create(paste(getwd(),"/Parameters/RentalTime",sep=""))
}

serviceTimeDist <- function(initDist,phGen,timeIdx,distIdx,assetIdx){
  nPhases <- length(initDist)
  
  #create directory
  dir.create(paste(getwd(),"/Parameters/RentalTime/time",timeIdx,sep=""),showWarnings=FALSE)
  dir.create(paste(getwd(),"/Parameters/RentalTime/time",timeIdx,"/asset",assetIdx,sep=""),showWarnings=FALSE)
  dir.create(paste(getwd(),"/Parameters/RentalTime/time",timeIdx,"/asset",assetIdx,
             "/distribution",distIdx,sep=""),showWarnings=FALSE)
  
  dir <- paste(paste(getwd(),"/Parameters/RentalTime/time",timeIdx,"/asset",assetIdx,
                     "/distribution",distIdx,"/phases",sep=""))
  str <- paste(initDist[1],paste(phGen[1,],collapse=" "),collapse = "   ")
  cat(str,file=dir,sep="\n")
  
  if (nPhases>1){
    for (i in 2:nPhases){
      str <- paste(initDist[i],paste(phGen[i,],collapse=" "),collapse = "   ")
      cat(str,file=dir,append=TRUE,sep="\n")
    }  
  }
  
}

relocationRules <- function(rules){
  cat(rules[1],file=paste(getwd(),"/Parameters/RelocationRules",sep=""),sep="\n")
  if (length(rules)>1){
    for (i in 2:length(rules)){
      cat(rules[i],file=paste(getwd(),"/Parameters/RelocationRules",sep=""),append=TRUE,sep="\n")  
    }  
  }
}

numberOfAssets <- function(nAssets){
  
  cat(nAssets,file=paste(getwd(),"/Parameters/NumberOfAssets",sep=""),sep="")
  
}



#----------------------------
# INPUT PARAMETERS
#----------------------------

#task ("optimize" or "evaluate")
task <- "optimize"
 
#number of assets
nAssets <- 2

#asset names
asset.nms <- c("asset0","asset1")

#time periods
timePeriods <- 10

#currently occupied capacity
currentOccupied <- c(0,0)
names(currentOccupied) <- asset.nms
currentOccupied <- t(as.data.frame(currentOccupied))

#number of distributions per asset
assetDists <- c(2,2)
names(assetDists) <- asset.nms
assetDists <- t(as.data.frame(assetDists))

#capacity
capacity <- matrix(c(4,4,
                     5,5,
                     6,6,
                     6,6,
                     7,7,
                     8,8,
                     9,9,
                     9,9,
                     10,10,
                     10,10),ncol=2,nrow=10,byrow=TRUE)
colnames(capacity) <- asset.nms
capacity <- as.data.frame(capacity)

#arrival rate
arrivalRate <- matrix(c(1.0,1.0,
                        1.5,1.5,
                        2.0,2.0,
                        2.5,2.5,
                        3.0,3.0,
                        3.5,3.5,
                        4.0,4.0,
                        4.5,4.5,
                        5.0,5.0,
                        5.5,5.5),ncol=2,nrow=10,byrow=TRUE)
colnames(arrivalRate) <- asset.nms
arrivalRate <- as.data.frame(arrivalRate)

#relocation rules
#1.0 of customers are relocated from asset 0 to asset 1
#when asset 0 is blocked
rule1 <- "betweenAssets,1.0,0,1,{0}" 

#1.0 of customers are relocated from asset 1 to asset 0
#when asset 1 is blocked
rule2 <- "betweenAssets,1.0,1,0,{1}" 

#customer relocated from asset 0 to asset 1
#can use distribution 0 and 1 in asset 1.
#the relocated customers are distributed with 5% and 95%
#on the two distributions.
rule3 <- "toDistsInAsset,0,1,{0,1},{0.05,0.95}"

#customer relocated from asset 1 to asset 0
#can use distribution 0 and 1 in asset 0.
#the relocated customers are distributed with 5% and 95%
#on the two distributions.
rule4 <- "toDistsInAsset,1,0,{0,1},{0.05,0.95}"

relocRules <- c(rule1,rule2,rule3,rule4)

#----- asset 0 distributions -----
#distribution 0:
asset0.dist0.initDist <- c(0.75,0.25)
asset0.dist0.phGen <- matrix(c(-2.0,1.0,
                         3.0,-5.0),nrow=2,ncol=2,byrow=TRUE)
#distribution 1:
asset0.dist1.initDist <- c(0.10,0.90)
asset0.dist1.phGen <- matrix(c(-3.0,2.0,
                         0.5,-8.0),nrow=2,ncol=2,byrow=TRUE)
#----- asset 1 distributions -----
#distribution 0:
asset1.dist0.initDist <- c(0.75,0.25)
asset1.dist0.phGen <- matrix(c(-2.0,1.0,
                         3.0,-5.0),nrow=2,ncol=2,byrow=TRUE)
#distribution 1:
asset1.dist1.initDist <- c(0.10,0.90)
asset1.dist1.phGen <- matrix(c(-3.0,2.0,
                         0.5,-8.0),nrow=2,ncol=2,byrow=TRUE)

#note: in this example we assume the distributions are the 
#same across all time periods, although the program supports
#time-inhomogeneous parameters


#----------------------------
# INSERT (SAVE) PARAMETERS
#----------------------------

numberOfAssets(nAssets)

relocationRules(relocRules)

cleanServiceTime()
for (timeIdx in c(0:(timePeriods-1))){
  #----- asset 0 distributions -----
  #distribution 0:
  serviceTimeDist(asset0.dist0.initDist,asset0.dist0.phGen,timeIdx,0,0)
  
  #distribution 1:
  serviceTimeDist(asset0.dist1.initDist,asset0.dist1.phGen,timeIdx,1,0)
  
  #----- asset 1 distributions -----
  #distribution 0:
  serviceTimeDist(asset1.dist0.initDist,asset1.dist0.phGen,timeIdx,0,1)
  
  #distribution 1:
  serviceTimeDist(asset1.dist1.initDist,asset1.dist1.phGen,timeIdx,1,1)
}

write.table(currentOccupied,file="Parameters/CurrentlyOccupied",sep=",",row.names=FALSE,quote=FALSE)
write.table(assetDists,file="Parameters/NumberOfAssetDists",sep=",",row.names=FALSE,quote=FALSE)
write.table(capacity,file="Parameters/Capacity",sep=",",row.names=FALSE,quote=FALSE)
write.table(arrivalRate,file="Parameters/ArrivalRates",sep=",",row.names=FALSE,quote=FALSE)


#----------------------------
# RUN THE PROGRAM
#----------------------------

system(paste("java -jar TranReloc.jar","-t",task))

#----------------------------
# GET THE RESULTS
#----------------------------

results <- read.table("Results/results.csv",sep=",",header=TRUE)



