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

//Class for evaluating the state distribution
//in one or multiple sequential segments


public class EvaluateSystem {
    
    double[][] arrivalRates; //time x assets
    int[][] capacity; //time x assets
    int nAssets; //assets
    
    AggregatedResults res;
    
    
    //rental time distributions
    PhaseTypeDistribution[][][] phDists; //time x assets x dists
    
    CustomerRelocationMap relMap;
    
    
//    public EvaluateSystem(int nAssets,CustomerRelocationMap relMap){
//        
//        this.nAssets = nAssets;
//        this.relMap = relMap;
//        
//        res = new AggregatedResults();
//        
//    }
    
    public EvaluateSystem(int nAssets, CustomerRelocationMap relMap,
            double[][] arrivalRates, int[][] capacity,
            PhaseTypeDistribution[][][] phDists){
        
        this.nAssets = nAssets;
        this.relMap = relMap;
        this.arrivalRates = arrivalRates;
        this.capacity = capacity;
        this.phDists = phDists;
        
        res = new AggregatedResults(capacity.length);
        
    }
    
    public AggregatedResults getResults(){
        //returns the aggregated results.
        //this only applies if the state
        //dstribution was evaluated using
        //the evaluateSequence method.
        
        return(res);
    }
    
    
    public void changeArrivalRates(double[][] arrivalRates){
        
        this.arrivalRates = arrivalRates;
        
    }
    
    public void changeCapacity(int[][] capacity){
        
        this.capacity = capacity;
        
    }
    
    public void changePhaseTypeDists(PhaseTypeDistribution[][][] phDists){
        
        this.phDists = phDists;
        
    }
    
    public StateDistribution evaluateSingleSegment(int[] currentOccupation, int timeSegment){
        ///evaluate a single segment using the
        //current occupation of the system
        
        //create the asset objects
        Asset[] assets = new Asset[nAssets];
        
        for (int assetIdx=0; assetIdx<nAssets; assetIdx++){
            assets[assetIdx] = new Asset(capacity[timeSegment][assetIdx],
                    phDists[timeSegment][assetIdx],arrivalRates[timeSegment][assetIdx]);
        }
        
        //create the state space
        StateSpace S = new StateSpace(assets,relMap);
        
        //initialize the state distribution
        StateDistribution stateDist = new StateDistribution();
        //install the state space
        stateDist.setStateSpace(S);
        //set the current occupation
        stateDist.setOccupiedCapacity(currentOccupation);
        
        //create the associated transition rate matrix
        TransitionRateMatrix tranMat = new TransitionRateMatrix(S);
        tranMat.generateMatrix();
        
        //evaluate the system at the end of the segment
        StateDistSolver solver = new StateDistSolver(S,tranMat);
        double tol = 1e-6; //the uniformization tolerance
        double t = 1.0;
        solver.uniformization(stateDist,t,tol);
        
        return(stateDist);
    }
    
    
    public StateDistribution evaluateSingleSegment(StateDistribution stateDist, int timeSegment){
        ///evaluate a single segment using the
        //currently evaluated and stored state
        //distribution.
        
        //assumes the state distribution is already initialized.
        
        //assumes the parameters have changed since the latest
        //evaluation.
        
        //create the asset objects
        Asset[] assets = new Asset[nAssets];
        
        for (int assetIdx=0; assetIdx<nAssets; assetIdx++){
            assets[assetIdx] = new Asset(capacity[timeSegment][assetIdx],
                    phDists[timeSegment][assetIdx],arrivalRates[timeSegment][assetIdx]);
        }
        
        //create the state space
        StateSpace S = new StateSpace(assets,relMap);
        
        if (stateDist!=null){
            
//            double[][] margDist = stateDist.getMarginalStateDists();
        
//            System.out.println("BEFORE");
//            for (int assetIdx=0; assetIdx<margDist.length; assetIdx++){
//                System.out.println("Asset Idx:");
//                for (int i=0; i<margDist[assetIdx].length; i++){
//                    System.out.println(margDist[assetIdx][i]);
//                }
//                System.out.println();
//            }
            
            stateDist.newStateSpace(S);
            
            
//            margDist = stateDist.getMarginalStateDists();
        
//            System.out.println("AFTER");
//            for (int assetIdx=0; assetIdx<margDist.length; assetIdx++){
//                System.out.println("Asset Idx:");
//                for (int i=0; i<margDist[assetIdx].length; i++){
//                    System.out.println(margDist[assetIdx][i]);
//                }
//                System.out.println();
//            }
            
        }else{
            System.out.println("Warning: The state distribution "
                    + "was not initialized.");
        }
        
        //create the associated transition rate matrix
        TransitionRateMatrix tranMat = new TransitionRateMatrix(S);
        tranMat.generateMatrix();
        
        //evaluate the system at the end of the segment
        StateDistSolver solver = new StateDistSolver(S,tranMat);
        double tol = 1e-6; //the uniformization tolerance
        double t = 1.0;
        solver.uniformization(stateDist,t,tol);
        
        return(stateDist);
    }
    
    
    public void evaluateSequence(int[] currentOccupation){
        //evaluates a sequence of segments
        //starting with a known occupancy
        //of assets
        
        double elapsed;
        long startTime;
        
        System.out.println("-------- SEGMENT " + 0 + " --------");
        
        startTime = System.currentTimeMillis();
        
        StateDistribution stateDist = evaluateSingleSegment(currentOccupation,0);
        
        elapsed = (double) (System.currentTimeMillis()-startTime)/1000.0;
        
        double[][] margDist = stateDist.getMarginalStateDists();
        double[] cap = new double[nAssets];
        for (int i=0; i<nAssets; i++){
            cap[i]=(double)capacity[0][i];
        }
        res.addResults(margDist,cap,elapsed);
        
//        for (int assetIdx=0; assetIdx<margDist.length; assetIdx++){
//            System.out.println("Asset Idx:");
//            for (int i=0; i<margDist[assetIdx].length; i++){
//                System.out.println(margDist[assetIdx][i]);
//            }
//            System.out.println();
//        }
        
        
        if (capacity.length>1){
            for (int timeSegment=1; timeSegment<capacity.length; timeSegment++){
                
                System.out.println("-------- SEGMENT " + timeSegment + " --------");
                
                startTime = System.currentTimeMillis();
                
                stateDist = evaluateSingleSegment(stateDist,timeSegment);
                
                elapsed = (double) (System.currentTimeMillis()-startTime)/1000.0;
                
                margDist = stateDist.getMarginalStateDists();
                for (int i=0; i<nAssets; i++){
                    cap[i]=(double)capacity[timeSegment][i];
                }
                res.addResults(margDist,cap,elapsed);
                
//                for (int assetIdx=0; assetIdx<margDist.length; assetIdx++){
//                    System.out.println("Asset Idx:");
//                    for (int i=0; i<margDist[assetIdx].length; i++){
//                        System.out.println(margDist[assetIdx][i]);
//                    }
//                    System.out.println();
//                }
                
            }
        }
        
    }
    
    public void writeResultsToFile(String fileName){    
        res.writeResultsToFile(fileName);
    }

    public void writeMarginalDistsToFile(String fileName){    
        res.writeMarginalDistsToFile(fileName);
    }
    
}




