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


public class Asset {
    
    int capacity; //total capacity of the asset's inventory
    int nAlts; //number of alternative service time dist.
    int[] nPhases; //number of phases in each phase-type distributions (primary and alternatives)
    
    //ARRIVAL RATE
    double arrivalRate; //the raw arrival rate
    double[] arrivalRatePH; //the arrival rate to each phase in the primary dist.
    
    //SERVICE DISTRIBUTION - phase type distribution objects
    PhaseTypeDistribution[] phDists;
    
    LocalStateSpace[] localSts; //dist index (index=0 being the primary)
    LocalStateSpace tempLocalSts; //extra local state spaces for column calculations
    
    
    int[] capDist; //distribution of capacity between the primary and alternative distributions
    int nsize; //size of the mini-space accounting for the distribution of capacity  
    int cidx,Kuse;
    
    int assetStateSpaceSize; //the complete size of the state space for this asset
    int stateIdx;
    
    public Asset(int capacity, PhaseTypeDistribution[] phDists, double arrivalRate){
        
        nPhases = new int[phDists.length];
        for (int i=0; i<phDists.length; i++){
         nPhases[i] = phDists[i].numberOfPhases;  
        }
        
        arrivalRatePH = new double[nPhases[0]];
        for (int i=0; i<arrivalRatePH.length; i++){
            arrivalRatePH[i] = arrivalRate*phDists[0].initialDistribution[i];
        }
        
        this.phDists = phDists;
        this.arrivalRate = arrivalRate;
        this.capacity = capacity;
        this.nAlts = nPhases.length-1;
        
        initialize();
        
    }
    
    private void initialize(){
        localSts = new LocalStateSpace[nPhases.length];
        capDist = new int[nPhases.length];
        stateIdx=0;
        
        capDistSize();
        resetCapDist();
        calculateSize();
    }
    
    public int getAssetStateSpaceSize(){
        return(assetStateSpaceSize);
    }
    
    public int delta_capChange(int didx, int phChange, String direction){
        
        if (direction.equals("up")){
            return(delta_capChangeUp(didx,phChange));
        }else{
            return(-delta_capChangeDown(didx,phChange));
        }
        
    } 
    
    private int delta_capChangeUp(int didx, int phUp){
        
        int rep,tot,d0,d1,d2;
        
        //evaluate jump size until next capacity change (d0)
        
        d0=0;
        tot=1;
        for (int i=0; i<nPhases.length; i++){
            rep=1;
            if (i<(nPhases.length-1)){
                for (int j=(i+1); j<nPhases.length; j++){
                    rep*=localSts[j].getStateSpaceSize();
                }
            }
            d0 += (localSts[i].currentIndex*rep);
            tot*=localSts[i].getStateSpaceSize();
        }
        d0=tot-d0;
        
        //evaluate jump size to new capacity (d1)
        d1 = jumpSizeCapChange(didx,1);
        
        //evaluate jump size under the new capacity
        //to the new phase configuration
        
        int[] newPhases = new int[nPhases[didx]];
        for (int i=0; i<newPhases.length; i++){
            if (i==phUp){
                newPhases[i] = localSts[didx].getCurrentState()[i]+1;
            }else{
                newPhases[i] = localSts[didx].getCurrentState()[i];
            }
        }
        
        d2=0;
        int fidx=findIndexLocalSts(newPhases,(capDist[didx]+1),nPhases[didx]);
        for (int i=0; i<nPhases.length; i++){
            rep=1;
            if (i<(nPhases.length-1)){
                for (int j=(i+1); j<nPhases.length; j++){
                    if (j!=didx){
                        rep*=localSts[j].getStateSpaceSize();
                    }else{
                        rep*=tempLocalSts.getStateSpaceSize();
                    }
                }
            }
            if (i!=didx){
                d2 += (localSts[i].currentIndex*rep);
            }else{
                d2 += (fidx*rep);
            }
        }
        
        return((d0+d1+d2));
    }


    private int delta_capChangeDown(int didx, int phDown){
        
        int rep,tot,d0,d1,d2;
        
        //evaluate jump size until next capacity change (d0)
        
        d0=0;
        for (int i=0; i<nPhases.length; i++){
            rep=1;
            if (i<(nPhases.length-1)){
                for (int j=(i+1); j<nPhases.length; j++){
                    rep*=localSts[j].getStateSpaceSize();
                }
            }
            d0 += (localSts[i].currentIndex*rep);
        }
        
        //evaluate jump size to new capacity (d1)
        d1 = jumpSizeCapChange(didx,-1);
        
        //evaluate jump size under the new capacity
        //to the new phase configuration
        
        int[] newPhases = new int[nPhases[didx]];
        for (int i=0; i<newPhases.length; i++){
            if (i==phDown){
                newPhases[i] = localSts[didx].getCurrentState()[i]-1;
            }else{
                newPhases[i] = localSts[didx].getCurrentState()[i];
            }
        }
        
        d2=0;
        tot=1;
        int fidx=findIndexLocalSts(newPhases,(capDist[didx]-1),nPhases[didx]);
        for (int i=0; i<nPhases.length; i++){
            rep=1;
            if (i<(nPhases.length-1)){
                for (int j=(i+1); j<nPhases.length; j++){
                    if (j!=didx){
                        rep*=localSts[j].getStateSpaceSize();
                    }else{
                        rep*=tempLocalSts.getStateSpaceSize();
                    }
                }
            }
            if (i!=didx){
                tot*=localSts[i].getStateSpaceSize();
                d2 += (localSts[i].currentIndex*rep);
            }else{
                tot*=tempLocalSts.getStateSpaceSize();
                d2 += (fidx*rep);
            }
        }
        d2=tot-d2;
        
        return((d0+d1+d2));
    }

    private int jumpSizeCapChange(int didx, int diff){
        
        int pidx,Ktemp,dd,val;
        int[] capNew = new int[capDist.length];
        for (int i=0; i<capNew.length; i++){
            if (i!=didx){
                capNew[i] = capDist[i];
            }else{
                capNew[i] = capDist[i]+diff;
            }
        }
        
        val=0;
        dd=0;
        if (diff>0){
            
            Ktemp=Kuse;
            int[] capTemp = new int[capDist.length];
            for (int i=0; i<capTemp.length; i++){
                capTemp[i] = capDist[i];
            }            
            
            while (!equalVectors(capTemp,capNew)){
                pidx=nPhases.length-1;
                do{
                    if (Ktemp<capacity){
                        capTemp[pidx]++; Ktemp++;
                        pidx = -1;
                    }else if (capTemp[pidx]>0){
                        Ktemp -= capTemp[pidx];
                        capTemp[pidx] = 0;
                        pidx--;
                    }else{
                        pidx--;
                    }
                }while (pidx>=0);
                
                val+=dd;
                
                dd=1;
                for (int i=0; i<capTemp.length; i++){
                    dd*=fixedCapSize(capTemp[i],nPhases[i]);
                }
                
            }   
            
        }else{
            
            Ktemp=Kuse+diff;
            
            while (!equalVectors(capDist,capNew)){
                pidx=nPhases.length-1;
                do{
                    if (Ktemp<capacity){
                        capNew[pidx]++; Ktemp++;
                        pidx = -1;
                    }else if (capNew[pidx]>0){
                        Ktemp -= capNew[pidx];
                        capNew[pidx] = 0;
                        pidx--;
                    }else{
                        pidx--;
                    }
                }while (pidx>=0);
                
                val+=dd;
                
                dd=1;
                for (int i=0; i<capNew.length; i++){
                    dd*=fixedCapSize(capNew[i],nPhases[i]);
                }            
            }
            
        }
        
        return(val);
    }
    
    public int delta_localPhaseChange(int didx, int pFrom, int pTo){
        //returns the delta change (change relative to the current state)
        //associated with a local phase change (without a change of capacity) for
        //asset dsitribution didx, where there server changes from
        //phase pFrom to phase pTo.
        
        
        int[] newPhases = new int[localSts[didx].getCurrentState().length];
        for (int i=0; i<newPhases.length; i++){
            if (i==pFrom){
                newPhases[i] = localSts[didx].getCurrentState()[i]-1;
            }else if (i==pTo){
                newPhases[i] = localSts[didx].getCurrentState()[i]+1;
            }else{
                newPhases[i] = localSts[didx].getCurrentState()[i];
            }
        }
        
        //find index of state accounting for all dists in the asset
        int d1,d2,rep;
        d1=0;d2=0;
        for (int i=0; i<nPhases.length; i++){
            rep=1;
            if (i<(nPhases.length-1)){
                for (int j=(i+1); j<nPhases.length; j++){
                    rep*=localSts[j].getStateSpaceSize();
                }
            }
            if (i!=didx){
                d1 += (localSts[i].currentIndex*rep);
            }else{
                d1 += (findIndexLocalSts(newPhases,capDist[didx],nPhases[didx])*rep);
            }
            d2 += (localSts[i].currentIndex*rep);
        }
        
        //d1-d2 evaluates the difference from the
        //current state in the asset 
        
        //a positive delta means the new state
        //has higher index than the current state
        
        return((d1-d2));
    }
    
    private int findIndexLocalSts(int[] newPhases, int c, int nPh){
        
        tempLocalSts = new LocalStateSpace(c,nPh);
        tempLocalSts.generate();
        
        //assumes the vector is in the local state space
        //(i.e. does not check if index violate state
        //space size).
        while (!equalVectors(newPhases,tempLocalSts.getCurrentState())){
            tempLocalSts.nextState();
        }
        
        return(tempLocalSts.currentIndex);
    }
    
    
    public void nextState(){
        
        if (stateIdx<(assetStateSpaceSize-1)){
            stateIdx++;
        }else{
            stateIdx=0;
        }
        
        int idx=nPhases.length-1;
        do{
            localSts[idx].nextState();
            if (localSts[idx].currentIndex==0){
                idx--;
            }else{
                idx=Integer.MIN_VALUE; //exit
            }
        }while(idx>=0 && idx<(nPhases.length-1) && localSts[(idx+1)].currentIndex==0);
        
        if (idx==-1 && localSts[0].currentIndex==0){
            nextCapDist();
            for (int i=0; i<nPhases.length; i++){
                localSts[i] = new LocalStateSpace(capDist[i],nPhases[i]);
                localSts[i].generate();
            }
        }
        
    }
    
    public void resetState(){
        stateIdx=0;
        resetCapDist();
    }
    
    public int currentState_CapDist(int didx){
        //get local state of occupied capacity
        //for the distribution with index didx
        
        return(capDist[didx]);
    }
    
    public int[] currentState_PhaseType(int didx){
        //get local state of occupied capacity
        //among phases in distribution with
        //index didx
        
        return(localSts[didx].getCurrentState());
    }
    
    private void resetCapDist(){
        for (int i=0; i<capDist.length; i++){
            capDist[i]=0;
            localSts[i] = new LocalStateSpace(capDist[i],nPhases[i]);
            localSts[i].generate();
        }
        cidx=0;
        Kuse=0;
    }
    
    private void nextCapDist(){
        //advance state index
        if (cidx<(nsize-1)){ 
            cidx++;
        
            //advance state form
            int pidx=nPhases.length-1;
            do{
                if (Kuse<capacity){
                    capDist[pidx]++; Kuse++;
                    pidx = -1;
                }else if (capDist[pidx]>0){
                    Kuse -= capDist[pidx];
                    capDist[pidx] = 0;
                    pidx--;
                }else{
                    pidx--;
                }
            }while (pidx>=0);
        
        }else{
            resetCapDist();
        }
        
    }
    
    private void capDistSize(){
        
        for (int c=0; c<=capacity; c++){
            nsize+=fixedCapSize(c,nPhases.length);
        }
    }
    
    private int[][] fixedCapDist(int x, int k){
        //all configurations of items x
        //in k different bins
        
        int smAll,sw;
        int l = fixedCapSize(x,k);
        int[][] s = new int[l][k];
        
        s[0][0] = x;
        
        if (s.length>1){
            smAll=0;
            for (int i=1; i<s.length; i++){
                for (int ii=0; ii<k; ii++){
                    s[i][ii] = s[(i-1)][ii]; 
                }
                sw=1;
                for (int j=(k-1); j>=1; j--){
                    if (sw==1 && smAll<x){
                        s[i][j] = s[i-1][j]+1;
                        smAll++;
                        sw=0;
                    }else if(sw==1){
                        smAll-=s[i-1][j];
                        s[i][j]=0;
                        sw=1;
                    }            
                }                
                s[i][0] = x-smAll;
            }            
        }
        
        return(s);
    }
    
    
    private void calculateSize(){
        assetStateSpaceSize=0;

        int pidx,sm,K=0;
        int[] cp = new int[nPhases.length];
        
        for (int i=0; i<nsize; i++){
        
            pidx=nPhases.length-1;
        
            do{
                if (K<capacity){
                    cp[pidx]++; K++;
                    pidx = -1;
                }else if (cp[pidx]>0){
                    K -= cp[pidx];
                    cp[pidx] = 0;
                    pidx--;
                }else{
                    pidx--;
                }
            }while (pidx>=0);
            
            sm=1;
            for (int j=0; j<nPhases.length; j++){
                sm *= fixedCapSize(cp[j],nPhases[j]);
            }
            assetStateSpaceSize+=sm;
            
        }

    }
    
    public int sizeOnLevel(int capUse){
        //returns the number of states on a specific
        //capacity occupation level for the asset
        
        LocalStateSpace ls = new LocalStateSpace(capUse,nPhases.length);
        ls.generate();
        
        int sm,sl=0;
        for (int i=0; i<ls.localStateSpace.length; i++){
            sm=1;
            for (int j=0; j<nPhases.length; j++){
                sm *= fixedCapSize(ls.localStateSpace[i][j],nPhases[j]);
            }
            sl+=sm;
        }
        
        return(sl);
    }
    
    private int fixedCapSize(int x, int k){
        return((int)binomialCoefficient((x + k-1), (k-1))); 
    }
    
    private long binomialCoefficient(int n, int k){
        //From The flying keyboard
        // 2018 TheFlyingKeyboard and released under MIT License
        // theflyingkeyboard.net
        
        long top = 1;
        for(int i = n; i > k; --i){
            top *= i;
        }
        return (top/factorial(n - k));
    }
    
    private long factorial(int n){
        //From The flying keyboard
        // 2018 TheFlyingKeyboard and released under MIT License
        // theflyingkeyboard.net
        
        int fact = 1;
        for(int i = 2; i <= n; i++){
            fact *= i;
        }
        return fact;
    }
    
    private boolean equalVectors(int[] a, int[] b){
        //assumes both vectors have equal length
        
        int idx=0;
        while (idx<a.length && a[idx]==b[idx]){
            idx++;
        }
        if (idx==a.length){
            return(true);
        }        
        return(false);
    }
    
    
}




