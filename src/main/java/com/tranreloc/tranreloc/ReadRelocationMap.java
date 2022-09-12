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

//Class for reading and storing the
//instructions for relocation in
//the system.
//The class also creates the relocation
//map object.


public class ReadRelocationMap {

    String dir;
    
    int nAssets;
    
    
    CustomerRelocationMap relMap;
    
    public ReadRelocationMap(String dir){
        
        this.dir = dir;
        
        readNumberOfAssets();
        createRelocationMap();
    }

    public CustomerRelocationMap getRelocationMap(){
        return(relMap);
    } 
    
    public void createRelocationMap(){
        
        relMap = new CustomerRelocationMap(nAssets);
        
        String fileName = dir + "/RelocationRules";
        String[] stringArray = fileToString(fileName);
        
        for (int i=0; i<stringArray.length; i++){
            String[] str = stringArray[i].split(",",10000);
            if (str[0].equals("betweenAssets")){
                addBetweenAssetsRule(stringArray[i]);
            }else if(str[0].equals("toDistsInAsset")){
                addToDistsInAssetsRule(stringArray[i]);
            }
        }
        
    }
    
    private void addBetweenAssetsRule(String str){
        
        String[] strSplit = str.split(",",10000);
        double relProb = Double.parseDouble(strSplit[1]);
        int fromAsset = Integer.parseInt(strSplit[2]);
        int toAsset = Integer.parseInt(strSplit[3]);
        
        double[] temp = arrayFromString(str,str.indexOf("{"),str.indexOf("}"));
        int[] blockedAssets = new int[temp.length];
        for (int i=0; i<temp.length; i++){
            blockedAssets[i] = (int) temp[i];
        }
        
        relMap.addRelocationProbToAsset(relProb,fromAsset,toAsset,blockedAssets);
        
    }
    
    private void addToDistsInAssetsRule(String str){
        
        String[] strSplit = str.split(",",10000);
        int fromAsset = Integer.parseInt(strSplit[1]);
        int toAsset = Integer.parseInt(strSplit[2]);
        
        double[] temp = arrayFromString(str,str.indexOf("{"),str.indexOf("}"));
        int[] distIndices = new int[temp.length];
        for (int i=0; i<temp.length; i++){
            distIndices[i] = (int) temp[i];
        }
        
        str = str.substring((str.indexOf("}")+2));
        double[] distProbs = arrayFromString(str,str.indexOf("{"),str.indexOf("}"));
        
        relMap.addRelocationToAsset(fromAsset,toAsset,distIndices,distProbs);
        
    }
    
    private double[] arrayFromString(String str, int startIdx, int stopIdx){
        
        String subStr = str.substring((startIdx+1),stopIdx);
        String[] strSplit = subStr.split(",",10000);
        
        double[] a = new double[strSplit.length];
        for (int i=0; i<strSplit.length; i++){
            a[i] = Double.parseDouble(strSplit[i]);
        }
        
        return(a);
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
    
    private void readNumberOfAssets(){
        
        String fileName = dir + "/NumberOfAssets"; 
        double[][] tempN = stringToDouble(fileToString(fileName),",",false);
        nAssets = (int) tempN[0][0];
        
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

    
    
}
