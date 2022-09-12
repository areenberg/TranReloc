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
public class TransitionRateMatrix {
    
    StateSpace S;
    double[][] rates;
    int[][] columnIndices;
    double mxRate; //use for uniformization
    
    public TransitionRateMatrix(StateSpace S){
        
        this.S = S;
        
    }
    
    public void generateMatrix(){
        
        System.out.print("Generating transition rate matrix...");
        
        rates = new double[S.stateSpaceSize][];
        columnIndices = new int[S.stateSpaceSize][];
        double[] tempRates;
        int[] tempIndices;
        S.resetState();
        double diag,rt;
        int idx,ii,cidx;
        mxRate = Double.MIN_VALUE;
    
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            
            //allocate memory
            S.currentTotalJumps();
            //allocate current number of jumps plus diagonal
//            rates[sidx] = new double[(S.ctjumps+1)];
//            columnIndices[sidx] = new int[(S.ctjumps+1)];
            tempRates = new double[(S.ctjumps+1)];
            tempIndices = new int[(S.ctjumps+1)];
            
            //insert rates
            idx=0;
            diag=0;
            
            //loop through each asset
            for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
                
                //DISCHARGE AND LOCAL PHASE CHANGE
                //loop through each distribution in asset
                for (int didx=0; didx<S.assets[assetIdx].nPhases.length; didx++){
                    if (S.assets[assetIdx].currentState_CapDist(didx)>0){ //if some of the capacity is occupied
                       
                        for (int phIdx=0; phIdx<S.assets[assetIdx].nPhases[didx]; phIdx++){ //run through each phase in the dist.
                            if (S.assets[assetIdx].currentState_PhaseType(didx)[phIdx]>0){
                                //discharge rate
                                tempRates[idx] = rateDischarge(assetIdx,didx,phIdx)*(double)S.assets[assetIdx].currentState_PhaseType(didx)[phIdx];
                                diag+=tempRates[idx];
                                tempIndices[idx] = S.newState_CapChange(assetIdx, didx, phIdx,"down");
                                idx++;
                                
                                //local phase change - loop through the other phases
                                for (int phIdx_to=0; phIdx_to<S.assets[assetIdx].nPhases[didx]; phIdx_to++){
                                    if (phIdx!=phIdx_to){
                                        tempRates[idx] = rateLocalTransition(assetIdx,didx,phIdx,phIdx_to)*(double)S.assets[assetIdx].currentState_PhaseType(didx)[phIdx];
                                        diag+=tempRates[idx];
                                        tempIndices[idx] = S.newState_PhaseChange(assetIdx,didx,phIdx,phIdx_to);
                                        idx++;
                                    }
                                }
                                
                            }
                        }
                        
                    }
                }
                    
                //PRIMARY ARRIVALS
                if (S.assets[assetIdx].Kuse<S.assets[assetIdx].capacity){ //if there is free capacity
                    
                    //run through each phase in the primary dist.
                    for (int phIdx=0; phIdx<S.assets[assetIdx].nPhases[0]; phIdx++){
                        
                        cidx = S.newState_CapChange(assetIdx,0,phIdx,"up");
                        rt = rateArrival(assetIdx,phIdx);
                        ii=0;
                        while (ii<idx && tempIndices[ii]!=cidx){
                            ii++;
                        }
                        if (ii<idx){
                            tempRates[ii] += rt;
                        }else{
                            tempRates[idx] = rt;
                            tempIndices[idx] = cidx;
                            idx++; 
                        }
                        diag+=rt;
                        
                    }
                    
                //RELOCATIONS TO OTHER ASSETS
                }else if (S.assets[assetIdx].Kuse==S.assets[assetIdx].capacity){ //if entire capacity is occupied
                        
                    for (int assetIdx_to=0; assetIdx_to<S.assets.length; assetIdx_to++){
                        if (assetIdx!=assetIdx_to && S.assets[assetIdx_to].Kuse<S.assets[assetIdx_to].capacity &&
                                S.relMap.canRelocateToAsset(assetIdx,assetIdx_to)){
                            
                                for (int didx=0; didx<S.relMap.getRelocationToDist(assetIdx,assetIdx_to).length; didx++){
                                    for (int phIdx=0; phIdx<S.assets[assetIdx_to].nPhases[S.relMap.getRelocationToDist(assetIdx,assetIdx_to)[didx]]; phIdx++){
                                        
                                        cidx = S.newState_CapChange(assetIdx_to,
                                                S.relMap.getRelocationToDist(assetIdx,assetIdx_to)[didx], phIdx, "up");
                                        rt = rateRelocation(assetIdx,assetIdx_to,didx,phIdx);
                                        ii=0;
                                        while (ii<idx && tempIndices[ii]!=cidx){
                                            ii++;
                                        }
                                        if (ii<idx){
                                            tempRates[ii] += rt;
                                        }else{
                                            tempRates[idx] = rt;
                                            tempIndices[idx] = cidx;
                                            idx++;
                                        }
                                        diag+=rt;
                                        
                                        
                                        
                                    }
                                    
                                }
                            
                        }
                    }
                    
                    
                }
                
            }
            
            //store values
            rates[sidx] = new double[(idx+1)];
            columnIndices[sidx] = new int[(idx+1)];
            for (int tidx=0; tidx<idx; tidx++){
                rates[sidx][tidx] = tempRates[tidx];
                columnIndices[sidx][tidx] = tempIndices[tidx];
            }
            
            //insert diagonal
            rates[sidx][idx] = -diag;
            columnIndices[sidx][idx] = sidx;
            
            //update maximum rate (compare the new diagonal to the current
            //largest absolute diagonal element)
            if (diag>mxRate){
                mxRate=diag;
            }
            
            //move to next state
            S.nextState();
        }
        
        System.out.println(" done.");
        
    }
    
    
    public void transposeTransitionMatrix(){
        
        System.out.print("Transposing...");

        double[][] tempRates = new double[rates.length][];
        int[][] tempIndices = new int[columnIndices.length][];
        int[] k = new int[rates.length];
        
        //allocate memory
        for (int sidx=0; sidx<rates.length; sidx++){
            for (int jidx=0; jidx<rates[sidx].length; jidx++){
                k[columnIndices[sidx][jidx]]++;
            }
        }
        for (int sidx=0; sidx<rates.length; sidx++){
            tempRates[sidx] = new double[k[sidx]];
            tempIndices[sidx] = new int[k[sidx]];
        }
        
        //insert values;
        k = new int[rates.length];
        for (int sidx=0; sidx<rates.length; sidx++){
            for (int jidx=0; jidx<rates[sidx].length; jidx++){
                tempRates[columnIndices[sidx][jidx]][k[columnIndices[sidx][jidx]]] = rates[sidx][jidx];
                tempIndices[columnIndices[sidx][jidx]][k[columnIndices[sidx][jidx]]] = sidx;
                
                k[columnIndices[sidx][jidx]]++;
            }
        }
        
        rates = tempRates;
        columnIndices = tempIndices;
        
        
//        double[][] tempRates = new double[rates.length][];
//        int[][] tempIndices = new int[columnIndices.length][];
//        
//        int kk,j; boolean found;
//        for (int sidx=0; sidx<rates.length; sidx++){
//            
//            kk=0;
//            for (int i=0; i<rates.length; i++){
//                found=false;
//                j=0;
//                while (j<columnIndices[i].length && !found){
//                    if (columnIndices[i][j]==sidx){
//                        kk++;
//                        found=true;
//                    }
//                    j++;
//                }
//            }
//            tempRates[sidx] = new double[kk];
//            tempIndices[sidx] = new int[kk];
//            
//            kk=0;
//            for (int i=0; i<rates.length; i++){
//                found=false;
//                for (j=0; j<columnIndices[i].length; j++){
//                    if (columnIndices[i][j]==sidx){
//                        if (!found){
//                            tempIndices[sidx][kk]=i;
//                            tempRates[sidx][kk]=rates[i][j];
//                            found=true;
//                        }else{
//                            tempRates[sidx][kk]+=rates[i][j];
//                        }
//                        
//                    }
//                }
//                if (found){
//                    kk++;
//                }
//            }
//            
//        }
//        
//        for (int i=0; i<rates.length; i++){
//            rates[i] = new double[tempRates[i].length];
//            columnIndices[i] = new int[tempIndices[i].length];
//            for (j=0; j<rates[i].length; j++){
//                rates[i][j] = tempRates[i][j];
//                columnIndices[i][j] = tempIndices[i][j];
//            }
//        }
//        
//        tempIndices=null;
//        tempRates=null;
        
        System.out.println(" done.");
    }
    
    
    public void convertToEmbeddedChain(){
        
        System.out.print("Converting to embedded chain...");
        
        double delta_t = 1.0/mxRate;
        for (int i=0; i<rates.length; i++){ //scale all transitions
            for (int j=0; j<rates[i].length; j++){
                rates[i][j] *= delta_t;
            }
        }
        for (int i=0; i<rates.length; i++){ //add 1 to diagonal
            for (int j=0; j<rates[i].length; j++){
                if (columnIndices[i][j]==i){
                    rates[i][j]+=1.0;
                }
            }
        }    
        
        System.out.println(" done.");
        
    }

    
    private double rateDischarge(int assetIdx, int didx, int phIdx){
        //returns the raw exit rate.
        //multiplication with number of customers
        //occur outside this method.
        
        return(S.assets[assetIdx].phDists[didx].exitRates[phIdx]);
    }
    
    private double rateLocalTransition(int assetIdx, int didx,
            int phFrom, int phTo){
        //returns the raw local transition rate
        //from the PH generator.
        //multiplication with number of customers
        //occur outside this method.
        
        return(S.assets[assetIdx].phDists[didx].phaseTypeGenerator[phFrom][phTo]);
    }
    
    private double rateArrival(int assetIdx, int phIdx){
        //returns the arrival rate multiplied by the
        //element from the initial distribution
        
        //the arrivals are assumed primary and therefore didx=0
        
        return(S.assets[assetIdx].arrivalRatePH[phIdx]);
    }

    private double rateRelocation(int fromAsset, int toAsset, int didx, int phIdx){
        //returns the relocation arrival rate.
        //the complete rate is calculated within this method before
        //returned.
        //checking of the feasibility of the relocation occurs outside
        //of this method using the relMap object.
        
        //fromAsset - indicates the blocked asset and controls the arrival rate.
        //toAsset - indicates the asset the customers are relocated/routed to 
        //and controls the relocation probability.
        //didx - indicates the distribution that customers are relocated to 
        //within toAsset.
        //phIdx - indicates the phase in the selected distribution and controls the
        //initial distribution probability.
        
        //note: this method use a state-dependent relocation probability
        //(the current state can be returned directly from the state space object, S).
            
        return(S.assets[fromAsset].arrivalRate*
                S.relMap.getRelocationProbToAsset(fromAsset,toAsset,currentlyBlockedAssets())*
                S.relMap.getRelocationProbToDist(fromAsset,toAsset)[didx]*
                S.assets[toAsset].phDists[S.relMap.getRelocationToDist(fromAsset,toAsset)[didx]].initialDistribution[phIdx]);
        
    }
    
    private int[] currentlyBlockedAssets(){
        //indices of the currently blocked
        //assets
        
        int n=0;
        for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
            if (S.assets[assetIdx].Kuse==S.assets[assetIdx].capacity){
                n++;
            }
        }
        
        int[] blockedAssets = new int[n];
        n=0;
        for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
            if (S.assets[assetIdx].Kuse==S.assets[assetIdx].capacity){
                blockedAssets[n] = assetIdx;
                n++;
            }
        }
        
        return(blockedAssets);
    }
    
    public void printTransitionRateMatrix(){
        
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            System.out.print(sidx + ": ");
            for (int j=0; j<columnIndices[sidx].length; j++){
                System.out.print(rates[sidx][j] + "(" + columnIndices[sidx][j] + ")" + " ");
            }
            System.out.println();
        }
        
    }

    
    
    
}
