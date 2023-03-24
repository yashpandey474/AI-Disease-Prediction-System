/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.mycompany.heartdiseaseprediction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.lang.Math;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HeartDiseasePrediction {

    public static void main(String[] args) {
        DBConnection.createDatabase();
        DBConnection.createHeartDiseaseData();
        readAndAddData();
        
        ArrayList<HeartDiseaseMissingData> almd = new ArrayList<HeartDiseaseMissingData>();
        ArrayList<HeartDiseaseCompleteData> alcd = new ArrayList<HeartDiseaseCompleteData>();
        readMissingAndCompleteData(almd, alcd);
        
        int k2 = bestK_thal(alcd);
        int k1 = bestK_ca(alcd);
        
        System.out.println("CA K = " + k1 + " THAL K = " + k2);
        
        //USE K1 AND K2 TO CREATE
        completeValues(almd, alcd, k1, k2);
    }
    
    public static void completeValues(ArrayList<HeartDiseaseMissingData> testing,  ArrayList<HeartDiseaseCompleteData> training, int kCa, int kThal){
        ArrayList<HeartDiseaseCompleteData> replica = training;
        for(HeartDiseaseMissingData item: testing){
            if(item.ca == -1){
                double predicted = listAverage(kNearestNeighbors_ca(item, replica, kCa));
                item.setCa(predicted);
            }
            if(item.thal == -1){
                double predicted = listAverage(kNearestNeighbors_thal(item, replica, kThal));
                item.setThal(predicted);
            }
            HeartDiseaseCompleteData item1 = new HeartDiseaseCompleteData(item.age, item.sex, item.cp, item.trestbps, item.chol, item.fbs, item.restecg,item.thalach, item.exang, item.oldpeak, item.slope, item.ca, item.thal, item.num);
            replica.add(item1);
        }
    }
    public static void readAndAddData(){
        Scanner sc = null;
        try{
            sc = new Scanner(new FileInputStream("D://Notes BE//Sem_2_2//CS F407 (AI)//TheProject//HeartDiseasePrediction//src//main//java//com//mycompany//heartdiseaseprediction//processed.cleveland.data/"));
        }
        catch (FileNotFoundException e){
            System.out.println("File processed.cleveland.data was not found");
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
            String sqatnb = "insert into heartdata  "
                        + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
            try{
                PreparedStatement atnb = con.prepareStatement(sqatnb);
                atnb.setDouble(1, Double.parseDouble(vals[0]));
                atnb.setDouble(2, Double.parseDouble(vals[1]));
                atnb.setDouble(3, Double.parseDouble(vals[2]));
                atnb.setDouble(4, Double.parseDouble(vals[3]));
                atnb.setDouble(5, Double.parseDouble(vals[4]));
                atnb.setDouble(6, Double.parseDouble(vals[5]));
                atnb.setDouble(7, Double.parseDouble(vals[6]));
                atnb.setDouble(8, Double.parseDouble(vals[7]));
                atnb.setDouble(9, Double.parseDouble(vals[8]));
                atnb.setDouble(10, Double.parseDouble(vals[9]));
                atnb.setDouble(11, Double.parseDouble(vals[10]));
                atnb.setString(12, vals[11]);
                atnb.setString(13, vals[12]);
                atnb.setInt(14, Integer.parseInt(vals[13]));
                int addednrows = atnb.executeUpdate();
            }
            catch(Exception e){
                System.out.println("Error!");
                                System.out.println(Double.parseDouble(vals[0]));
                                System.out.println(Double.parseDouble(vals[1]));
            }
            
        }
        sc.close();
    }
    public static void readMissingAndCompleteData(ArrayList<HeartDiseaseMissingData> almd, ArrayList<HeartDiseaseCompleteData> alcd){
        Connection con = DBConnection.getConnection();
        try{
            String qry = "Select * from heartdata;";
            PreparedStatement ps = con.prepareStatement(qry);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                if(rs.getString("ca").equals("?") || rs.getString("thal").equals("?")){
                    HeartDiseaseMissingData tempMissing = new HeartDiseaseMissingData(
                            rs.getDouble("age"),
                            rs.getDouble("sex"),
                            rs.getDouble("cp"),
                            rs.getDouble("trestbps"),
                            rs.getDouble("chol"),
                            rs.getDouble("fbs"),
                            rs.getDouble("restecg"),
                            rs.getDouble("thalach"),
                            rs.getDouble("exang"),
                            rs.getDouble("oldpeak"),
                            rs.getDouble("slope"),
                            rs.getInt("num")
                    );
                    almd.add(tempMissing);
                }
                else{
                    HeartDiseaseCompleteData tempComplete = new HeartDiseaseCompleteData(
                            rs.getDouble("age"),
                            rs.getDouble("sex"),
                            rs.getDouble("cp"),
                            rs.getDouble("trestbps"),
                            rs.getDouble("chol"),
                            rs.getDouble("fbs"),
                            rs.getDouble("restecg"),
                            rs.getDouble("thalach"),
                            rs.getDouble("exang"),
                            rs.getDouble("oldpeak"),
                            rs.getDouble("slope"),
                            rs.getDouble("ca"),
                            rs.getDouble("thal"),
                            rs.getInt("num")
                    );
                    alcd.add(tempComplete);
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    private static double abs(double i) {
        if(i>0){
            return i;
        }    
        return -i;
    }
    public static double distanceMetric(HeartDiseaseCompleteData item1, HeartDiseaseCompleteData item2){
        //ASSUMING ITEM1 HAS THE MISSING DATA IN 6TH INDEX
        double diff = 0;
        diff += abs(item1.getAge() - item2.getAge());
        diff += abs(item1.getChol() - item2.getChol());
        diff += abs(item1.getCp() - item2.getCp());
        diff += abs(item1.getExang()- item2.getExang());
        diff += abs(item1.getFbs() - item2.getFbs());
        diff += abs(item1.getNum() - item2.getNum());
        diff += abs(item1.getOldpeak() - item2.getOldpeak());
        diff += abs(item1.getRestecg() - item2.getRestecg());
        diff += abs(item1.getSex() - item2.getSex());
        diff += abs(item1.getSlope() - item2.getSlope());
        diff += abs(item1.getThalach() - item2.getThalach());
        diff += abs(item1.getTrestbps() - item2.getTrestbps());
        return diff;
    }
    public static double distanceMetric(HeartDiseaseMissingData item1, HeartDiseaseCompleteData item2){
        //ASSUMING ITEM1 HAS THE MISSING DATA IN 6TH INDEX
        double diff = 0;
        diff += abs(item1.getAge() - item2.getAge());
        diff += abs(item1.getChol() - item2.getChol());
        diff += abs(item1.getCp() - item2.getCp());
        diff += abs(item1.getExang()- item2.getExang());
        diff += abs(item1.getFbs() - item2.getFbs());
        diff += abs(item1.getNum() - item2.getNum());
        diff += abs(item1.getOldpeak() - item2.getOldpeak());
        diff += abs(item1.getRestecg() - item2.getRestecg());
        diff += abs(item1.getSex() - item2.getSex());
        diff += abs(item1.getSlope() - item2.getSlope());
        diff += abs(item1.getThalach() - item2.getThalach());
        diff += abs(item1.getTrestbps() - item2.getTrestbps());
        return diff;
    }
    public static ArrayList<Double> kNearestNeighbors_ca(HeartDiseaseCompleteData item1, ArrayList<HeartDiseaseCompleteData> items, int k){
         ArrayList<Double> neighbors = new ArrayList<>(); //BARE NUCLEI STORED
         HashMap<Double, Double> nuclieDist = new HashMap<>(); //DISTANCE->BARE NUCLEI
        double distance = 0;

        for(HeartDiseaseCompleteData item: items){
            // System.out.println("IN Knn" + items.size());
            distance = distanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getCa());
                nuclieDist.put(distance, item.getCa());
            }    
            else{ 
                if(!nuclieDist.keySet().isEmpty() && Collections.max(nuclieDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getCa());
                    neighbors.remove(neighbors.indexOf(nuclieDist.get(Collections.max(nuclieDist.keySet()))));
                    nuclieDist.put(distance, item.getCa());
                    nuclieDist.remove(Collections.max(nuclieDist.keySet()));
                }
            }
        
        }

        return neighbors;
    }
    public static ArrayList<Double> kNearestNeighbors_ca(HeartDiseaseMissingData item1, ArrayList<HeartDiseaseCompleteData> items, int k){
         ArrayList<Double> neighbors = new ArrayList<>(); //BARE NUCLEI STORED
         HashMap<Double, Double> nuclieDist = new HashMap<>(); //DISTANCE->BARE NUCLEI
        double distance = 0;

        for(HeartDiseaseCompleteData item: items){
            // System.out.println("IN Knn" + items.size());
            distance = distanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getCa());
                nuclieDist.put(distance, item.getCa());
            }    
            else{ 
                if(!nuclieDist.keySet().isEmpty() && Collections.max(nuclieDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getCa());
                    neighbors.remove(neighbors.indexOf(nuclieDist.get(Collections.max(nuclieDist.keySet()))));
                    nuclieDist.put(distance, item.getCa());
                    nuclieDist.remove(Collections.max(nuclieDist.keySet()));
                }
            }
        
        }
        return neighbors;
    }
    public static ArrayList<Double> kNearestNeighbors_thal(HeartDiseaseCompleteData item1, ArrayList<HeartDiseaseCompleteData> items, int k){
        ArrayList<Double> neighbors = new ArrayList<>(); //BARE NUCLEI STORED
         HashMap<Double, Double> nuclieDist = new HashMap<>(); //DISTANCE->BARE NUCLEI
        double distance = 0;

        for(HeartDiseaseCompleteData item: items){
            // System.out.println("IN Knn" + items.size());
            distance = distanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getThal());
                nuclieDist.put(distance, item.getThal());
            }    
            else{ 
                if(!nuclieDist.keySet().isEmpty() && Collections.max(nuclieDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getThal());
                    neighbors.remove(neighbors.indexOf(nuclieDist.get(Collections.max(nuclieDist.keySet()))));
                    nuclieDist.put(distance, item.getThal());
                    nuclieDist.remove(Collections.max(nuclieDist.keySet()));
                }
            }
        
        }
        return neighbors;
    }
    public static ArrayList<Double> kNearestNeighbors_thal(HeartDiseaseMissingData item1, ArrayList<HeartDiseaseCompleteData> items, int k){
        ArrayList<Double> neighbors = new ArrayList<>(); //BARE NUCLEI STORED
         HashMap<Double, Double> nuclieDist = new HashMap<>(); //DISTANCE->BARE NUCLEI
        double distance = 0;

        for(HeartDiseaseCompleteData item: items){
            // System.out.println("IN Knn" + items.size());
            distance = distanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getThal());
                nuclieDist.put(distance, item.getThal());
            }    
            else{ 
                if(!nuclieDist.keySet().isEmpty() && Collections.max(nuclieDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getThal());
                    neighbors.remove(neighbors.indexOf(nuclieDist.get(Collections.max(nuclieDist.keySet()))));
                    nuclieDist.put(distance, item.getThal());
                    nuclieDist.remove(Collections.max(nuclieDist.keySet()));
                }
            }
        
        }
        return neighbors;
    }
    public static double percent(int a, int b){
        return (a/b)*100;
    }
    public static double listAverage(ArrayList<Double> arr){
        double sum = 0;
        for(Double i: arr){
            sum+=i;
        }
        return sum/arr.size();
    }
    public static ArrayList<HeartDiseaseCompleteData> cloneList(ArrayList<HeartDiseaseCompleteData> arr){
        ArrayList<HeartDiseaseCompleteData> replica = new ArrayList<>();
        for(HeartDiseaseCompleteData item: arr){
            replica.add(item);
        }
        return replica;
    }
    public static double accuracyOfK_ca(int k, ArrayList<HeartDiseaseCompleteData> trainingSet){
        ArrayList<HeartDiseaseCompleteData> replica;
        double known, predicted;
        int sumDiff =0;
        for(HeartDiseaseCompleteData item: trainingSet){
            known = item.getCa();
            predicted = listAverage((kNearestNeighbors_ca(item, trainingSet, k)));
            sumDiff += abs(predicted-known);
        }
        return sumDiff;
    }
    public static double accuracyOfK_thal(int k, ArrayList<HeartDiseaseCompleteData> trainingSet){
        ArrayList<HeartDiseaseCompleteData> replica;
        double known, predicted;
        int sumDiff =0;
        for(HeartDiseaseCompleteData item: trainingSet){
            known = item.getThal();
            predicted = listAverage((kNearestNeighbors_ca(item, trainingSet, k)));
            sumDiff += abs(predicted-known);
        }
        return sumDiff;
    }
    public static int bestK_ca(ArrayList<HeartDiseaseCompleteData> trainingSet){
        HashMap<Integer, Double> kmap = new HashMap<>();
        for(int k = 1; k<26; k++){
            kmap.put(k, accuracyOfK_ca(k, trainingSet));
        }
        int k = 1;
        for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
            if(ele.getValue()<kmap.get(k)){
                k = ele.getKey();
            }
        }
        return k;
    }
    public static int bestK_thal(ArrayList<HeartDiseaseCompleteData> trainingSet){
        HashMap<Integer, Double> kmap = new HashMap<>();
        for(int k = 1; k<26; k++){
            kmap.put(k, accuracyOfK_thal(k, trainingSet));
        }
        int k = 1;
        for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
            if(ele.getValue()<kmap.get(k)){
                k = ele.getKey();
            }
        }
        return k;
    }
}
