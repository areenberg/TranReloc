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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Anders Reenberg Andersen
 */

public class AggregatedResults {

    int[] timeSegment; //time
    double[] runtime; //time
    double[][] meanOccupancy; //time x asset
    double[][] blockingProbability; //time x asset
    double[][][] percentiles; //time x asset x percentile (0.01,0.025,0.25,0.5,0.75,0.975,0.99)
    double[][] capacity; //time x asset
    
    double[] per;
    
    public AggregatedResults(){
        
        //percentiles to evaluate
        double[] per0 = {0.01,0.025,0.25,0.50,0.75,0.975,0.99};
        per = new double[per0.length];
        for (int i=0; i<per0.length; i++){
            per[i] = per0[i];
        }
        
    }
    
    public void addResults(double[][] margDists, double[] cap, double elapsedTime){
        
        double[] newMeanOccupancy = new double[margDists.length];
        double[] newBlockingProbability = new double[margDists.length];
        double[][] newPercentiles = new double[margDists.length][per.length];
        
        if (timeSegment==null){
            timeSegment = extendVectorInt(0,timeSegment);
        }else{
            timeSegment = extendVectorInt((timeSegment[(timeSegment.length-1)]+1),timeSegment);
        }
        
        //evaluate results for each asset
        for (int assetIdx=0; assetIdx<margDists.length; assetIdx++){
            newMeanOccupancy[assetIdx] = meanValue(margDists[assetIdx]);
            newBlockingProbability[assetIdx] = blockingProbability(margDists[assetIdx]);
            
            for (int i=0; i<per.length; i++){
                newPercentiles[assetIdx][i] = percentile(per[i],margDists[assetIdx]); 
            }
            
        }
        
        meanOccupancy = extendVector1(newMeanOccupancy,meanOccupancy);
        blockingProbability = extendVector1(newBlockingProbability,blockingProbability);
        percentiles = extendVector2(newPercentiles,percentiles);
        capacity = extendVector1(cap,capacity);
        runtime = extendVector0(elapsedTime,runtime);
        
    }
    
    
    public double[][] getResults(){
        //returns the results in a matrix
        //with rows corresponding to each segment
        
        int l = 1 + meanOccupancy[0].length + blockingProbability[0].length + percentiles[0].length*per.length;

        double[][] res = new double[timeSegment.length][l];
        int idx;
        
        for (int t=0; t<res.length; t++){
            res[t][0] = timeSegment[t];
            
            idx=1;
            for (int assetIdx=0; assetIdx<meanOccupancy[t].length; assetIdx++){
                res[t][idx] = meanOccupancy[t][assetIdx];
                idx++;
            }
            for (int assetIdx=0; assetIdx<blockingProbability[t].length; assetIdx++){
                res[t][idx] = blockingProbability[t][assetIdx];
                idx++;
            }
            for (int assetIdx=0; assetIdx<percentiles[t].length; assetIdx++){
                for (int p=0; p<percentiles[t][assetIdx].length; p++){
                    res[t][idx] = percentiles[t][assetIdx][p];
                    idx++;
                }
            }
            
        }
        
        return(res);
    }
    
    public String resultsHeader(){
        
        String header = "segment,";
        
        for (int assetIdx=0; assetIdx<meanOccupancy[0].length; assetIdx++){
            header += "meanAsset" + assetIdx + ",";
        }
        for (int assetIdx=0; assetIdx<blockingProbability[0].length; assetIdx++){
            header += "blockProbAsset" + assetIdx + ",";
        }
        for (int assetIdx=0; assetIdx<capacity[0].length; assetIdx++){
            header += "capacityAsset" + assetIdx + ",";
        }
        for (int assetIdx=0; assetIdx<percentiles[0].length; assetIdx++){
            for (int p=0; p<per.length; p++){
                
                header += "percentileAsset" + assetIdx + "_" + String.valueOf(Math.round(per[p]*100)) + ",";
                
            }
        }
        header += "runtime(s)";
        
        
        return(header);
    }
    
    
    
    
    private double meanValue(double[] d){
        
        double y=0.0;
        for (int i=0; i<d.length; i++){
            y+=(i*d[i]);
        }
        
        return(y);
    }
    
    private double blockingProbability(double[] d){
        return(d[(d.length-1)]);
    }
    
    private double percentile(double p,double[] d){
        
        int idx=0;
        double cumProb=0.0;
        while ((d[idx]+cumProb)<p){
            cumProb+=d[idx];
            idx++;
        }
        
        return(idx);
    }
    
    
    private double[] extendVector0(double newValue, double[] oldVec){
        
        double[] newVec;
        
        if (oldVec!=null){
        
            newVec = new double[(oldVec.length+1)];
        
            for (int i=0; i<oldVec.length; i++){
                newVec[i] = oldVec[i];
            }
            newVec[oldVec.length]=newValue;
        
        }else{
            
            newVec = new double[1];
            newVec[0]=newValue;
            
        }
        
        
        return(newVec);
    }

    private double[][] extendVector1(double[] newValue, double[][] oldVec){
        
        double[][] newVec;
        
        if (oldVec!=null){
            
            newVec = new double[(oldVec.length+1)][];
        
            for (int i=0; i<oldVec.length; i++){
                newVec[i] = new double[oldVec[i].length];
                for (int j=0; j<oldVec[i].length; j++){
                    newVec[i][j] = oldVec[i][j];
                }
            }
        
            newVec[oldVec.length] = new double[newValue.length];
            for (int j=0; j<newValue.length; j++){
                newVec[oldVec.length][j] = newValue[j];
            }
        
        }else{
            
            newVec = new double[1][];
            newVec[0] = new double[newValue.length];
            for (int j=0; j<newValue.length; j++){
                newVec[0][j] = newValue[j];
            }
            
        }
        
        return(newVec);
    }
    
    private double[][][] extendVector2(double[][] newValue, double[][][] oldVec){
        
        double[][][] newVec;
        
        
        if (oldVec!=null){
        
            newVec = new double[(oldVec.length+1)][][];
        
            for (int i=0; i<oldVec.length; i++){
                newVec[i] = new double[oldVec[i].length][];
                for (int j=0; j<oldVec[i].length; j++){
                    newVec[i][j] = new double[oldVec[i][j].length];
                    for (int k=0; k<oldVec[i][j].length; k++){
                        newVec[i][j][k] = oldVec[i][j][k];
                    }
                }
            }
            
            newVec[oldVec.length] = new double[newValue.length][];
            for (int j=0; j<newValue.length; j++){
                newVec[oldVec.length][j] = new double[newValue[j].length];
                for (int k=0; k<newValue[j].length; k++){
                    newVec[oldVec.length][j][k] = newValue[j][k];
                }
            }
        
        }else{
            
            newVec = new double[1][][];
            newVec[0] = new double[newValue.length][];
            for (int j=0; j<newValue.length; j++){
                newVec[0][j] = new double[newValue[j].length];
                for (int k=0; k<newValue[j].length; k++){
                    newVec[0][j][k] = newValue[j][k];
                }
            }
            
        }
        
        
        return(newVec);
    }
    
    private int[] extendVectorInt(int newValue, int[] oldVec){
        
        int[] newVec;
        
        if (oldVec!=null){
        
            newVec = new int[(oldVec.length+1)];
        
            for (int i=0; i<oldVec.length; i++){
                newVec[i] = oldVec[i];
            }
            newVec[oldVec.length]=newValue;
        
        }else{
            
            newVec = new int[1];
            newVec[0]=newValue;
        }
        
        
        return(newVec);
    }
    
    
    
    public void writeResultsToFile(String fileName){
        
        String[] stringArray = new String[(timeSegment.length+1)];
        stringArray[0] = resultsHeader();
        
        String str;
        for (int i=1; i<stringArray.length; i++){
            
            str=String.valueOf(timeSegment[(i-1)]) + ",";
            
            for (int assetIdx=0; assetIdx<meanOccupancy[(i-1)].length; assetIdx++){
                str += String.valueOf(meanOccupancy[(i-1)][assetIdx])+",";
            }
            for (int assetIdx=0; assetIdx<blockingProbability[(i-1)].length; assetIdx++){
                str += String.valueOf(blockingProbability[(i-1)][assetIdx])+",";
            }
            for (int assetIdx=0; assetIdx<capacity[(i-1)].length; assetIdx++){
                str += String.valueOf((int)capacity[(i-1)][assetIdx])+",";
            }
            for (int assetIdx=0; assetIdx<percentiles[(i-1)].length; assetIdx++){
                for (int p=0; p<per.length; p++){
                    str += String.valueOf(percentiles[(i-1)][assetIdx][p])+",";
                }
            }
            str+=runtime[(i-1)];
            
            stringArray[i]=str;
            
        }
        
        writeStringToFile(fileName,stringArray);
    }
    
    private void writeStringToFile(String fileName, String[] stringArray){
        //writes an array of strings to
        //a file on the computer
        
        //create the file
        try {
            File resFile = new File(fileName);
            if (resFile.createNewFile()) {
                System.out.println("File created: " + resFile.getName());
            }else{
                System.out.println(fileName + " already exists.");
            }
    
            
        }catch(IOException e){
            System.out.println("An error occurred writing " + fileName);
            e.printStackTrace();
        }        
        
        try {
            FileWriter mw = new FileWriter(fileName);
            
            //insert data
            for (int i=0; i<stringArray.length; i++){
                mw.write(stringArray[i]+"\n");
            }
            
            mw.close();
        }catch(IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        
    }
    
    
    
}
