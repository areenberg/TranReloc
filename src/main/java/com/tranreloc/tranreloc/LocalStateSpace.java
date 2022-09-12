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
public class LocalStateSpace {
    
    int nPhases;
    int c; //occupied servers
    
    int[][] localStateSpace;
    
    int currentIndex;
    
    
    public LocalStateSpace(int c, int nPhases){
        
        this.c = c;
        this.nPhases = nPhases;
        
        resetState();
    }
    
    public void generate(){
        //generates the local state space
        //with c occupied servers and
        //nPhases.
        
        int smAll,sw;
        
        localStateSpace = new int[(int)stateSpaceSize()][nPhases];
        
        localStateSpace[0][0] = c;
        
        if (localStateSpace.length>1){
            smAll=0;
            for (int i=1; i<localStateSpace.length; i++){
                for (int ii=0; ii<nPhases; ii++){
                    localStateSpace[i][ii] = localStateSpace[(i-1)][ii]; 
                }
                sw=1;
                for (int j=(nPhases-1); j>=1; j--){
                    if (sw==1 && smAll<c){
                        localStateSpace[i][j] = localStateSpace[i-1][j]+1;
                        smAll++;
                        sw=0;
                    }else if(sw==1){
                        smAll-=localStateSpace[i-1][j];
                        localStateSpace[i][j]=0;
                        sw=1;
                    }            
                }                
                localStateSpace[i][0] = c-smAll;
            }            
        }
        
        //since the local state space has changed
        //the state must be reset
        resetState();
        
    }
    
    
    public int getStateSpaceSize(){
        return(localStateSpace.length);
    }
    
    
    public void nextState(){
        currentIndex++;
        if (currentIndex==localStateSpace.length){
            currentIndex=0;
        }
    }
    
    public void resetState(){
        currentIndex=0;
    }
    
    public int[] getCurrentState(){
        return(localStateSpace[currentIndex]);
    }
    
    public long stateSpaceSize(){
        return(binomialCoefficient((c + nPhases-1), (nPhases-1))); 
    }
    
    private long binomialCoefficient(int n, int k){
        //From The flying keyboard
        // 2018 TheFlyingKeyboard and released under MIT License
        // theflyingkeyboard.net
        
        long top = 1;
        for(int i = n; i > k; --i){
            top *= i;
        }
        
        return (top / factorial(n - k));
    }
    
    private long factorial(int n){
        //From The flying keyboard
        // 2018 TheFlyingKeyboard and released under MIT License
        // theflyingkeyboard.net
        
        int fact = 1;
        for(int i = 2; i <= n; i++){
            fact *= i;
        }
        return (fact);
    }
    
    
    
}
