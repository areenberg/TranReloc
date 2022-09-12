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
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 *
 * @author Anders Reenberg Andersen
 */

//Class for reading and storing the basic
//system parameters

public class ReadParameters {
    
    //parameter directory
    String paramDir; 
    
    //parameters
    int nAssets;
    double[][] arrivalRates;
    int[][] capacity;
    int[] occupied;
    PhaseTypeDistribution[][][] phDists;
            
    
    public ReadParameters(String paramDir){
        
        this.paramDir = paramDir;
        
        readArrivalRates();
        readCapacity();
        readNumberOfAssets();        
        readCurrentlyOccupied();
        readRentalTime();
        
    }
    
    
    private void readRentalTime(){
        
        int[] nDists = readNumberOfAssetDists();
        int timePeriods = arrivalRates.length;
        String fileName;
        
        phDists = new PhaseTypeDistribution[timePeriods][nAssets][];
        
        for (int t=0; t<timePeriods; t++){
            for (int assetIdx=0; assetIdx<nAssets; assetIdx++){
                phDists[t][assetIdx] = new PhaseTypeDistribution[nDists[assetIdx]];
                for (int didx=0; didx<nDists[assetIdx]; didx++){
                    
                    fileName = paramDir + "/RentalTime/time" + t + "/asset" + assetIdx + "/distribution" + didx + "/phases";
                    double[] initialDistribution = readInitDist(fileName);
                    double[][] phaseTypeGenerator = readPHGen(fileName);
                    
                    phDists[t][assetIdx][didx] = new PhaseTypeDistribution(initialDistribution,phaseTypeGenerator);
                    
                }
            }
        }
        
    }
    
    
    private int[] readNumberOfAssetDists(){
        
        String fileName = paramDir + "/NumberOfAssetDists"; 
        double[][] temp = stringToDouble(fileToString(fileName),",",true);
        
        int[] y = new int[temp[0].length];
        for (int i=0; i<y.length; i++){
            y[i] = (int) temp[0][i];
        }
        
        return(y);
    }
    
    
    private void readNumberOfAssets(){
        
        String fileName = paramDir + "/NumberOfAssets"; 
        double[][] tempN = stringToDouble(fileToString(fileName),",",false);
        nAssets = (int) tempN[0][0];
        
    }
    
    private void readCurrentlyOccupied(){
        
        String fileName = paramDir + "/CurrentlyOccupied"; 
        double[][] temp = stringToDouble(fileToString(fileName),",",true);
        
        occupied = new int[temp[0].length];
        for (int i=0; i<occupied.length; i++){
            occupied[i] = (int) temp[0][i];
        }
        
    }
    
    private void readArrivalRates(){
        
        String fileName = paramDir + "/ArrivalRates"; 
        arrivalRates = stringToDouble(fileToString(fileName),",",true);
        
    }
    
    private void readCapacity(){
        
        String fileName = paramDir + "/Capacity"; 
        double[][] tempCap = stringToDouble(fileToString(fileName),",",true);
        
        capacity = new int[tempCap.length][];
        for (int i=0; i<capacity.length; i++){
            capacity[i] = new int[tempCap[i].length];
            for (int j=0; j<capacity[i].length; j++){
                capacity[i][j] = (int) tempCap[i][j];
            }
        }
        
    }
    
    private String[] fileToString(String fileName){
        
        int n=0;
        
        try {
            File file = new File(fileName);
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                reader.nextLine();
                n++;
            }
            
            reader = new Scanner(file);
            String[] y = new String[n];
            n=0;
            while (reader.hasNextLine()) {
                y[n]=reader.nextLine();
                n++;
            }
            
            reader.close();
            return(y);
        } catch (FileNotFoundException e) {
            System.out.println("Could not locate " + fileName);
            e.printStackTrace();
        }        
        
        return(null);
    }
    
    private double[][] stringToDouble(String[] stringArray, String sep, boolean header){
        
        int offset=0;
        if (header){
            offset++;
        }
        
        double[][] y = new double[(stringArray.length-offset)][];
        
        for (int i=0; i<y.length; i++){
            String[] str = stringArray[(i+offset)].split(sep,10000);
            y[i] = new double[str.length];
            for (int j=0; j<str.length; j++){
                y[i][j] = Double.parseDouble(str[j]);
            }
        }
        
        return(y);
    }
    
    private double[] readInitDist(String fileName){
        
        String[] str = fileToString(fileName);
        int n=0;
        for (int i=0; i<str.length; i++){
            if (!str[i].isEmpty()){
                n++;
            }
        }
        
        double[] y = new double[n];
        n=0;
        for (int i=0; i<str.length; i++){
            if (!str[i].isEmpty()){
                y[n]=Double.parseDouble(str[i].substring(0,str[i].indexOf(' ')));
                n++;
            }
        }
        
        return(y);
    }
    
    
    private double[][] readPHGen(String fileName){

        String[] str = fileToString(fileName);
        int n=0;
        for (int i=0; i<str.length; i++){
            if (!str[i].isEmpty()){
                n++;
            }
        }
        
        double[][] y = new double[n][];
        n=0;
        for (int i=0; i<str.length; i++){
            if (!str[i].isEmpty()){
                y[n]=getPHGenLine(str[i],y.length);
                n++;
            }
        }
        
        return(y);
    }
    
    private double[] getPHGenLine(String str, int splitLim){
        
        int idx=str.indexOf(' ');
        while(str.charAt(idx)==' '){
            idx++;
        }
        str=str.substring(idx,str.length());
        
        
        String[] strSplit = str.split(" ",splitLim);
        
        double[] y = new double[strSplit.length];
        for (int i=0; i<y.length; i++){
            try{
                y[i]=Double.parseDouble(strSplit[i]);
            }catch(NumberFormatException e){
                System.out.print("Tried to convert: '" + strSplit[i] +"'");
            }
        }
        
        return(y);
    }
    
    
    
    
}
