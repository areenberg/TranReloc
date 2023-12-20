# TranReloc
TranReloc evaluates queueing systems with customer relocation. Consider the M/PH/c/c queue, i.e. where customers have exponentially distributed inter-arrival time, phase-type distributed service time, and c servers. The capacity of the queue is equal to the number of servers, and customers will be rejected if they arrive when none of the c servers are idle at the time of arrival to the queue. TranReloc extends the M/PH/c/c queue by considering a series of systems that may relocate customers instead completely rejecting them. Moreover, TranReloc gives the user the opportunity to evaluate the system with time-dependent parameters. 

TranReloc is written in Java, and the interface currently consists of a simple Command Line Interface (CLI). The easiest way to send commands to TranReloc (using the CLI) is through an R-script. The section *"Implement your model using R"* shows how to use R for implementing and evaluating a model.
  
## Features

- Evaluate a time-dependent queueing system with exponentially distributed inter-arrival time, phase-type distributed service-time, and customer relocation.
- Choose the capacity and customer's arrival rate and phase-type distribution at each point in time.
- Choose how customers are relocated as function of the blocked queues in the system.
- Choose the service-time distribution of the relocated customers.
- Choose to either *evaluate* or *optimize* (i.e. minimize) the total capacity in the system.

# Table of contents

1. How does it work
2. Implementing your model (using R)
3. How to cite  
4. License

# How does it work

TranReloc use a Continuous-Time Markov Chain (CTMC) and a numerical approach to model the system.
Consider a discretized timeline consisting of n consecutive segments. The algorithm in TranReloc use uniformization to evaluate the transient solution to the CTMC by assuming time-homogenuous (i.e. fixed) parameters within each segment. As the algorithm change segment, so does the parameters. Thus, the accuracy of the solution to the state probabilities can be controlled by adjusting the size of the segments.

<img src="https://github.com/areenberg/TranReloc/blob/main/Images/TimeLine.jpg?raw=true" width="1400" height="232">


## Structure of parameters

The system contains a range of queues, denoted *assets*. Additionally, each asset is subject to a capacity, arrival intensity and range of service-time distributions. All service-times are governed by phase-type (PH) distributions. The number of assets and service distributions are fixed, but the capacity, arrival intensity and configurations of the distributions (i.e. the number of phases and parameter values) are allowed to change over time. That is, between the consecutive segments.  


<img src="https://github.com/areenberg/TranReloc/blob/main/Images/Parameters1.jpg?raw=true" width="1400" height="350">

## Relocation of customers

Customers arriving to an asset without idle servers can be relocated to an alternative asset instead of being lost from the system. The customers relocate to the alternative asset with a pre-specified probability, denoted *the relocation probability*, which is a function of the assets that are in shortage of servers at the time of arrival to the system.

As they relocate to the alternative asset, the relocated customers must be assigned a service-time distribution. The following governs the assignment process: (1) The customer's preferred asset (the one currently in shortage), and (2) a probability distribution. For instance, say customers are relocated from *Asset 0* to *Asset 1*, and *Asset 1* contains three types of service-time distributions. The service-time of the relocated customers are governed by the first distribution with a probability of 25%, the second distribution with a probability of 10%, and the third distribution with a probability of 65%.         

The first service-time distribution (index 0) *always* governs the service-time of the customers that are not relocated. However, note that this service-time distribution can also be used by the relocated customers. 

<img src="https://github.com/areenberg/TranReloc/blob/main/Images/Relocation.jpg?raw=true" width="1400" height="450">

 
# Implement your model using R

Start by cloning the repository and navigating to the folder called `TranReloc incl R`. Open the template `R_Interface.R`.

### 1. Task and output type

- **`task`**: Define the task as either "optimize" or "evaluate".
  - Example: 
    ```R
    task <- "optimize"
    ```

- **`output.type`**: Choose between "measures" or "distributions" for the output type.
  - Example: 
    ```R
    output.type <- "distributions"
    ```

### 2. Assets configuration

- **`nAssets`**: Specify the number of assets.
  - Example: 
    ```R
    nAssets <- 2
    ```

- **`asset.nms`**: Provide names for each asset, corresponding to `nAssets`.
  - Example: 
    ```R
    asset.nms <- c("asset0", "asset1")
    ```

### 3. Time periods

- **`timePeriods`**: Define the number of time periods you want to evaluate.
  - Example: 
    ```R
    timePeriods <- 10
    ```

### 4. Currently occupied capacity

- **`currentOccupied`**: Set the currently occupied capacity for each asset.
  - Example: 
    ```R
    currentOccupied <- c(0, 0)
    ```

### 5. Distributions per asset

- **`assetDists`**: Specify the number of distributions for each asset.
  - Example: 
    ```R
    assetDists <- c(2, 2)
    ```

### 6. Capacity and arrival rates

- **`capacity`**: Define the capacity for each asset over time.
- **`arrivalRate`**: Set the arrival rates for each asset.

### 7. Relocation rules

Relocation rules define how customers are moved between assets under certain conditions.

- **Between assets relocation**
  - `rule1 <- "betweenAssets,1.0,0,1,{0}"` 
  - This rule specifies that when asset 0 (`{0}`) is blocked (i.e., reaches its capacity), 100% (`1.0`) of the customers are relocated from asset 0 to asset 1.

- **Distribution within an asset in case of relocation**
  - `rule4 <- "toDistsInAsset,1,0,{0,1},{0.05,0.95}"`
  - This rule is more complex. It specifies that customers relocated from asset 1 to asset 0 can be distributed across two different distributions within asset 0 (distributions 0 and 1). The customers are further distributed with a ratio of 5% (`0.05`) to distribution 0 and 95% (`0.95`) to distribution 1.

### Constructing your own relocation rules

- When constructing your own relocation rules, consider the following format:
  - `"betweenAssets, [relocation_ratio], [source_asset], [destination_asset], {[blocked_assets]}"` for asset-to-asset relocation.
  - `"toDistsInAsset, [source_asset], [destination_asset], {[dist_indices]}, {[dist_ratios]}"` for distribution-specific relocation within assets.

### 8. Asset distributions

- Specify initial distributions and phase-type generators for each distribution of each asset.
  - Example for Asset 0, Distribution 0:
    ```R
    asset0.dist0.initDist <- c(0.75, 0.25)
    asset0.dist0.phGen <- matrix(...)
    ```

### 9. Modifying service time distributions

The *service time distributions* section of the script is responsible for defining and applying service time distributions for each asset across different time periods. Here is how you can modify this section to suit your requirements.

The `serviceTimeDist` function sets up the service time distribution for a specific asset and distribution. It takes the following parameters:
- `initDist`: The initial distribution of service time.
- `phGen`: The phase-type generator matrix for service time transitions.
- `timeIdx`: The index for the time period.
- `distIdx`: The index for the specific distribution.
- `assetIdx`: The index for the asset.

Example: 
```R
#service time distributions
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
```


## Prerequisites

* The package `rlist` is required. Install with `install.packages("rlist")`.
* The template assumes that the script runs in RStudio. If this is **not** the case, simply replace `setwd(dirname(getActiveDocumentContext()$path))` with `setwd("your_workspace_directory")` and remove the import `library(rstudioapi)`. If this **is** the case, install package `rstudioapi` with `install.packages("rstudioapi")`.

## Debugging

* If the JAR-file for TranReloc cannot be found, try modifying the line `system(paste("java -jar \"",getwd(),"/TranReloc.jar\""," -t ",task," -o ",output.type,sep=""))` in the bottom of the template.

# How to cite

Andersen, A. R., TranReloc: Relocation of customers in a transient queueing system with phase-type distributed service times, GitHub, URL: https://github.com/areenberg/TranReloc

# License

Copyright 2023 Anders Reenberg Andersen.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
