/*
 * Copyright 2022 Anders Reenberg Andersen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tranreloc.tranreloc;

/**
 *
 * @author Anders Reenberg Andersen
 */
public class OptimizeSystem {
    
    EvaluateSystem eval;
    AggregatedResults res;
    
    StateSpace S;
    Asset[] assets;
    StateDistribution newStateDist;
    StateDistribution oldStateDist;
    int[] currentOccupation;
    
    double serviceLevel; //minimum service limit
    int[][] capacity;
    
    public OptimizeSystem(EvaluateSystem eval){
        
        this.eval = eval;
        
        newStateDist = null;
        oldStateDist = null;
        
        res = new AggregatedResults();
        
    }
    
    public void optimize(double serviceLevel, int[] currentOccupation){
        
        this.serviceLevel = serviceLevel;
        this.currentOccupation = currentOccupation;
        
        if (eval.nAssets==1){
            optimizeSingle();
        }else{
            optimizeMultiple();
        }
        
        
    }
    
    
    public void optimizeSingle(){
        //optimization for a single asset
        //in the system
        
        //some initialization
        capacity = new int[eval.arrivalRates.length][1];
        double[][] margDist;
        double[] cap = new double[1];
        
        double elapsed;
        long startTime;
        
        //optimize each segment
        for (int timeSegment=0; timeSegment<capacity.length; timeSegment++){
            
            System.out.println("#############################################");
            System.out.println("         SEGMENT " + timeSegment + "        ");
            System.out.println("#############################################");
            startTime = System.currentTimeMillis();
            
            do{
                
                capacity[timeSegment][0]++;
                eval.changeCapacity(capacity); //employ capacity in the system
                
                if (timeSegment==0){
                    newStateDist = eval.evaluateSingleSegment(currentOccupation,0);
                }else{
                    
                    //allocate memory for a new state distribution, assets and state space matching the new capacity
                    newStateDist = new StateDistribution();
                    assets = new Asset[eval.nAssets];
                    for (int assetIdx=0; assetIdx<eval.nAssets; assetIdx++){
                        assets[assetIdx] = new Asset(capacity[(timeSegment-1)][assetIdx],
                            eval.phDists[(timeSegment-1)][assetIdx],eval.arrivalRates[(timeSegment-1)][assetIdx]);
                    }
                    S = new StateSpace(assets,eval.relMap);
                    
                    newStateDist.setStateSpace(S); //employ state space in state distribution
                    newStateDist.setStateDistribution(oldStateDist.stateDist); //employ the old state dist.
                    
                    //change from the old to the new state distribution
                    newStateDist = eval.evaluateSingleSegment(newStateDist,timeSegment);
                }
                
                //calculate the marginal distribution
                margDist = newStateDist.getMarginalStateDists();
                
            }while(margDist[0][(margDist[0].length-1)]>(1.0-serviceLevel)); //assess the shortage probability
                    
            oldStateDist = new StateDistribution();
            assets = new Asset[eval.nAssets];
            for (int assetIdx=0; assetIdx<eval.nAssets; assetIdx++){
                assets[assetIdx] = new Asset(capacity[timeSegment][assetIdx],
                    eval.phDists[timeSegment][assetIdx],eval.arrivalRates[timeSegment][assetIdx]);
            }
            S = new StateSpace(assets,eval.relMap);
            oldStateDist.setStateSpace(S);
            oldStateDist.setStateDistribution(newStateDist.stateDist);
            
            cap[0] = capacity[timeSegment][0];
            elapsed = (double) (System.currentTimeMillis()-startTime)/1000.0;
            res.addResults(margDist,cap,elapsed);
            
        }
        
        
        
    }
    
    public void optimizeMultiple(){
        //optimization for multiple assets
        //in the system
        
        capacity = new int[eval.arrivalRates.length][eval.nAssets];
        double[] cap = new double[eval.nAssets];
        
        //current lower and upper capacity bounds
        //of each asset
        int[][] capBounds;
        
        double elapsed;
        long startTime;
        
        //optimize each segment
        for (int timeSegment=0; timeSegment<capacity.length; timeSegment++){
            
            System.out.println("#############################################");
            System.out.println("         SEGMENT " + timeSegment + "        ");
            System.out.println("#############################################");
            startTime = System.currentTimeMillis();
            
            //derive bounds for each asset
            System.out.println("Deriving bounds...");
            capBounds = deriveBounds(timeSegment);
            System.out.println("Done.");
            
            //derive the optimized capacity and store in
            //the capacity array
            System.out.println("Optimizing using bounds:");
            for (int assetIdx=0; assetIdx<eval.nAssets; assetIdx++){
                System.out.print("(" + capBounds[assetIdx][1] + "," + capBounds[assetIdx][0] + ") ");
            }
            System.out.println();
            optimizeWithBounds(timeSegment,capBounds);
            System.out.println("Done.");
            
            //store state distribution of optimal capacity allocation
            //assuming the capacity array stores the solution
            oldStateDist = new StateDistribution();
            assets = new Asset[eval.nAssets];
            for (int assetIdx=0; assetIdx<eval.nAssets; assetIdx++){
                assets[assetIdx] = new Asset(capacity[timeSegment][assetIdx],
                    eval.phDists[timeSegment][assetIdx],eval.arrivalRates[timeSegment][assetIdx]);
            }
            S = new StateSpace(assets,eval.relMap);
            oldStateDist.setStateSpace(S);
            oldStateDist.setStateDistribution(newStateDist.stateDist);
            
            
            //store results
            for (int assetIdx=0; assetIdx<eval.nAssets; assetIdx++){
                cap[assetIdx] = (double) capacity[timeSegment][assetIdx];
            }
            elapsed = (double) (System.currentTimeMillis()-startTime)/1000.0;
            res.addResults(oldStateDist.getMarginalStateDists(),cap,elapsed);
            
        }
        
        
    }
    
    public int[][] deriveBounds(int timeSegment){
        //derive the current lower and upper bounds
        //of each asset
        
        int[][] capBounds = new int[eval.nAssets][2]; //assets x {upper,lower}
        double[][] margDist;
        
        //upper bounds
        System.out.println("-------- UPPER BOUNDS --------");
        for (int assetIdx=0; assetIdx<eval.nAssets; assetIdx++){
            
            //initialize
            capacity[timeSegment] = new int[eval.nAssets];
            for (int i=0; i<eval.nAssets; i++){
                if (i!=assetIdx){
                    capacity[timeSegment][i]=1;
                }
            }
            
            do{
                
                capacity[timeSegment][assetIdx]++;
                
                System.out.println("Employ capacity");
                for (int ii=0; ii<eval.nAssets; ii++){
                    System.out.print(capacity[timeSegment][ii] + " ");
                }
                System.out.println();
                
                eval.changeCapacity(capacity); //employ capacity in the system
                
                //evaluate 
                if (timeSegment==0){
                    newStateDist = eval.evaluateSingleSegment(currentOccupation,0);
                }else{
                    
                    //allocate memory for a new state distribution, assets and state space matching the new capacity
                    newStateDist = new StateDistribution();
                    assets = new Asset[eval.nAssets];
                    for (int i=0; i<eval.nAssets; i++){
                        assets[i] = new Asset(capacity[(timeSegment-1)][i],
                            eval.phDists[(timeSegment-1)][i],eval.arrivalRates[(timeSegment-1)][i]);
                    }
                    S = new StateSpace(assets,eval.relMap);
                    
                    newStateDist.setStateSpace(S); //employ state space in state distribution
                    newStateDist.setStateDistribution(oldStateDist.stateDist); //employ the old state dist.
                    
                    //change from the old to the new state distribution
                    newStateDist = eval.evaluateSingleSegment(newStateDist,timeSegment);
                }
                
                //calculate the marginal distribution
                margDist = newStateDist.getMarginalStateDists();
                
                System.out.println("Shortage prob: " + margDist[assetIdx][(margDist[assetIdx].length-1)]);
                
            }while(margDist[assetIdx][(margDist[assetIdx].length-1)]>(1.0-serviceLevel)); //assess the shortage probability
            
            capBounds[assetIdx][0] = capacity[timeSegment][assetIdx]; //insert upper bound
            
        }
        
        
        
        //lower bounds
        System.out.println("-------- LOWER BOUNDS --------");
        for (int assetIdx=0; assetIdx<eval.nAssets; assetIdx++){
            
            //initialize
            capacity[timeSegment] = new int[eval.nAssets];
            for (int i=0; i<eval.nAssets; i++){
                if (i!=assetIdx){
                    capacity[timeSegment][i]=capBounds[i][0];
                }
            }
            
            do{
                
                capacity[timeSegment][assetIdx]++;
                
                System.out.println("Employ capacity");
                for (int ii=0; ii<eval.nAssets; ii++){
                    System.out.print(capacity[timeSegment][ii] + " ");
                }
                System.out.println();
                
                eval.changeCapacity(capacity); //employ capacity in the system
                
                //evaluate 
                if (timeSegment==0){
                    newStateDist = eval.evaluateSingleSegment(currentOccupation,0);
                }else{
                    
                    //allocate memory for a new state distribution, assets and state space matching the new capacity
                    newStateDist = new StateDistribution();
                    assets = new Asset[eval.nAssets];
                    for (int i=0; i<eval.nAssets; i++){
                        assets[i] = new Asset(capacity[(timeSegment-1)][i],
                            eval.phDists[(timeSegment-1)][i],eval.arrivalRates[(timeSegment-1)][i]);
                    }
                    S = new StateSpace(assets,eval.relMap);
                    
                    newStateDist.setStateSpace(S); //employ state space in state distribution
                    newStateDist.setStateDistribution(oldStateDist.stateDist); //employ the old state dist.
                    
                    //change from the old to the new state distribution
                    newStateDist = eval.evaluateSingleSegment(newStateDist,timeSegment);
                }
                
                //calculate the marginal distribution
                margDist = newStateDist.getMarginalStateDists();
                
                System.out.println("Shortage prob: " + margDist[assetIdx][(margDist[assetIdx].length-1)]);
                
            }while(margDist[assetIdx][(margDist[assetIdx].length-1)]>(1.0-serviceLevel));

            capBounds[assetIdx][1] = capacity[timeSegment][assetIdx]; //insert lower bound
            //if the fast approx. state dist. change caused the lower bound
            //to be larger than the upper bound
            if (capBounds[assetIdx][1]>capBounds[assetIdx][0]){
                capBounds[assetIdx][0]=capBounds[assetIdx][1];
            }
                
            
            
            
        }
        
        
        return(capBounds);
    }
    
    
    
    public void optimizeWithBounds(int timeSegment, int[][] capBounds){
        //enumerate the solution space using the
        //previously derived capacity bounds
        
        //the capacity array stores the optimized solution
        //minimizing the total capacity in the time segment
        
        
        double[][] margDist;
        
        int best_objVal; //objective
        int current_objVal;
        int[] best_solution = new int[eval.nAssets];
        
        int nsol = 1; //total number of configurations
        best_objVal=0;
        current_objVal=0;
        for (int i=0; i<eval.nAssets; i++){
            nsol *= (capBounds[i][0]-capBounds[i][1])+1;
            best_solution[i] = capBounds[i][0];
            best_objVal += best_solution[i];
            capacity[timeSegment][i] = capBounds[i][1];
            current_objVal += capacity[timeSegment][i]; 
        }
        
        
        int assetIdx;
        boolean increase,violate;
        for (int i=0; i<nsol; i++){
            
            //evaluate solution
            
            eval.changeCapacity(capacity); //employ capacity in the system
            
            //evaluate 
            if (timeSegment==0){
                newStateDist = eval.evaluateSingleSegment(currentOccupation,0);
            }else{
                    
                //allocate memory for a new state distribution, assets and state space matching the new capacity
                newStateDist = new StateDistribution();
                assets = new Asset[eval.nAssets];
                for (int j=0; j<eval.nAssets; j++){
                    assets[j] = new Asset(capacity[(timeSegment-1)][j],
                        eval.phDists[(timeSegment-1)][j],eval.arrivalRates[(timeSegment-1)][j]);
                }
                S = new StateSpace(assets,eval.relMap);
                    
                newStateDist.setStateSpace(S); //employ state space in state distribution
                newStateDist.setStateDistribution(oldStateDist.stateDist); //employ the old state dist.
                    
                //change from the old to the new state distribution
                newStateDist = eval.evaluateSingleSegment(newStateDist,timeSegment);
            }
                
            //calculate the marginal distribution
            margDist = newStateDist.getMarginalStateDists();
            
            violate=false;
            for (int j=0; j<eval.nAssets; j++){
               if(margDist[j][(margDist[j].length-1)]>(1.0-serviceLevel)){
                   violate=true;
               }
            }
            if (!violate){
                current_objVal=0;
                for (int j=0; j<eval.nAssets; j++){
                    current_objVal+=capacity[timeSegment][j];
                }
                if (current_objVal<best_objVal){
                    System.out.println("Solution improved: " + current_objVal);
                    best_objVal=current_objVal;
                    for (int j=0; j<eval.nAssets; j++){
                        best_solution[j]=capacity[timeSegment][j];
                    }
                }
            }
            
            //move to next solution (if possible)
            assetIdx = eval.nAssets-1;
            increase=false;
            while (assetIdx>=0 && !increase){
                if (capacity[timeSegment][assetIdx]>=capBounds[assetIdx][0]){
                    capacity[timeSegment][assetIdx]=capBounds[assetIdx][1];
                    assetIdx--;
                }else{
                    capacity[timeSegment][assetIdx]++;
                    increase=true;
                }
                
            }
            
        }
        
        //store the optimal solution in the
        //capacity array
        for (int i=0; i<eval.nAssets; i++){
            capacity[timeSegment][i]=best_solution[i];
        }
        
        //final validation
        eval.changeCapacity(capacity); //employ capacity in the system
            
            //evaluate 
        if (timeSegment==0){
            newStateDist = eval.evaluateSingleSegment(currentOccupation,0);
        }else{
                    
            //allocate memory for a new state distribution, assets and state space matching the new capacity
            newStateDist = new StateDistribution();
            assets = new Asset[eval.nAssets];
            for (int i=0; i<eval.nAssets; i++){
                assets[i] = new Asset(capacity[(timeSegment-1)][i],
                    eval.phDists[(timeSegment-1)][i],eval.arrivalRates[(timeSegment-1)][i]);
            }
            S = new StateSpace(assets,eval.relMap);
                    
            newStateDist.setStateSpace(S); //employ state space in state distribution
            newStateDist.setStateDistribution(oldStateDist.stateDist); //employ the old state dist.
                    
            //change from the old to the new state distribution
            newStateDist = eval.evaluateSingleSegment(newStateDist,timeSegment);
        }
        
    }
    
    
    
    
    public void writeResultsToFile(String fileName){    
        res.writeResultsToFile(fileName);
    }
    
    private double[] storeDistribution(double[] dist0){
        
        double[] dist1 = new double[dist0.length];
        for (int i=0; i<dist0.length; i++){
            dist1[i]=dist0[i];
        }
        
        return(dist1);
    }
    
    
    
}
