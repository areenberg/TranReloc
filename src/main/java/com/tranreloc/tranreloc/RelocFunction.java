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

//Class that contains and controls the relocation probability of
//one asset to another. The relocation probability is a function
//of the asssets that are blocked in the system.


public class RelocFunction {

    
    int fromAsset,toAsset;
    int nAssets;
    int[] prodVec;
    int position;
    
    double[] relProb;
            
            
    public RelocFunction(int fromAsset, int toAsset, int nAssets){
        
        this.nAssets = nAssets;
        this.fromAsset = fromAsset;
        this.toAsset = toAsset;
        
        initialize();
        
    }
    
    private void initialize(){
        
        //allocate space for the possible combinations
        //of relocation probabilities
        relProb = new double[(int)Math.pow(2.0,nAssets)];
        
        //calculcate jumb size for each asset
        int c=1;
        prodVec = new int[nAssets];
        for (int i=(nAssets-1); i>=0; i--){
            prodVec[i]=c;
            c*=2;
        }
        
    } 
    
    
    public void addRelocProbability(double probability, int[] blockedAssets){
        //store relocation probability corresponding to
        //the array of blocked assets
        
        //derive position in array
        position = 0;
        if (blockedAssets!=null){
            
            for (int i=0; i<blockedAssets.length; i++){
                position+=prodVec[blockedAssets[i]];
            }
            
        }
        
        relProb[position] = probability;
    
    }
    
    public double getRelocProbability(int[] blockedAssets){
        //return relocation probability corresponding
        //to the array of blocked assets.
        
        //derive position in array
        position = 0;
        if (blockedAssets!=null){
            
            for (int i=0; i<blockedAssets.length; i++){
                position+=prodVec[blockedAssets[i]];
            }
            
        }
        
        return(relProb[position]);
    }
    
    
    
    
//    private int[] sortAscending(int[] a){
//        
//        int[] b = new int[a.length];
//        boolean[] tagged = new boolean[a.length];
//        int added=0;
//        
//        int minVal;
//        int minIdx;
//        
//        while(added<a.length){
//            minIdx=-1;
//            minVal=Integer.MAX_VALUE;
//            for (int i=0; i<a.length; i++){
//                if (!tagged[i] && a[i]<minVal){
//                    minVal=a[i];
//                    minIdx=i;
//                }
//            }
//            b[added]=minVal;
//            tagged[minIdx]=true;
//            added++;
//        }
//        
//        return(b);
//    }
    
//    private int[][] appendToBlockedList(int[] blockedAssets, int[][] oldBlockedList){
//        
//        int[][] newBlockedList = new int[(oldBlockedList.length+1)][];
//        for (int i=0; i<oldBlockedList.length; i++){
//            newBlockedList[i] = oldBlockedList[i];
//        }        
//        newBlockedList[oldBlockedList.length] = blockedAssets;        
//        
//        return(newBlockedList);
//    }
            
//    private double[] appendToRelocList(double probability, double[] oldRelProb){
//        
//        double[] newRelProb = new double[(oldRelProb.length+1)];
//        for (int i=0; i<oldRelProb.length; i++){
//            newRelProb[i] = oldRelProb[i];
//        }
//        newRelProb[oldRelProb.length] = probability;
//        
//        return(newRelProb);
//    }        
    

    
    
}
