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
public class PhaseTypeDistribution {
    
    double[] initialDistribution;
    double[][] phaseTypeGenerator;
    double[] exitRates;
    int numberOfPhases;
    
    public PhaseTypeDistribution(double[] initialDistribution,
            double[][] phaseTypeGenerator){
        
        this.initialDistribution = initialDistribution;
        this.phaseTypeGenerator = phaseTypeGenerator;
        numberOfPhases = initialDistribution.length;
        
        calculateExitRates();
    }
    
    
    private void calculateExitRates(){
        
        exitRates = new double[numberOfPhases];
        double sm;
        for (int i=0; i<phaseTypeGenerator.length; i++){
            sm=0;
            for (int j=0; j<phaseTypeGenerator[i].length; j++){
                sm+=phaseTypeGenerator[i][j];
            }
            exitRates[i]=-sm;
        }
        
    }
    
    
    
    
    
}
