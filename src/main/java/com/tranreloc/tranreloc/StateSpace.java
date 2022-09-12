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
public class StateSpace {
    
    Asset[] assets;
    
    int stateIdx;
    int stateSpaceSize;
    int[] reps;
    CustomerRelocationMap relMap;
    int ctjumps; //current total number of jumps
    
    public StateSpace(Asset[] assets, CustomerRelocationMap relMap){
        
        this.assets = assets;
        this.relMap = relMap;
        ctjumps=-1;
        
        calculateStateSpace();
        calculateReps();
        resetState();
        
    }
    
    private void calculateReps(){
        reps = new int[assets.length];
        reps[(assets.length-1)]=1;
        
        if (assets.length>1){
            for (int i=(assets.length-2); i>=0; i--){
                reps[i]=reps[(i+1)]*assets[(i+1)].assetStateSpaceSize;
            }
        }
        
    }
    
    public int newState_CapChange(int assetIdx, int didx, int ph, String dir){
        //returns the new state (i.e. the column index) associated with
        //a capacity change of 1.
        
        //assetIdx - is the asset index
        //didx - is the service-distribution associated with the asset
        //ph - is the index of the phase that changes
        //dir - is the direction of the change ("up" or "down")
        
        return((stateIdx+(assets[assetIdx].delta_capChange(didx,ph,dir)*reps[assetIdx])));
    }
    

    public int newState_PhaseChange(int assetIdx, int didx, int phFrom, int phTo){
        //returns the new state (i.e. the column index) associated with
        //a local phase change for a distribution in one of the assets
        
        //assetIdx - is the asset index
        //didx - is the service-distribution associated with the asset
        //phFrom - is the index of the phase that gives off a server
        //phTo - is the index of the phase that receives a server
        
        return((stateIdx+(assets[assetIdx].delta_localPhaseChange(didx, phFrom, phTo)*reps[assetIdx])));
    }
    
    public Asset currentState(int assetIdx){
        return(assets[assetIdx]);
    }
            
    public void nextState(){
        
        if (stateIdx<(stateSpaceSize-1)){
            stateIdx++;
        }else{
            stateIdx=0;
        }
        
        int idx=assets.length-1;
        do{
            assets[idx].nextState();
            if (assets[idx].stateIdx==0){
                idx--;
            }else{
                idx=Integer.MIN_VALUE; //exit
            }
        }while(idx>=0 && idx<(assets.length-1) && assets[(idx+1)].stateIdx==0);
        
        ctjumps=-1;
        
    }
    
    public void currentTotalJumps(){
        
        ctjumps=0;
        int j,k;
        
        for (int assetIdx=0; assetIdx<assets.length; assetIdx++){
            
            //capacity up
            if (assets[assetIdx].Kuse<assets[assetIdx].capacity){
                ctjumps+=assets[assetIdx].nPhases[0];
                
                for (j=0; j<assets.length; j++){
                    if (j!=assetIdx && assets[j].Kuse==assets[j].capacity && relMap.canRelocateToAsset(j,assetIdx)){
                        for (k=0; k<relMap.getRelocationToDist(j,assetIdx).length; k++){
                            ctjumps+=assets[assetIdx].nPhases[relMap.getRelocationToDist(j,assetIdx)[k]];
                        }
                    }
                }
                
            }
            //capacity down and local transitions
            if (assets[assetIdx].Kuse>0){
                for (j=0; j<assets[assetIdx].nPhases.length; j++){
                    for (k=0; k<assets[assetIdx].nPhases[j]; k++){
                        if (assets[assetIdx].currentState_PhaseType(j)[k]>0){
                            ctjumps++; //discharge
                            ctjumps+=(assets[assetIdx].nPhases[j]-1); //local transitions
                        }
                    }
                }
            }
            
        }
        
    }
            
    public void resetState(){
        stateIdx=0;
        for (int i=0; i<assets.length; i++){
            assets[i].resetState();
        }
        ctjumps=-1;
    }
    
    public void printState(){
        for (int assetIdx=0; assetIdx<assets.length; assetIdx++){
                System.out.print(" Ast: " + (assetIdx+1) + " | ");
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
        
    }
    
    private void calculateStateSpace(){
        stateSpaceSize=1;
        for (int i=0; i<assets.length; i++){
            stateSpaceSize *= assets[i].assetStateSpaceSize;
        }
        System.out.println("State space size: " + stateSpaceSize);
    }
    
    
}
