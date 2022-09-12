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

//Class for solving the state distribution associated
//with the give transition rate matrix

public class StateDistSolver {

    StateSpace S;
    TransitionRateMatrix tranMat;
    
    
    public StateDistSolver(StateSpace S, TransitionRateMatrix tranMat){
     
        this.S = S;
        this.tranMat = tranMat;
        
        
    }
    
    
    
    public void uniformization(StateDistribution stateDist, double t,
            double epsilon){
        
//        tranMat.printTransitionRateMatrix();
        
        tranMat.transposeTransitionMatrix();
        tranMat.convertToEmbeddedChain();
        
        
        //calculate if mxRate*t causes underflow
        double tUnderflow = 70.0/tranMat.mxRate;
        int steps = 1;
        double[] tvec = {t};
        //if mxRate*t causes underflow, use uniformization in parts
        if (t>tUnderflow){ 
            steps = (int) Math.ceil(t/tUnderflow);
            tvec = new double[steps];
            for (int i=0; i<(steps-1); i++){
                tvec[i] = tUnderflow;
            }
            tvec[(steps-1)] = t - tUnderflow*(steps-1);
        }
        
        System.out.println("Uniformization requires " + steps + " steps.");
        System.out.print("Solving");
        for (int stp=0; stp<steps; stp++){
            uniformizationInParts(stateDist,tvec[stp],epsilon);
            stateDist.normalizeDist();
            System.out.print(".");
        }
        System.out.println(" done.");
        
    }
    
    private void uniformizationInParts(StateDistribution stateDist, double t,
            double epsilon){
        
        //get number of iterations
        double mxRatetk,mxRatet = tranMat.mxRate*t;
        int K = (int) numbiter(mxRatet,epsilon);
        
        double[] yOld = new double[stateDist.stateDist.length];
        double[] yNew;
        for (int i=0; i<yOld.length; i++){
            yOld[i]=stateDist.stateDist[i];
        }
        int jj;
        
        //iterate
        for(int k=1; k<=K; k++){
            yNew = new double[yOld.length];
            mxRatetk = mxRatet/(double)k;
            
            //method for the non-transposed transition matrix
            //Note: if the below is uncommented, then the transposeTransitionMatrix()
            //method in the uniformization method must be commented along with
            //the code for the transposed transition matrix below.
//            for (int i=0; i<yNew.length; i++){
//                for (int j=0; j<tranMat.rates.length; j++){
//                    for (jj=0; jj<tranMat.rates[j].length; jj++){
//                        if (tranMat.columnIndices[j][jj]==i){
//                            yNew[i] += (yOld[j]*tranMat.rates[j][jj]*mxRatetk);
//                        }
//                    }
//                }
//                stateDist.stateDist[i] += yNew[i];
//            }
            
            //method for the transposed transition matrix
            for (int i=0; i<yNew.length; i++){
                for (jj=0; jj<tranMat.rates[i].length; jj++){
                    yNew[i] += (yOld[tranMat.columnIndices[i][jj]]*tranMat.rates[i][jj]*mxRatetk);
                }
                stateDist.stateDist[i] += yNew[i];
            }

            
            
            for (int i=0; i<yOld.length; i++){
                yOld[i]=yNew[i];
            }
            
        }
        
        //finalize
        for (int i=0; i<stateDist.stateDist.length; i++){
            stateDist.stateDist[i] *= Math.exp(-mxRatet);
        }
        
    }
    
    private int numbiter(double gammat, double epsilon){
        //minimal number of iterations in uniformization.
        //takes the tolerance epsilon, and the product between
        //the uniformization rate and time.
      
        double sigma = 1;
        double si = 1;
        int K = 0; 
        
        double tol = (1-epsilon)*Math.exp(gammat);
        
        while (sigma<tol){           
            si *= (gammat)/(double)(K+1);
            sigma += si;
            K++;
        }
            
        return(K);
    }  
    
    
    
}
