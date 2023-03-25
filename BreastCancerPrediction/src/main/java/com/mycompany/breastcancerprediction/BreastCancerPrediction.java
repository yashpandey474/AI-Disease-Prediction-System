/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.mycompany.breastcancerprediction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.lang.*;
import java.io.*;
import org.apache.commons.math3.stat.regression.MultipleLinearRegression;


public class BreastCancerPrediction {
    
    public static void main(String[] args) {
        
        DBConnection.createDatabase();
        DBConnection.createBreastCancerData();
        readAndAddData();
        
        ArrayList<BreastCancerMissingData> almd = new ArrayList<BreastCancerMissingData>();
        ArrayList<BreastCancerCompleteData> alcd = new ArrayList<BreastCancerCompleteData>();
        readMissingAndCompleteData(almd, alcd);
        //TESTING = ALMD
        //TRAINING = ALCD
        int k = bestK(alcd);
        System.out.println("K = " + k);
        completeValues(almd, alcd, k);
        
        doFeatureSelection(alcd);
    }
    public static void completeValues(ArrayList<BreastCancerMissingData> testing, ArrayList<BreastCancerCompleteData> training, int k){
         ArrayList<BreastCancerCompleteData> replica = training;
        for(BreastCancerMissingData item: testing){
            int predicted = listAverage(kNearestNeighbors(item, replica, k));
            item.setBareNuclei(predicted);
            BreastCancerCompleteData item1 = new BreastCancerCompleteData(item.codeNumber, item.clumpThickness, item.cSizeUni, item.cShapeUni, item.mAdhesion, item.secs, item.bareNuclei, 
            item.blandChromatin,  item.normalNucleoli, item.mitoses, item.classification);
            replica.add(item1);
        }
    }
    public static void readAndAddData(){
        Scanner sc = null;
        try{
            sc = new Scanner(new FileInputStream("D://Notes BE//Sem_2_2//CS F407 (AI)//TheProject//BreastCancerPrediction//src//main//java//com//mycompany//breastcancerprediction//breast-cancer-wisconsin.txt/"));
        }
        catch (FileNotFoundException e){
            System.out.println("File breast-cancer-wisconsin.data was not found");
            System.out.println("or could not be opened.");
            System.exit(0);
        }
        Connection con = DBConnection.getConnection();
        String line = null;
        int count=0;
        while (sc.hasNextLine()){
            line = sc.nextLine();
            count++;
            String[] vals = line.split(",");
            String sqatnb = "insert into bcancerdata  "
                        + "values (?,?,?,?,?,?,?,?,?,?,?);";
            try{
                PreparedStatement atnb = con.prepareStatement(sqatnb);
                atnb.setInt(1, Integer.parseInt(vals[0]));
                atnb.setInt(2, Integer.parseInt(vals[1]));
                atnb.setInt(3, Integer.parseInt(vals[2]));
                atnb.setInt(4, Integer.parseInt(vals[3]));
                atnb.setInt(5, Integer.parseInt(vals[4]));
                atnb.setInt(6, Integer.parseInt(vals[5]));
                atnb.setString(7, vals[6]);
                atnb.setInt(8, Integer.parseInt(vals[7]));
                atnb.setInt(9, Integer.parseInt(vals[8]));
                atnb.setInt(10, Integer.parseInt(vals[9]));
                atnb.setInt(11, Integer.parseInt(vals[10]));
                int addednrows = atnb.executeUpdate();
            }
            catch(Exception e){
                System.out.println("Error!");
                System.out.println(Integer.parseInt(vals[0]));
                System.out.println(Integer.parseInt(vals[1]));
                     
            }
            
        }
        sc.close();
    }
    public static void readMissingAndCompleteData(ArrayList<BreastCancerMissingData> almd, ArrayList<BreastCancerCompleteData> alcd){
        Connection con = DBConnection.getConnection();
        try{
            String qry = "Select * from bcancerdata;";
            PreparedStatement ps = con.prepareStatement(qry);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                if(rs.getString("barenuclei").equals("?")){
                    BreastCancerMissingData tempMissing = new BreastCancerMissingData(
                            rs.getInt("codenumber"),
                            rs.getInt("clumpthickness"),
                            rs.getInt("csizeuni"),
                            rs.getInt("cshapeuni"),
                            rs.getInt("madhesion"),
                            rs.getInt("secs"),
                            rs.getInt("blandchromatin"),
                            rs.getInt("normalnucleoli"),
                            rs.getInt("mitoses"),
                            rs.getInt("class")
                    );
                    almd.add(tempMissing);
                }
                else{
                    BreastCancerCompleteData tempComplete = new BreastCancerCompleteData(
                            rs.getInt("codenumber"),
                            rs.getInt("clumpthickness"),
                            rs.getInt("csizeuni"),
                            rs.getInt("cshapeuni"),
                            rs.getInt("madhesion"),
                            rs.getInt("secs"),
                            Integer.parseInt(rs.getString("barenuclei")),
                            rs.getInt("blandchromatin"),
                            rs.getInt("normalnucleoli"),
                            rs.getInt("mitoses"),
                            rs.getInt("class")
                    );
                    alcd.add(tempComplete);
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }


    public static int distanceMetric(BreastCancerCompleteData item1, BreastCancerCompleteData item2){
        //ASSUMING ITEM1 HAS THE MISSING DATA IN 6TH INDEX
        int diff = 0;
        diff += abs(item1.getBlandChromatin() - item2.getBlandChromatin());
        diff += abs(item1.getClassification() - item2.getClassification());
        diff += abs(item1.getClumpThickness() - item2.getClumpThickness());
        diff += abs(item1.getMitoses()- item2.getMitoses());
        diff += abs(item1.getNormalNucleoli() - item2.getNormalNucleoli());
        diff += abs(item1.getSecs() - item2.getSecs());
        diff += abs(item1.getcShapeUni() - item2.getcShapeUni());
        diff += abs(item1.getcSizeUni() - item2.getcSizeUni());
        diff += abs(item1.getmAdhesion() - item2.getmAdhesion());
        return diff;
    }
    public static int distanceMetric(BreastCancerMissingData item1, BreastCancerCompleteData item2){
        //ASSUMING ITEM1 HAS THE MISSING DATA IN 6TH INDEX
        int diff = 0;
        diff += abs(item1.getBlandChromatin() - item2.getBlandChromatin());
        diff += abs(item1.getClassification() - item2.getClassification());
        diff += abs(item1.getClumpThickness() - item2.getClumpThickness());
        diff += abs(item1.getMitoses()- item2.getMitoses());
        diff += abs(item1.getNormalNucleoli() - item2.getNormalNucleoli());
        diff += abs(item1.getSecs() - item2.getSecs());
        diff += abs(item1.getcShapeUni() - item2.getcShapeUni());
        diff += abs(item1.getcSizeUni() - item2.getcSizeUni());
        diff += abs(item1.getmAdhesion() - item2.getmAdhesion());
        return diff;
    }

    private static int abs(int i) {
        if(i>0){
            return i;
        }    
        return -i;
    }
    public static ArrayList<Integer> kNearestNeighbors(BreastCancerCompleteData item1, ArrayList<BreastCancerCompleteData> items, int k){
        ArrayList<Integer> neighbors = new ArrayList<>(); //BARE NUCLEI STORED
        HashMap<Integer, Integer> nuclieDist = new HashMap<>(); //DISTANCE->BARE NUCLEI
        int distance = 0;

        for(BreastCancerCompleteData item: items){
            // System.out.println("IN Knn" + items.size());
            distance = distanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getBareNuclei());
                nuclieDist.put(distance, item.getBareNuclei());
            }    
            else{ 
                if(!nuclieDist.keySet().isEmpty() && Collections.max(nuclieDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getBareNuclei());
                    neighbors.remove(neighbors.indexOf(nuclieDist.get(Collections.max(nuclieDist.keySet()))));
                    nuclieDist.put(distance, item.getBareNuclei());
                    nuclieDist.remove(Collections.max(nuclieDist.keySet()));
                }
            }
        
        }

        return neighbors;
    }
    public static ArrayList<Integer> kNearestNeighbors(BreastCancerMissingData item1, ArrayList<BreastCancerCompleteData> items, int k){
        ArrayList<Integer> neighbors = new ArrayList<>(); //BARE NUCLEI STORED
        HashMap<Integer, Integer> nuclieDist = new HashMap<>(); //DISTANCE->BARE NUCLEI
        int distance = 0;

        for(BreastCancerCompleteData item: items){
            // System.out.println("IN Knn" + items.size());
            distance = distanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getBareNuclei());
                nuclieDist.put(distance, item.getBareNuclei());
            }    
            else{ 
                if(!nuclieDist.keySet().isEmpty() && Collections.max(nuclieDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getBareNuclei());
                    neighbors.remove(neighbors.indexOf(nuclieDist.get(Collections.max(nuclieDist.keySet()))));
                    nuclieDist.put(distance, item.getBareNuclei());
                    nuclieDist.remove(Collections.max(nuclieDist.keySet()));
                }
            }
        
        }

        return neighbors;
    }
    
    public static double percent(int a, int b){
        return (a/b)*100;
    }
    public static int listAverage(ArrayList<Integer> arr){
        int sum = 0;
        for(Integer i: arr){
            sum+=i;
        }
        if(arr.isEmpty()){
            return 1000;
        }
        return sum/arr.size();
    }
    // public static ArrayList<BreastCancerCompleteData> cloneList(ArrayList<BreastCancerCompleteData> arr){
    //     ArrayList<BreastCancerCompleteData> replica = new ArrayList<>();
    //     for(BreastCancerCompleteData item: arr){
    //         replica.add(item);
    //     }
    //     return replica;
    // }
    public static double accuracyOfK(int k, ArrayList<BreastCancerCompleteData> trainingSet){
        ArrayList<BreastCancerCompleteData> replica = new ArrayList<>();
        int known, predicted;
        int sumDiff =0;
        replica = trainingSet;
        for(BreastCancerCompleteData item: trainingSet){
            known = item.getBareNuclei();
            // System.out.println("IN ORIG : " + trainingSet.size());
            // replica.remove(item);
            predicted = listAverage(kNearestNeighbors(item, replica, k));
            // replica.add(item);
            System.out.println("KNOWN = " + known + " PREDICTED ="+ predicted);
            sumDiff += abs(predicted-known);
        }
        return sumDiff;
    }

    public static int bestK(ArrayList<BreastCancerCompleteData> trainingSet){
        HashMap<Integer, Double> kmap = new HashMap<>();
        // System.out.println("SIZE IN OG " + trainingSet.size());
        for(int k = 1; k<26; k++){
            // System.out.println("HELLO");
            kmap.put(k, accuracyOfK(k, trainingSet));
            System.out.println("K = " + k + " VAL = " + kmap.get(k));
        }
        int k = 1;
        
       for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
//            System.out.println("K = " + ele.getKey() + " VAL = " + ele.getValue());
           if(ele.getValue()<kmap.get(k)){
               k = ele.getKey();
           }
       }
        return k;
    }
    public static double pvalueClumpThickness(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getClumpThickness()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static double pvalueCSizeUni(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getcSizeUni()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static double pvalueCShapeUni(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getcShapeUni()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static double pvalueMAdhesion(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getmAdhesion()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static double pvalueSecs(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getSecs()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static double pvalueBareNuclei(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getBareNuclei()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static double pvalueBlandChromatin(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getBlandChromatin()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static double pvalueNormalNecleoli(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getNormalNucleoli()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static double pvalueMitoses(ArrayList<BreastCancerCompleteData> aa){
        double tot=0, neg=0;
        for(BreastCancerCompleteData item: aa){
            if(item.getClassification()==2){
                tot++;
                if(item.getMitoses()>5) neg++;
            }
        }
        if(tot==0) return 0;
        return neg/tot;
    }
    public static void doFeatureSelection(ArrayList<BreastCancerCompleteData> alcd){
        MultipleLinearRegression model = new MultipleLinearRegression();
        double[][] X = new double[alcd.size()][9];
        double[] Y = new double[alcd.size()];
        int i=0;
        for(BreastCancerCompleteData item : alcd){
            X[i][0] = item.getClumpThickness();
            X[i][1] = item.getcSizeUni();
            X[i][2] = item.getcShapeUni();
            X[i][3] = item.getmAdhesion();
            X[i][4] = item.getSecs();
            X[i][5] = item.getBareNuclei();
            X[i][6] = item.blandChromatin;
            X[i][7] = item.getNormalNucleoli();
            X[i][8] = item.getMitoses(); 
            Y[i] = item.getClassification();
            i++;
        }
        model.newSampleData(X, Y);
        double[] pValues = model.getSignificanceLevels();
        for(int j=0; j<pValues.length; j++){
           if(pValues[j] > 0.05){
              // remove the independent variable from the model
              model.dropXColumn(j);
            }
        }
        model.fit();
    }
}



