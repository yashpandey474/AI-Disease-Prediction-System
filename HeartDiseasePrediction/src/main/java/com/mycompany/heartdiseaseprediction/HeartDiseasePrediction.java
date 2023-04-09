/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.mycompany.heartdiseaseprediction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

import javax.management.MBeanTrustPermission;
import javax.xml.transform.TransformerConfigurationException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.lang.Math;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;


public class HeartDiseasePrediction {
    private static final double INITIAL_TEMPERATURE = 1000;
    private static final double COOLING_RATE = 0.003;
    private static final int ITERATIONS_PER_TEMPERATURE = 100;

    public static double[] predict(double[][] X, double[] Y, double[][] newX) {
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        model.newSampleData(Y, X);
        double[] beta = model.estimateRegressionParameters();
        int n = newX.length;
        double[] predictions = new double[n];
        for (int i = 0; i < n; i++) {
            double[] x = newX[i];
            double yHat = beta[0];
            for (int j = 0; j < x.length; j++) {
                yHat += beta[j + 1] * x[j];
            }
            predictions[i] = yHat;
        }
        return predictions;
    }

    public static double[] getPvalues(double[][] X, double[] Y) {
        CustomOLS model = new CustomOLS();
        model.newSampleData(Y, X);
        double[] beta = model.estimateRegressionParameters();
        double[] se = model.estimateRegressionParametersStandardErrors();
        double[][] Xt = model.getDesignMatrix();
        int n = Xt.length;
        TDistribution tDist = new TDistribution(n - X[0].length - 1);
        double[] pValues = new double[beta.length];
        for (int i = 0; i < beta.length; i++) {
            double tStat = beta[i] / se[i];
            pValues[i] = 2 * tDist.cumulativeProbability(-Math.abs(tStat));
        }
        return pValues;
    }
    private static void shuffle(double[][] X, double[] Y) {
        int n = Y.length;
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            int j = rand.nextInt(n);
            // swap X[i] with X[j]
            double[] tempX = X[i];
            X[i] = X[j];
            X[j] = tempX;
            // swap Y[i] with Y[j]
            double tempY = Y[i];
            Y[i] = Y[j];
            Y[j] = tempY;
        }
    }
    
    public static double crossValidation( double[][] X, double[] Y){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        int k = 5; // number of folds
        int n = X.length; // size of data set
        int foldSize = n / k; // size of each fold
        double[] accuracy = new double[k]; // array to store accuracy of each fold

        // shuffle the data set randomly
        shuffle(X, Y);
        // perform k-fold cross-validation
        for (int i = 0; i < k; i++) {
            // split data into training and testing sets
            double[][] X_train = new double[n - foldSize][X[0].length];
            double[][] X_test = new double[foldSize][X[0].length];
            double[] Y_train = new double[n - foldSize];
            double[] Y_test = new double[foldSize];
            int trainIdx = 0;
            int testIdx = 0;
            for (int j = 0; j < n; j++) {
                if (j >= i * foldSize && j < (i + 1) * foldSize) {
                    // add data point to testing set
                    X_test[testIdx] = X[j];
                    Y_test[testIdx] = Y[j];
                    testIdx++;
                } else {
                    // add data point to training set
                    X_train[trainIdx] = X[j];
                    Y_train[trainIdx] = Y[j];
                    trainIdx++;
                }
            }
            
            // train model on training set
            model.newSampleData(Y_train, X_train);
            double[] beta = model.estimateRegressionParameters();
            double[] residuals = model.estimateResiduals();
            // Calculate the variance of the residuals
            double s2 = 0.0;
            for (int k1 = 0; k1< residuals.length; k1++) {
                s2 += residuals[k1] * residuals[k1];
            }
            s2 /= residuals.length - X[0].length;
            // Calculate the standard errors of the regression coefficients
           double[] stdErrs = new double[X[0].length];
            for (int k1 = 0; k1 < X[0].length; k1++) {
                stdErrs[k1] = Math.sqrt(s2 * model.calculateResidualSumOfSquares() / ((X.length - X[0].length) * model.calculateTotalSumOfSquares()));
            }
            // test model on testing set
            double[] predicted = predict(X_train, Y_train, X_test);
            for (int j = 0; j < predicted.length; j++) {
                if (predicted[j] > 0.5) {
                    predicted[j] = 1;
                } else {
                    predicted[j] = 0;
                }
            }
            
            // calculate accuracy of model on testing set
            accuracy[i] = calculateAccuracy(Y_test, predicted);
        }

        // calculate average accuracy over all folds
        double sum = 0;
        for (int i = 0; i < k; i++) {
            sum += accuracy[i];
        }
        double averageAccuracy = sum / k;
        // System.out.println("Average accuracy: " + averageAccuracy);
        return averageAccuracy;

}

    public static void main(String[] args) {
        DBConnection.createDatabase();
        DBConnection.createHeartDiseaseData();
        readAndAddData();
        
        ArrayList<HeartDiseaseMissingData> almd = new ArrayList<HeartDiseaseMissingData>();
        ArrayList<HeartDiseaseCompleteData> alcd = new ArrayList<HeartDiseaseCompleteData>();
        readMissingAndCompleteData(almd, alcd);
        
        int k1 = bestK_ca(alcd);
        int k2 = bestK_thal(alcd);

        
        // System.out.println("CA K = " + k1 + " THAL K = " + k2);
        
        //USE K1 AND K2 TO CREATE
        // completeByMeanError(alcd);
        completeValues(almd, alcd, k1, k2);
        normalFeatureSelection(alcd);
        // printPValues(alcd);
        // getDistribution(alcd, almd);
        // completeValues(almd, alcd, k1, k2);
        
    }

    public static void getDistribution(ArrayList<HeartDiseaseCompleteData> dataList, ArrayList<HeartDiseaseMissingData> almd){
            int numMales = 0;
            int numFemales = 0;
            int numHighChol = 0;
            int numYoung = 0;
            int numMiddleAged = 0;
            int numOld = 0;
            System.out.println("SIZE = " + dataList.size());
            for(HeartDiseaseCompleteData data : dataList){
                // Count males and females
                if(data.getSex() == 1){
                    numMales++;
                }
                if (data.getSex() == 0){
                    numFemales++;
                }
        
                // Count high cholesterol
        
                // Count age groups
                double age = data.getAge();
                if(age < 40){
                    numYoung++;
                }
                if(age < 60 && age>=40){
                    numMiddleAged++;
                }
                if(age>=60){
                    numOld++;
                }
            }
            for(HeartDiseaseMissingData data : almd){
                // Count males and females
                if(data.getSex() == 1){
                    numMales++;
                }
                if (data.getSex() == 0){
                    numFemales++;
                }
        
                // Count high cholesterol
        
                // Count age groups
                double age = data.getAge();
                if(age < 40){
                    numYoung++;
                }
                if(age < 60 && age>=40){
                    numMiddleAged++;
                }
                if(age>=60){
                    numOld++;
                }
            }
        
    
        System.out.println("Number of males: " + numMales);
        System.out.println("Number of females: " + numFemales);
                System.out.println("Number of young patients (<40 years old): " + numYoung);
        System.out.println("Number of middle-aged patients (40-59 years old): " + numMiddleAged);
        System.out.println("Number of old patients (>=60 years old): " + numOld);
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
            training.add(item1);
        }
    }

    public static void completeValuesMean(ArrayList<HeartDiseaseCompleteData> alcd, ArrayList<HeartDiseaseMissingData> almd){
        //REPLACING VALUES BY MEAN/MODE
        double sumthal = 0;
        double sumca = 0;
        int n = alcd.size();

        for(HeartDiseaseCompleteData cd: alcd){
            sumthal += cd.getThal();
            sumca += cd.getCa();
        }
        for(HeartDiseaseMissingData md: almd){
            if(md.getThal() != -1){
                sumthal+=md.getThal();
            }
            if(md.getCa() != -1){
                sumca += md.getCa();
            }
        }
        sumthal = sumthal/n;
        sumca = sumca/n;
        for(HeartDiseaseMissingData item: almd){
            if(item.getThal() == -1){
                item.setThal(sumthal);
            }
            if(item.getCa() == -1){
                item.setCa(sumca);
            }
            alcd.add(new HeartDiseaseCompleteData(item.age, item.sex, item.cp, item.trestbps, item.chol, item.fbs, item.restecg,item.thalach, item.exang, item.oldpeak, item.slope, item.ca, item.thal, item.num));
        }
    }
    public static void completeByMeanError(ArrayList<HeartDiseaseCompleteData> alcd){
        int n = alcd.size();
        double mean = 0;
        double meanThal = 0;
        for (HeartDiseaseCompleteData cd : alcd) {
            mean += cd.getCa();
            meanThal += cd.getThal();
        }
        mean /= alcd.size();
        meanThal /= alcd.size();
        double sumSquares = 0;
        double sumSquaresThal = 0;
        for (HeartDiseaseCompleteData cd : alcd) {
            double known = cd.getCa();
            double predicted = mean;
            double known1 = cd.getThal();
            double predictedThal = meanThal;
            double squaredDiff = Math.pow(predicted - known, 2);
            double squaredDiffThal = Math.pow(predictedThal - known1, 2);
            sumSquares += squaredDiff;
            sumSquaresThal += squaredDiffThal;
        }
        System.out.println("BY MEAN, CA DIFFERENCE = :" + sumSquares);
        // System.out.println("SIZE IN MEAN = " + alcd.size());
        System.out.println("BY MEAN, CA MSE = " + sumSquares/(double)alcd.size());
        System.out.println("BY MEAN, THAL DIFFERENCE = :" + sumSquaresThal);
        System.out.println("BY MEAN, THAL MSE = " + sumSquaresThal/(double)alcd.size());
        
    }
    public static void readAndAddData(){
        Scanner sc = null;
        try{
            sc = new Scanner(new FileInputStream("C:/AIprojectRep/HeartDiseasePrediction/src/main/java/com/mycompany/heartdiseaseprediction/processed.cleveland.data"));
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
        int totalCount = trainingSet.size();
        double sumDiff =0;
        for(HeartDiseaseCompleteData item: trainingSet){
            double known = item.getCa();
            double predicted = listAverage((kNearestNeighbors_ca(item, trainingSet, k)));
            double squaredDiff = Math.pow(predicted - known, 2);
            sumDiff += squaredDiff;
        }

        return sumDiff/(double)trainingSet.size();
    }
    public static double accuracyOfK_thal(int k, ArrayList<HeartDiseaseCompleteData> trainingSet){
        double known, predicted;
        double sumDiff =0;
        int totalCount = trainingSet.size();
        for(HeartDiseaseCompleteData item: trainingSet){
            known = item.getThal();
            predicted = listAverage((kNearestNeighbors_thal(item, trainingSet, k)));
            double squaredDiff = Math.pow(predicted - known, 2);
            sumDiff += squaredDiff;
        }

        return sumDiff/(double)trainingSet.size();
    }
    public static int bestK_ca(ArrayList<HeartDiseaseCompleteData> trainingSet){
        HashMap<Integer, Double> kmap = new HashMap<>();
        for(int k = 1; k<26; k++){
            kmap.put(k, accuracyOfK_ca(k, trainingSet));
            // System.out.println("K, MSE IN  CA: ");
            System.out.println(kmap.get(k));
        }
        int k = 1;
        for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
            if(ele.getValue()<kmap.get(k)){
                k = ele.getKey();
            }
        }
        System.out.println("CA: BEST K = " + k + " WITH MSE = " + kmap.get(k));
        return k;
    }
    public static int bestK_thal(ArrayList<HeartDiseaseCompleteData> trainingSet){
        HashMap<Integer, Double> kmap = new HashMap<>();
        for(int k = 1; k<26; k++){
            kmap.put(k, accuracyOfK_thal(k, trainingSet));
            // System.out.println("ACCURACY OF K IN THAL: ");
            System.out.println(kmap.get(k));
        }
        int k = 1;
        for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
            if(ele.getValue()<kmap.get(k)){
                k = ele.getKey();
            }
        }
        System.out.println("THAL: BEST K = " + k + " WITH MSE = " + kmap.get(k));
        return k;
    }
    /* 
    (item1.getAge() - item2.getAge());
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

*/
public static double calculateAccuracy(double[] yTrue, double[] yPred) {
    int n = yTrue.length;
    int correctPredictions = 0;

    for (int i = 0; i < n; i++) {
        if (yTrue[i] == yPred[i]) {
            correctPredictions++;
        }
        System.out.println("TRUE = " + yTrue[i] + " Predicted = " + yPred[i]);
    }

    double accuracy = (double) correctPredictions / n;
    return accuracy;
}
public static void normalFeatureSelection(ArrayList<HeartDiseaseCompleteData> alcd){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        double[][] X = new double[alcd.size()][12];
        double[] Y = new double[alcd.size()];
        int i=0;
        for(HeartDiseaseCompleteData item : alcd){
            X[i][0] = item.getAge();
            X[i][1] = item.getChol();
            X[i][2] = item.getCp();
            X[i][3] = item.getExang();
            X[i][4] = item.getFbs();
            X[i][5] = item.getThal();
            X[i][6] = item.getOldpeak();
            X[i][7] = item.getRestecg();
            X[i][8] = item.getSex(); 
            X[i][9] = item.getSlope();
            X[i][10] = item.getThalach();
            X[i][11] = item.getTrestbps();
            Y[i] = item.getNum();
            i++;
        }
        model.newSampleData(Y, X);
        double[] pValues = getPvalues(X, Y);
        double currentSolution = 0.05;
        for(int j = 0; j < pValues.length;j++ ) {
            if(pValues[j] > currentSolution) {
                // remove the j-th column from the X matrix
                double[][] temp = new double[X.length][X[0].length - 1];
                for(int m = 0; m < X.length; m++) {
                    int w = 0;
                    for(int n = 0; n < X[0].length && w<X[0].length-1; n++) {
                        if(n != j) {
                            temp[m][w] = X[m][n];
                            w++;
                        }
                    }
                }
                X = temp; // update X matrix with the new one without the j-th column
                pValues = getPvalues(X, Y);
                j = 0; // recalculate p-values
            }
        }
            
        // }
        model.newSampleData(Y, X);
        double[] beta = model.estimateRegressionParameters();
        // Get the residuals by calling estimateResiduals()
        double[] residuals = model.estimateResiduals();
        // Calculate the variance of the residuals
        double s2 = 0.0;
        for (int k = 0; k< residuals.length; k++) {
            s2 += residuals[k] * residuals[k];
        }
        s2 /= residuals.length - X[0].length;
        // Calculate the standard errors of the regression coefficients
        double[] stdErrs = new double[X[0].length];
        for (int k = 0; k < X[0].length; k++) {
            stdErrs[k] = Math.sqrt(s2 * model.calculateResidualSumOfSquares() / ((X.length - X[0].length) * model.calculateTotalSumOfSquares()));
        }
        // Get the predicted values for the input data
        double[] predicted = predict(X, Y, X);

        double accuracy = crossValidation(X,Y);
        double accuracy1 = calculateAccuracy(Y, predicted);
        System.out.println("P-value = 0.05 Accuracy = " + accuracy);
        System.out.println("P-value = 0.05 Direct Accuracy = " + accuracy1);        
    }
    public static double acceptanceProbability(double currentEnergy, double newEnergy, double temperature) {
        // If the new solution is better than the current solution, accept it
        if (newEnergy < currentEnergy) {
            return 1.0;
        }
        
        // Calculate the probability of accepting a worse solution based on the temperature and energy difference
        return Math.exp((currentEnergy - newEnergy) / temperature);
    }
    public static void printPValues(ArrayList<HeartDiseaseCompleteData> alcd){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        double[][] X = new double[alcd.size()][13];
        double[] Y = new double[alcd.size()];
        int i=0;
        for(HeartDiseaseCompleteData item : alcd){
            X[i][0] = item.getAge();
            X[i][1] = item.getSex();
            X[i][2] = item.getCp();
            X[i][3] = item.getTrestbps();
            X[i][4] = item.getChol();
            X[i][5] = item.getFbs();
            X[i][6] = item.getRestecg();
            X[i][7] = item.getThalach();
            X[i][8] = item.getExang();
            X[i][9] = item.getOldpeak();
            X[i][10] = item.getSlope();
            X[i][11] = item.getCa();
            X[i][12] = item.getThal();
            Y[i] = item.getNum();
            i++;
        }
        model.newSampleData(Y, X);
        double[] pValues = getPvalues(X, Y);
        System.out.println("\n\nP - VALUES: ");
        for(int j = 0; j<pValues.length; j++){
            System.out.println(pValues[j]);
        }
        System.out.println("\n\nEnd of P-values");
    }
    public static void doFeatureSelection(ArrayList<HeartDiseaseCompleteData> alcd){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        double[][] X = new double[alcd.size()][12];
        double[] Y = new double[alcd.size()];
        int i=0;
        for(HeartDiseaseCompleteData item : alcd){
            X[i][0] = item.getAge();
            X[i][1] = item.getChol();
            X[i][2] = item.getCp();
            X[i][3] = item.getExang();
            X[i][4] = item.getFbs();
            X[i][5] = item.getThal();
            X[i][6] = item.getOldpeak();
            X[i][7] = item.getRestecg();
            X[i][8] = item.getSex(); 
            X[i][9] = item.getSlope();
            X[i][10] = item.getThalach();
            X[i][11] = item.getTrestbps();
            Y[i] = item.getNum();
            i++;
        }
        model.newSampleData(Y, X);
        
        double[] beta = model.estimateRegressionParameters();

        // Get the residuals by calling estimateResiduals()
        double[] residuals = model.estimateResiduals();

        // Calculate the variance of the residuals
        double s2 = 0.0;
        for (int k = 0; k< residuals.length; k++) {
            s2 += residuals[k] * residuals[k];
        }
        s2 /= residuals.length - X[0].length;

        // Calculate the standard errors of the regression coefficients
        double[] stdErrs = new double[X[0].length];
        for (int k = 0; k < X[0].length; k++) {
            stdErrs[k] = Math.sqrt(s2 * model.calculateResidualSumOfSquares() / ((X.length - X[0].length) * model.calculateTotalSumOfSquares()));
        }
        // Get the predicted values for the input data
        double[] predicted = predict(X, Y, X);

        
        double[] pValues = getPvalues(X, Y);

        double lowerBound = 0.05;
        double upperBound =  0.075;
        Random random = new Random();
        double currentSolution = random.nextDouble() * (upperBound - lowerBound) + lowerBound;
        double temperature = INITIAL_TEMPERATURE;
        HashMap<Double, Double> pvalAccuracy = new HashMap<>();
        while (temperature > 1) {

                double newSolution = random.nextDouble() * (upperBound - lowerBound) + lowerBound;
                double acceptanceProbability = acceptanceProbability(currentSolution, newSolution, temperature);
                if (acceptanceProbability > random.nextDouble()) {
                    currentSolution = newSolution;
                }
                for(int j = 0; j < pValues.length;j++ ) {
                    if(pValues[j] > currentSolution) {
                        // remove the j-th column from the X matrix
                        double[][] temp = new double[X.length][X[0].length - 1];
                        for(int m = 0; m < X.length; m++) {
                            int w = 0;
                            for(int n = 0; n < X[0].length && w<X[0].length-1; n++) {
                                if(n != j) {
                                    temp[m][w] = X[m][n];
                                    w++;
                                }
                            }
                        }
                        X = temp; // update X matrix with the new one without the j-th column
                        pValues = getPvalues(X, Y); // recalculate p-values
                    }
                    
                }
                model.newSampleData(Y, X);
                beta = model.estimateRegressionParameters();
                // Get the residuals by calling estimateResiduals()
                residuals = model.estimateResiduals();
                // Calculate the variance of the residuals
                s2 = 0.0;
                for (int k = 0; k< residuals.length; k++) {
                    s2 += residuals[k] * residuals[k];
                }
                s2 /= residuals.length - X[0].length;
                // Calculate the standard errors of the regression coefficients
                stdErrs = new double[X[0].length];
                for (int k = 0; k < X[0].length; k++) {
                    stdErrs[k] = Math.sqrt(s2 * model.calculateResidualSumOfSquares() / ((X.length - X[0].length) * model.calculateTotalSumOfSquares()));
                }
                // Get the predicted values for the input data
                predicted = predict(X, Y, X);

                //FIND ACCURACY
                double accuracy = calculateAccuracy(Y,predicted);
                System.out.println("P-values = " + currentSolution  + " Accuracy = " + accuracy); 
                pvalAccuracy.put(currentSolution, accuracy);
                temperature *= 1 - COOLING_RATE;
        }
        double pval = 0;
        double accuracy = 0;
        for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
            if(entry.getValue() > accuracy){
                accuracy = entry.getValue();
                pval = entry.getKey();
            }
        }
        System.out.println("Best accuracy at p-value: "+pval +"of accuracy: " + accuracy);
        //accuracy
    }
    //DATA AUGMENTATION TECHNIQUES
    //1. ADDING RANDOM NOISE
    public HeartDiseaseCompleteData addNoise(HeartDiseaseCompleteData record, double stdDev) {
        Random rand = new Random();
        int age = (int) Math.round(record.age + rand.nextGaussian() * stdDev);
        int sex = (int) Math.round(record.sex + rand.nextGaussian() * stdDev);
        int cp = (int) Math.round(record.cp + rand.nextGaussian() * stdDev);
        int trestbps = (int) Math.round(record.trestbps + rand.nextGaussian() * stdDev);
        int chol = (int) Math.round(record.chol + rand.nextGaussian() * stdDev);
        int fbs = (int) Math.round(record.fbs + rand.nextGaussian() * stdDev);
        int restecg = (int) Math.round(record.restecg + rand.nextGaussian() * stdDev);
        int thalach = (int) Math.round(record.thalach + rand.nextGaussian() * stdDev);
        int exang = (int) Math.round(record.exang + rand.nextGaussian() * stdDev);
        double oldpeak = record.oldpeak + rand.nextGaussian() * stdDev;
        int slope = (int) Math.round(record.slope + rand.nextGaussian() * stdDev);
        int ca = (int) Math.round(record.ca + rand.nextGaussian() * stdDev);
        int thal = (int) Math.round(record.thal + rand.nextGaussian() * stdDev);
        int num = record.num;
        return new HeartDiseaseCompleteData(age, sex, cp, trestbps, chol, fbs, restecg, thalach, exang, oldpeak, slope, ca, thal, num);
    }

    //2. SCALING BY A FACTOR
    public HeartDiseaseCompleteData scale(HeartDiseaseCompleteData record, double factor) {
        int age = (int) Math.round(record.age * factor);
        int sex = (int) Math.round(record.sex * factor);
        int cp = (int) Math.round(record.cp * factor);
        int trestbps = (int) Math.round(record.trestbps * factor);
        int chol = (int) Math.round(record.chol * factor);
        int fbs = (int) Math.round(record.fbs * factor);
        int restecg = (int) Math.round(record.restecg * factor);
        int thalach = (int) Math.round(record.thalach * factor);
        int exang = (int) Math.round(record.exang * factor);
        double oldpeak = record.oldpeak * factor;
        int slope = (int) Math.round(record.slope * factor);
        int ca = (int) Math.round(record.ca * factor);
        int thal = (int) Math.round(record.thal * factor);
        int num = record.num;
        return new HeartDiseaseCompleteData(age, sex, cp, trestbps, chol, fbs, restecg, thalach, exang, oldpeak, slope, ca, thal, num);
    }

    //3. COMBINE TWO RECORDS FOR A NEW RECORD
    public HeartDiseaseCompleteData combine(HeartDiseaseCompleteData record1, HeartDiseaseCompleteData record2) {
        int age = (int)Math.max(record1.age, record2.age);
        int sex = (int)Math.max(record1.sex, record2.sex);
        int cp = (int)Math.max(record1.cp, record2.cp);
        int trestbps = (int)Math.max(record1.trestbps, record2.trestbps);
        int chol = (int)Math.max(record1.chol, record2.chol);
        int fbs = (int)Math.max(record1.fbs, record2.fbs);
        int restecg = (int)Math.max(record1.restecg, record2.restecg);
        int thalach = (int)Math.max(record1.thalach, record2.thalach);
        int exang = (int)Math.max(record1.exang, record2.exang);
        double oldpeak = (int)Math.max(record1.oldpeak, record2.oldpeak);
        int slope = (int)Math.max(record1.slope, record2.slope);
        int ca = (int)Math.max(record1.ca, record2.ca);
        int thal = (int)Math.max(record1.thal, record2.thal);
        int diagnosis = record1.num;
        return new HeartDiseaseCompleteData(age, sex, cp, trestbps, chol, fbs, restecg, thalach, exang, oldpeak, slope, ca, thal, diagnosis);
    }
    
    
    
    
    
    
}
