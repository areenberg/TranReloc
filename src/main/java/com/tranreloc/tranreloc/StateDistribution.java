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

import java.util.Random;

/**
 *
 * @author Anders Reenberg Andersen
 */

public class StateDistribution {

    double[] stateDist;
    StateSpace S;
    
    public StateDistribution(){
        
    }
    
    public void setStateSpace(StateSpace S){
        //store a new state space without
        //modifying the state distribution
        
        this.S = S;
        
    }
    
    public void setStateDistribution(double[] newDist){
        
        stateDist = new double[newDist.length];
        for (int i=0; i<stateDist.length; i++){
            stateDist[i]=newDist[i];
        }
        
    }
    
    
    public void newStateSpace(StateSpace newS){
        //change to a new state space and
        //modify the current state distribution
        //accordingly
        
        System.out.print("Changing state space ");
        
        long nscomp = (long) S.stateSpaceSize * (long)newS.stateSpaceSize;
             
        if (nscomp>=1e8){
            System.out.print("(fast method)...");
            newStateSpace_Fast(newS);
        }else{
            System.out.print("(accurate method)...");
            newStateSpace_Accurate(newS);
        }
    
        System.out.println(" done.");
    }
    
    public void newStateSpace_Fast(StateSpace newS){
        
        //check if capacity changed
        int idx=0;
        while (idx<newS.assets.length &&
                newS.assets[idx].capacity==S.assets[idx].capacity){
            idx++;
        }
        
        if (idx<newS.assets.length){
            
            double[] newStateDist = new double[newS.stateSpaceSize];
            double[] excessProb = new double[S.assets.length];
            double[][] margDist = getMarginalStateDists();
        
            for (int assetIdx=0; assetIdx<newS.assets.length; assetIdx++){
                if (S.assets[assetIdx].capacity>newS.assets[assetIdx].capacity){ 
                    for (int i=(newS.assets[assetIdx].capacity+1); i<margDist[assetIdx].length; i++){
                        excessProb[assetIdx] += margDist[assetIdx][i];
                    }
                }
            }
        
            double[][] newMargDist = new double[newS.assets.length][];
            double[][] szl = new double[newS.assets.length][];
            
            for (int assetIdx=0; assetIdx<newS.assets.length; assetIdx++){
                newMargDist[assetIdx] = new double[(newS.assets[assetIdx].capacity+1)];
            
                for (int i=0; i<(int)Math.min(newMargDist[assetIdx].length,margDist[assetIdx].length); i++){
                    newMargDist[assetIdx][i]=margDist[assetIdx][i];
                }
                newMargDist[assetIdx][(int)(Math.min(newMargDist[assetIdx].length,
                        margDist[assetIdx].length)-1)] += excessProb[assetIdx];
            
                szl[assetIdx] = new double[newMargDist[assetIdx].length];
            
                for (int i=0; i<newMargDist[assetIdx].length; i++){
                    szl[assetIdx][i] = 1.0/newS.assets[assetIdx].sizeOnLevel(i);
                }
            
            }
            
            double prob;
            newS.resetState();
        
            for (int sidx=0; sidx<newS.stateSpaceSize; sidx++){
        
                prob=1.0;
                for (int assetIdx=0; assetIdx<newS.assets.length; assetIdx++){
                    prob *= newMargDist[assetIdx][newS.assets[assetIdx].Kuse]*
                            szl[assetIdx][newS.assets[assetIdx].Kuse];
                }
                newStateDist[sidx]=prob;
            
            newS.nextState();
            }
            
            stateDist = new double[newStateDist.length];
            for (int sidx=0; sidx<newS.stateSpaceSize; sidx++){
                stateDist[sidx]=newStateDist[sidx];
            }
            
        }
        
        //store the new state space and
        //state distribution
        S=newS;
        
    }
    
    public void newStateSpace_Accurate(StateSpace newS){ 
        
        double[] newStateDist = new double[newS.stateSpaceSize];
        int[][] x = new int[S.assets.length][];
        int[][] bound = new int[S.assets.length][];
        int l,kk;
        double prob;
        for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
            l=0;
            for (int didx=0; didx<S.assets[assetIdx].nPhases.length; didx++){
                l+=S.assets[assetIdx].nPhases[didx];
            }
            x[assetIdx] = new int[l];
            bound[assetIdx] = new int[l];
        }
        
        
        S.resetState();
        for (int fromState=0; fromState<S.stateSpaceSize; fromState++){
            
                newS.resetState();
                for (int toState=0; toState<newS.stateSpaceSize; toState++){
                    
                    if (statesEqual(newS)){
                        
                        newStateDist[toState]+=stateDist[fromState];
                        
                    }else if (largerCapacity(newS)){
                        
                        prob=1.0;
                        for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
                            kk=0;
                            for (int didx=0; didx<S.assets[assetIdx].phDists.length; didx++){
                                for (int phidx=0; phidx<S.assets[assetIdx].nPhases[didx]; phidx++){
                                    x[assetIdx][kk]=S.assets[assetIdx].currentState_PhaseType(didx)[phidx]-newS.assets[assetIdx].currentState_PhaseType(didx)[phidx];
                                    bound[assetIdx][kk]=S.assets[assetIdx].currentState_PhaseType(didx)[phidx];
                                    kk++;
                                }
                            }
                            prob *= multinomProbWithBounds(x[assetIdx],bound[assetIdx]);
                        }
                        newStateDist[toState]+=(stateDist[fromState]*prob);
                        
                    }
                    newS.nextState();
                }
                S.nextState();
        }
        
        
        //store the new state space and
        //state distribution
        S=newS;
        
        stateDist = new double[newStateDist.length];
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            stateDist[sidx]=newStateDist[sidx];
        }
        
    }
    
    private boolean statesEqual(StateSpace newS){
        
        int assetIdx=0;
        int didx,phidx;
        while (assetIdx<S.assets.length){
            didx=0;
            while (didx<S.assets[assetIdx].phDists.length){
                phidx=0;
                while (phidx<S.assets[assetIdx].nPhases[didx]){
                    if (S.assets[assetIdx].currentState_PhaseType(didx)[phidx]!=
                            newS.assets[assetIdx].currentState_PhaseType(didx)[phidx]){
                        return(false);
                    }
                    phidx++;
                }
                didx++;
            }
            assetIdx++;
        }
        
        return(true);
    }
    
    private boolean largerCapacity(StateSpace newS){
        //checks if the state in the old state space
        //has more occupied products than is possible in
        //the new state space
        
        //also checks if it possible to transfer to the
        //new state
        
        int assetIdx=0;
        while (assetIdx<S.assets.length){
            if (S.assets[assetIdx].Kuse<newS.assets[assetIdx].Kuse){
                return(false);
            }else if(S.assets[assetIdx].Kuse>newS.assets[assetIdx].Kuse && 
                    newS.assets[assetIdx].Kuse<newS.assets[assetIdx].capacity){
                return(false);
            }
            assetIdx++;
        }
        int didx,phidx;
        
        assetIdx=0;
        while (assetIdx<S.assets.length){
            didx=0;
            while (didx<S.assets[assetIdx].phDists.length){
                phidx=0;
                while (phidx<S.assets[assetIdx].nPhases[didx]){
                    if (S.assets[assetIdx].currentState_PhaseType(didx)[phidx]<
                            newS.assets[assetIdx].currentState_PhaseType(didx)[phidx]){
                        return(false);
                    }
                    phidx++;
                }
                didx++;
            }
            assetIdx++;
        }

        return(true);
    }
    
    
    public void setOccupiedCapacity(int[] occupied){
        //adjust the state distribution such that the
        //probabilities correspond to the occupation
        //in the array occupied
        
        //assumes only the primary dist. in each asset
        //is occupied
        
        stateDist = new double[S.stateSpaceSize];
        
        boolean target;
        int idx;
        double prob;
        S.resetState();
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            
            //check if state is valid
            target=true;
            for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
                for (int didx=0; didx<S.assets[assetIdx].phDists.length; didx++){
                    if ( (didx==0 && S.assets[assetIdx].currentState_CapDist(didx)!=occupied[assetIdx]) ||
                            (didx>0 && S.assets[assetIdx].currentState_CapDist(didx)>0) ){
                        target=false;
                    }
                }
                
            }
            if (target){
                target=true;
                for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
                    for (int phidx=0; phidx<S.assets[assetIdx].nPhases[0]; phidx++){
                        if (S.assets[assetIdx].currentState_PhaseType(0)[phidx]!=occupied[assetIdx] &&
                                S.assets[assetIdx].currentState_PhaseType(0)[phidx]>0){
                            target=false;
                        }
                    }
                }
                
                //calculate probability
                if (target){
                    prob=1.0;
                    for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
                        if (occupied[assetIdx]>0){
                            idx=0;
                            while (idx<S.assets[assetIdx].nPhases[assetIdx] && 
                                    S.assets[assetIdx].currentState_PhaseType(0)[idx]!=occupied[assetIdx]){
                               idx++; 
                            }
                            prob *= S.assets[assetIdx].phDists[0].initialDistribution[idx];
                        }
                    }
                    stateDist[sidx]=prob;
                }
            
            }
            
            S.nextState();
        }
        
    }
    
    public void normalizeDist(){
        double sm=0;
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            sm+=stateDist[sidx];
        }
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            stateDist[sidx]/=sm;
        }
    }
    
    public void checkSum(){
        double sm=0;
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            sm+=stateDist[sidx];
        }
        System.out.println("State dist. sum: " + sm);
    }

    public void printStateDist(){
        for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
            System.out.println(stateDist[sidx]);
        }
    }
    
    public void setToAllAvailable(){
        //adjust the state distribution such that the
        //probabilities correspond to all assets
        //being available
        
        int[] occupied = new int[S.assets.length];
        setOccupiedCapacity(occupied);
        
    }
    
    public void setToRandomProbabilities(int seed){
        //fill the state distribution with
        //random probabilities
        
        stateDist = new double[S.stateSpaceSize];
        Random rnd = new Random(seed);
        double r,sm=0;
        for (int sidx=0; sidx<stateDist.length; sidx++){
            r = rnd.nextDouble();
            stateDist[sidx]=r;
            sm+=r;
        }
        for (int sidx=0; sidx<stateDist.length; sidx++){
            stateDist[sidx]/=sm;
        }
        
    }
    
    public double[][] getMarginalStateDists(){
        //returns the marginal state distributions
        //for each asset
        
        double[][] margDist = new double[S.assets.length][];
        for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
            margDist[assetIdx] = new double[(S.assets[assetIdx].capacity+1)];
        }
        
        for (int assetIdx=0; assetIdx<S.assets.length; assetIdx++){
            S.resetState();
            for (int sidx=0; sidx<S.stateSpaceSize; sidx++){
                margDist[assetIdx][S.assets[assetIdx].Kuse] += stateDist[sidx];
                S.nextState();
            }
        }
        
        return(margDist);
    }
    
    private int multinomialCoefficient(int[] x){
        //returns the multinomial coefficient
        
        int y1=1,y2=0;
        for (int i=0; i<x.length; i++){
            y1*=gamma((x[i]+1));
            y2+=x[i];
        }
        y2++;
        
        return((gamma(y2)/y1));
    }
    
    private int totalSequences(int[] x, int[] bound){
        
        //Problem: Visits the same solution multiple
        //times when the remaining use is distributed
        //among the bins.
        
        int use,sm=0;
        int[] x0 = new int[x.length];
        int[] xTemp = new int[(x.length-1)];
        for (int i=0; i<x.length; i++){
            sm+=x[i];
            x0[i]=x[i];
        }
        use=0;
        for (int i=0; i<xTemp.length; i++){
            xTemp[i]=x[(i+1)];
            use+=xTemp[i];
        }
        
        int totSeq=0;
        int j,d=0;
        
        do{
            if (d==0){
                totSeq+=multinomialCoefficient(x);
            }
            j = xTemp.length-1;
            
            while (j>=0){
                if (xTemp[j]==bound[(j+1)] || xTemp[j]==sm || use==sm){
                    use-=xTemp[j];
                    xTemp[j]=0;
                }else{
                    xTemp[j]++;
                    use++;
                    j=-1;
                }
                j--;
            }
            
            d=sm-use;
            
            x[0]=Math.min(Math.min(d,bound[0]),sm);
            d-=x[0];
            for (int i=1; i<x.length; i++){
                x[i]=xTemp[(i-1)];
            }
            
            
        }while(d>0 || !equalVectors(x0,x));
        
        return(totSeq);
    }
            
    
    private double multinomProbWithBounds(int[] x, int[] bound){
        //returns the probability of attaining
        //x given that items a picked with equal
        //probability and with an upper bound.
        
        int totalSeq = totalSequences(x,bound);
        
        return(((double)multinomialCoefficient(x)/(double)totalSeq));
    }
    
    
    private boolean equalVectors(int[] a, int[] b){
        for (int i=0; i<a.length; i++){
            if (a[i]!=b[i]){
                return(false);
            }
        }
        return(true);
    }
    
    
    
    private int factorial(int n){
        //From The flying keyboard
        // 2018 TheFlyingKeyboard and released under MIT License
        // theflyingkeyboard.net
        
        int fact = 1;
        for(int i = 2; i <= n; i++){
            fact *= i;
        }
        return fact;
    }
    
    private int gamma(int n){
        return(factorial(n-1));
    }
    

        
    
}
