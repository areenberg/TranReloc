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

//Class mapping the relocation of customers between assets

public class CustomerRelocationMap {
    
    int nAssets;
    RelocFunction[][] relFunc; //tracks the relocation probability
    //between the assets
    
    int[][][] mapping; //the third index contains an array if indices
    //for the distributions that are used when customers from asset i
    //are relocated to asset j.
    
    double[][][] probDistReloc; //the third index contains the distribution
    //that governs how customers are distributed among the distribution
    //specified by the variable called mapping.
    
    
    
    public CustomerRelocationMap(int nAssets){
        
        this.nAssets = nAssets;
        
        initialize();
    }
    
    public void initialize(){
        
        mapping = new int[nAssets][nAssets][];
        for (int i=0; i<nAssets; i++){
            for (int j=0; j<nAssets; j++){
                mapping[i][j]=null;
            }
        }
        
        probDistReloc = new double [nAssets][nAssets][];
        for (int i=0; i<nAssets; i++){
            for (int j=0; j<nAssets; j++){
                probDistReloc[i][j]=null;
            }
        }        
        
        relFunc = new RelocFunction[nAssets][nAssets];
        for (int i=0; i<nAssets; i++){
            for (int j=0; j<nAssets; j++){
                if (i!=j){
                    relFunc[i][j] = new RelocFunction(i,j,nAssets);
                }
            }
        }
        
    }
    
    public void addRelocationToAsset(int fromAsset, int toAsset, int[] distIndices, double[] distProbs){
        //adds an array of indices for the distributions that customers
        //relocated from fromAsset can use in toAsset.
        //Example 1: If distIndices={1} means that all customers are relocated
        //to the distribution with index 1 (the first alternative dist.) in
        //toAsset.
        //Example 2: distIndices={0,1,2} means that customers relocated from
        //fromAsset will use distribution 0 (the primary dist.), dist. 1
        //and dist. 2 in toAsset.
        
        //the index mapping
        mapping[fromAsset][toAsset] = new int[distIndices.length];
        for (int i=0; i<distIndices.length; i++){
            mapping[fromAsset][toAsset][i] = distIndices[i];
        }
        
        //the corresponding distribution
        probDistReloc[fromAsset][toAsset] = new double[distProbs.length];
        for (int i=0; i<distProbs.length; i++){
            probDistReloc[fromAsset][toAsset][i] = distProbs[i];
        }
        
    }

    public void addRelocationProbToAsset(double probability, int fromAsset, int toAsset, int[] blockedAssets){
        //stores the relocation probability from the primary asset (fromAsset)
        //to the alternative asset (toAsset). The relocation probability depends
        //on the assets that are currently blocked (blockedAssets).
        
        //fromAsset must be included in blockedAssets even if it is the
        //only one blocked.
        
        relFunc[fromAsset][toAsset].addRelocProbability(probability,blockedAssets);
        
    }

    
    public double getRelocationProbToAsset(int fromAsset, int toAsset, int[] blockedAssets){
        //returns the relocation probability from the primary asset (fromAsset)
        //to the alternative asset (toAsset). The relocation probability depends
        //on the assets that are also currently blocked (blockedAssets).
        
        //fromAsset must be included in blockedAssets even if it is the
        //only one blocked.
        
        return(relFunc[fromAsset][toAsset].getRelocProbability(blockedAssets));
    }
    
    public int[] getRelocationToDist(int fromAsset, int toAsset){
        return(mapping[fromAsset][toAsset]);
    }
    
    public double[] getRelocationProbToDist(int fromAsset, int toAsset){
        return(probDistReloc[fromAsset][toAsset]);
    }
    
    public void clearRelocationToAsset(int fromAsset, int toAsset){
        mapping[fromAsset][toAsset] = null;
        probDistReloc[fromAsset][toAsset] = null;
    }
    
    public boolean canRelocateToAsset(int fromAsset, int toAsset){
        if (mapping[fromAsset][toAsset]==null){
            return(false);
        }else{
            return(true);
        }
    }
    
}
