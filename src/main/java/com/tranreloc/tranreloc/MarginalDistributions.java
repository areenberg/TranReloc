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
public class MarginalDistributions {
    //stores the marginal distributions for a <<single>> asset
    
    double[][] marginalDist; //timeSegment x occupancy
    int nTimeSegments; //total number of timeSegments
    
    public MarginalDistributions(int nTimeSegments){
        
        this.nTimeSegments = nTimeSegments;
        marginalDist = new double[nTimeSegments][];
        
    }
    
    public void addMargDist(double[] margDist, int timeSegment){
        
        marginalDist[timeSegment] = new double[margDist.length];
        for (int i=0; i<margDist.length; i++){
            marginalDist[timeSegment][i] = margDist[i];
        }
        
    }
    
    
    public double[] getMargDist(int timeSegment){
        return(marginalDist[timeSegment]);
    } 
    
    
    
}



