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
public class TranReloc {

    public static void main(String[] args) {
        welcomeMessage();
        
        
        //-----------------------
        //input
        //-----------------------
        String task = getTask(args); //task to be conducted ("evaluate" or "optimize")
        String resDir = getResultsDirectory(args); //directory for results
        String outType = getOutputType(args); //type of results
        String serLevel = getServiceLevel(args); //get the service level for the optimization procedure
        if (task.equals("none")){
            task = "evaluate"; //default task
        }
        if (resDir.equals("none")){
            resDir = "Results/results.csv"; //default directory for results
        }
        if (outType.equals("none")){
            outType = "measures"; //default output type
        }
        //service level for the optimization procedure
        double serviceLevel;
        if (serLevel.equals("none")){
            serviceLevel = 0.99; //default service level
        }else{
            serviceLevel = Double.parseDouble(serLevel);
        }
        
        //-----------------------
        //read parameters
        //-----------------------
        String paramDir = "Parameters";
        ReadParameters readParam = new ReadParameters(paramDir);
        ReadRelocationMap readRelocMap = new ReadRelocationMap(paramDir);
        
        EvaluateSystem eval = new EvaluateSystem(readParam.nAssets,readRelocMap.getRelocationMap(),
            readParam.arrivalRates,readParam.capacity,readParam.phDists);
        
        //-----------------------
        //run analysis
        //-----------------------
        if (task.equals("evaluate")){
        
            //evaluate the system
            eval.evaluateSequence(readParam.occupied);
        
            //write results to file
            if (outType.equals("measures")){
                eval.writeResultsToFile(resDir);
            }else if(outType.equals("distributions")){
                eval.writeMarginalDistsToFile(resDir);
            }
    
        }else if(task.equals("optimize")){
                
            //optimize the system
            OptimizeSystem opt = new OptimizeSystem(eval);
            opt.optimize(serviceLevel,readParam.occupied);
            
            //write results to file
            if (outType.equals("measures")){
                opt.writeResultsToFile(resDir);
            }else if(outType.equals("distributions")){
                opt.writeMarginalDistsToFile(resDir);
            }
        }else{
            System.out.println("Warning. Unknown task: " + task + "\n"
                    + "Terminating program.");
        }      
        
        
    }
    
    public static String getTask(String[] inputArgs){
        
        int idx=0;
        while (idx<inputArgs.length && !inputArgs[idx].equals("-t")){
            idx++;
        }
        if (idx==inputArgs.length){
            return("none");
        }else{
            return(inputArgs[(idx+1)]);
        }
        
    }
    
    public static String getOutputType(String[] inputArgs){
        
        int idx=0;
        while (idx<inputArgs.length && !inputArgs[idx].equals("-o")){
            idx++;
        }
        if (idx==inputArgs.length){
            return("none");
        }else{
            return(inputArgs[(idx+1)]);
        }
        
    }            

    public static String getServiceLevel(String[] inputArgs){
        
        int idx=0;
        while (idx<inputArgs.length && !inputArgs[idx].equals("-s")){
            idx++;
        }
        if (idx==inputArgs.length){
            return("none");
        }else{
            if (Double.parseDouble(inputArgs[(idx+1)])>0.0&&
                    Double.parseDouble(inputArgs[(idx+1)])<1.0){
                return(inputArgs[(idx+1)]);
            }else{
                return("none");
            }
        }
        
    }

    public static String getResultsDirectory(String[] inputArgs){
        
        int idx=0;
        while (idx<inputArgs.length && !inputArgs[idx].equals("-r")){
            idx++;
        }
        if (idx==inputArgs.length){
            return("none");
        }else{
            return(inputArgs[(idx+1)]);
        }
        
    }

    
    public static void welcomeMessage(){
        
        System.out.println("#############################################");
        System.out.println("    Welcome to TranReloc v1.0                ");
        System.out.println("#############################################");
        
    }
    
    
    public static void testStateDistribution(){
        //test solving and changing the state
        //distribution
        
        //-----------------------
        //  PREAMPLE
        //-----------------------
        
        int nAssets = 2;
        Asset[] assets = new Asset[nAssets];
        int[] capacity={3,2};
        double[] arrivalRate = {3.2,2.1};
        
        //primary distribution in first asset
        double[] initialDistribution00 = {0.75,0.25};
        double[][] phaseTypeGenerator00 = {{-2,1},
                                         {3,-5}};
        
        //alternative distribution in first asset
        double[] initialDistribution01 = {0.10,0.90};
        double[][] phaseTypeGenerator01 = {{-3,2},
                                         {0.5,-8}};
        
        PhaseTypeDistribution[] phDists0 = new PhaseTypeDistribution[2];
        phDists0[0] = new PhaseTypeDistribution(initialDistribution00,phaseTypeGenerator00);
        phDists0[1] = new PhaseTypeDistribution(initialDistribution01,phaseTypeGenerator01);
        
        assets[0] = new Asset(capacity[0],phDists0,arrivalRate[0]);
        
        
        //primary distribution in second asset
        double[] initialDistribution10 = {0.10,0.90};
        double[][] phaseTypeGenerator10 = {{-10,1.5},
                                         {8,-9}};
        //alternative distribution in second asset
        double[] initialDistribution11 = {0.20,0.80};
        double[][] phaseTypeGenerator11 = {{-3,2},
                                         {8,-8.1}};
        
        PhaseTypeDistribution[] phDists1 = new PhaseTypeDistribution[2];
        phDists1[0] = new PhaseTypeDistribution(initialDistribution10,phaseTypeGenerator10);
        phDists1[1] = new PhaseTypeDistribution(initialDistribution11,phaseTypeGenerator11);
        
        assets[1] = new Asset(capacity[1],phDists1,arrivalRate[1]);
        
        CustomerRelocationMap relMap = new CustomerRelocationMap(nAssets);
        int[] dInd0 = {0,1}; double[] pInd0 = {0.05,0.95};
        relMap.addRelocationToAsset(0,1,dInd0,pInd0);
        //100% of customers are relocated to asset 1 when 0 is blocked
        int[] bA0 = {0};
        relMap.addRelocationProbToAsset(1.0,0,1,bA0);
        
        int[] dInd1 = {1}; double[] pInd1 = {1.0};
        relMap.addRelocationToAsset(1,0,dInd1,pInd1);
        //100% of customers are relocated to asset 1 when 0 is blocked
        int[] bA1 = {1};
        relMap.addRelocationProbToAsset(1.0,1,0,bA1);  
        
        //-----------------------
        //  ANALYSIS
        //-----------------------
        
        StateSpace S = new StateSpace(assets,relMap);

        StateDistribution stateDist = new StateDistribution();
        stateDist.setStateSpace(S);
        //stateDist.setToRandomProbabilities(123);
        int[] occupied = {0,0};
        stateDist.setOccupiedCapacity(occupied);
        double[][] margDist = stateDist.getMarginalStateDists();
        
        TransitionRateMatrix tranMat = new TransitionRateMatrix(S);
        tranMat.generateMatrix();
        
        System.out.println("Marg. dist. at t=0");
        for (int assetIdx=0; assetIdx<assets.length; assetIdx++){
            System.out.println("Asset Idx:");
            for (int i=0; i<margDist[assetIdx].length; i++){
                System.out.println(i + ": " + margDist[assetIdx][i]);
            }
            System.out.println();
        }

        StateDistSolver solver = new StateDistSolver(S,tranMat);
        solver.uniformization(stateDist,1,1e-6);

        margDist = stateDist.getMarginalStateDists();        
        
        System.out.println("Marg. dist. at t=1");
        for (int assetIdx=0; assetIdx<assets.length; assetIdx++){
            System.out.println("Asset Idx:");
            for (int i=0; i<margDist[assetIdx].length; i++){
                System.out.println(i + ": " + margDist[assetIdx][i]);
            }
            System.out.println();
        }
        
        
        
        //CHANGE CAPACITY
        
        //create the new assets
        Asset[] newAssets = new Asset[nAssets];
        int[] newCapacity={1,8};
        newAssets[0] = new Asset(newCapacity[0],phDists0,arrivalRate[0]);
        newAssets[1] = new Asset(newCapacity[1],phDists1,arrivalRate[1]);
        
        //adjust the state distribution
        S = new StateSpace(newAssets,relMap);
        stateDist.newStateSpace(S);
        
        margDist = stateDist.getMarginalStateDists();
        
        System.out.println("NEW STATE DIST.");
        for (int assetIdx=0; assetIdx<assets.length; assetIdx++){
            System.out.println("Asset Idx:");
            for (int i=0; i<margDist[assetIdx].length; i++){
                System.out.println(i + ": " + margDist[assetIdx][i]);
            }
            System.out.println("\n");
        }
        
        tranMat = new TransitionRateMatrix(S);
        tranMat.generateMatrix();
        
        solver = new StateDistSolver(S,tranMat);
        solver.uniformization(stateDist,1,1e-6);
        
        margDist = stateDist.getMarginalStateDists();        
        
        System.out.println("Marg. dist. at t=2");
        for (int assetIdx=0; assetIdx<assets.length; assetIdx++){
            System.out.println("Asset Idx:");
            for (int i=0; i<margDist[assetIdx].length; i++){
                System.out.println(i + ": " + margDist[assetIdx][i]);
            }
            System.out.println();
        }
        
        
    }
    
    
    public static void testTransitionRateMatrix(){
        //test generating the transition rate matrix
        
        
        //-----------------------
        //  PREAMPLE
        //-----------------------
        
        int nAssets = 2;
        Asset[] assets = new Asset[nAssets];
        int[] capacity={3,2};
        double[] arrivalRate = {3.2,2.1};
        
        //primary distribution in first asset
        double[] initialDistribution00 = {0.75,0.25};
        double[][] phaseTypeGenerator00 = {{-2,1},
                                         {3,-5}};
        
        //alternative distribution in first asset
        double[] initialDistribution01 = {0.10,0.90};
        double[][] phaseTypeGenerator01 = {{-3,2},
                                         {0.5,-8}};
        
        PhaseTypeDistribution[] phDists0 = new PhaseTypeDistribution[2];
        phDists0[0] = new PhaseTypeDistribution(initialDistribution00,phaseTypeGenerator00);
        phDists0[1] = new PhaseTypeDistribution(initialDistribution01,phaseTypeGenerator01);
        
        assets[0] = new Asset(capacity[0],phDists0,arrivalRate[0]);
        
        
        //primary distribution in second asset
        double[] initialDistribution10 = {0.10,0.90};
        double[][] phaseTypeGenerator10 = {{-10,1.5},
                                         {8,-9}};
        //alternative distribution in second asset
        double[] initialDistribution11 = {0.20,0.80};
        double[][] phaseTypeGenerator11 = {{-3,2},
                                         {8,-8.1}};
        
        PhaseTypeDistribution[] phDists1 = new PhaseTypeDistribution[2];
        phDists1[0] = new PhaseTypeDistribution(initialDistribution10,phaseTypeGenerator10);
        phDists1[1] = new PhaseTypeDistribution(initialDistribution11,phaseTypeGenerator11);
        
        assets[1] = new Asset(capacity[1],phDists1,arrivalRate[1]);
        
        CustomerRelocationMap relMap = new CustomerRelocationMap(nAssets);
        int[] dInd0 = {0,1}; double[] pInd0 = {0.01,0.99};
        relMap.addRelocationToAsset(0,1,dInd0,pInd0);
        //100% of customers are relocated to asset 1 when 0 is blocked
        int[] bA0 = {0};
        relMap.addRelocationProbToAsset(1.0,0,1,bA0);
        
        int[] dInd1 = {0,1}; double[] pInd1 = {0.05,0.95};
        relMap.addRelocationToAsset(1,0,dInd1,pInd1);
        //100% of customers are relocated to asset 1 when 0 is blocked
        int[] bA1 = {1};
        relMap.addRelocationProbToAsset(1.0,1,0,bA1); 
        
        
        
        
        //-----------------------
        //  ANALYSIS
        //-----------------------
        
        StateSpace S = new StateSpace(assets,relMap);
        TransitionRateMatrix tranMat = new TransitionRateMatrix(S);
        tranMat.generateMatrix();
        tranMat.convertToEmbeddedChain();
        
        //print result
        tranMat.printTransitionRateMatrix();
        
    }
    
    
    public static void testGetNewState(){
        //test and example of how to get the
        //next state
        
        int nAssets = 2;
        Asset[] assets = new Asset[nAssets];
        int[] capacity={3,2};
        double[] arrivalRate = {3.2,2.1};
        
        //primary distribution in first asset
        double[] initialDistribution00 = {0.75,0.25};
        double[][] phaseTypeGenerator00 = {{-2,1},
                                         {3,-5}};
        
        //alternative distribution in first asset
        double[] initialDistribution01 = {0.10,0.90};
        double[][] phaseTypeGenerator01 = {{-3,2},
                                         {0.5,-8}};
        
        PhaseTypeDistribution[] phDists0 = new PhaseTypeDistribution[2];
        phDists0[0] = new PhaseTypeDistribution(initialDistribution00,phaseTypeGenerator00);
        phDists0[1] = new PhaseTypeDistribution(initialDistribution01,phaseTypeGenerator01);
        
        assets[0] = new Asset(capacity[0],phDists0,arrivalRate[0]);
        
        
        //primary distribution in second asset
        double[] initialDistribution10 = {0.10,0.90};
        double[][] phaseTypeGenerator10 = {{-10,1.5},
                                         {8,-9}};
        //alternative distribution in second asset
        double[] initialDistribution11 = {0.20,0.80};
        double[][] phaseTypeGenerator11 = {{-3,2},
                                         {8,-8.1}};
        
        PhaseTypeDistribution[] phDists1 = new PhaseTypeDistribution[2];
        phDists1[0] = new PhaseTypeDistribution(initialDistribution10,phaseTypeGenerator10);
        phDists1[1] = new PhaseTypeDistribution(initialDistribution11,phaseTypeGenerator11);
        
        assets[1] = new Asset(capacity[1],phDists1,arrivalRate[1]);
        
        CustomerRelocationMap relMap = new CustomerRelocationMap(nAssets);
        int[] dInd0 = {0,1}; double[] pInd0 = {0.01,0.99};
        relMap.addRelocationToAsset(0,1,dInd0,pInd0);
        //100% of customers are relocated to asset 1 when 0 is blocked
        int[] bA0 = {0};
        relMap.addRelocationProbToAsset(1.0,0,1,bA0);
        
        int[] dInd1 = {0,1}; double[] pInd1 = {0.05,0.95};
        relMap.addRelocationToAsset(1,0,dInd1,pInd1);
        //100% of customers are relocated to asset 1 when 0 is blocked
        int[] bA1 = {1};
        relMap.addRelocationProbToAsset(1.0,1,0,bA1); 
        
        
        StateSpace S = new StateSpace(assets,relMap);
        
        //go to state 135
        for (int sidx=0; sidx<142; sidx++){
            S.nextState();
        }        
        System.out.println("Current state: " + S.stateIdx);
        System.out.println("New state: " + S.newState_PhaseChange(0,1,1,0));
        System.out.println("New state: " + S.newState_CapChange(0,1,1,"down"));
        System.out.println("New state: " + S.newState_CapChange(1,1,1,"up"));
        System.out.println("New state: " + S.newState_CapChange(1,0,1,"down"));
        S.currentTotalJumps();
        System.out.println("Current total jumps: " + S.ctjumps);
        
    }
    
    public static void testStateSpace(){
        //test and example of how to use the StateSpace class
        
        int nAssets = 2;
        Asset[] assets = new Asset[nAssets];
        int[] capacity={3,2};
        double[] arrivalRate = {3.2,2.1};
        
        //primary distribution in first asset
        double[] initialDistribution00 = {0.75,0.25};
        double[][] phaseTypeGenerator00 = {{-2,1},
                                         {3,-5}};
        
        //alternative distribution in first asset
        double[] initialDistribution01 = {0.10,0.90};
        double[][] phaseTypeGenerator01 = {{-3,2},
                                         {0.5,-8}};
        
        PhaseTypeDistribution[] phDists0 = new PhaseTypeDistribution[2];
        phDists0[0] = new PhaseTypeDistribution(initialDistribution00,phaseTypeGenerator00);
        phDists0[1] = new PhaseTypeDistribution(initialDistribution01,phaseTypeGenerator01);
        
        assets[0] = new Asset(capacity[0],phDists0,arrivalRate[0]);
        
        
        //primary distribution in second asset
        double[] initialDistribution10 = {0.10,0.90};
        double[][] phaseTypeGenerator10 = {{-10,1.5},
                                         {8,-9}};
        //alternative distribution in second asset
        double[] initialDistribution11 = {0.20,0.80};
        double[][] phaseTypeGenerator11 = {{-3,2},
                                         {8,-8.1}};
        
        PhaseTypeDistribution[] phDists1 = new PhaseTypeDistribution[2];
        phDists1[0] = new PhaseTypeDistribution(initialDistribution10,phaseTypeGenerator10);
        phDists1[1] = new PhaseTypeDistribution(initialDistribution11,phaseTypeGenerator11);
        
        assets[1] = new Asset(capacity[1],phDists1,arrivalRate[1]);
        
        CustomerRelocationMap relMap = new CustomerRelocationMap(nAssets);
        
        StateSpace S = new StateSpace(assets,relMap);
        
        System.out.println("State space size: " + S.stateSpaceSize);
        
        //enumerate the state space
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            for (int assetIdx=0; assetIdx<assets.length; assetIdx++){
                System.out.print(sidx + " Ast: " + (assetIdx+1) + " | ");
                for (int j=0; j<assets[assetIdx].nPhases.length; j++){
                    System.out.print(assets[assetIdx].currentState_CapDist(j) + " ");
                }
                System.out.print("| ");
                for (int j=0; j<assets[assetIdx].nPhases.length; j++){
                    int[] a = assets[assetIdx].currentState_PhaseType(j);
                    for (int k=0; k<assets[assetIdx].nPhases[j]; k++){
                        System.out.print(a[k] + " ");
                    }
                    System.out.print("| ");
                }
            }
            System.out.println();
            S.nextState();
        }


    }
    
}
