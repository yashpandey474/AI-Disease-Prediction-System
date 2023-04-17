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
    private static final double COOLING_RATE = 0.03;
    private static final int ITERATIONS_PER_TEMPERATURE = 100;

    public static void main(String[] args) {
        DBConnection.createDatabase();
        DBConnection.createHeartDiseaseData();
        readAndAddData();
        

        // SET OF RECORDS WHICH HAVE A MISSING VALUE FOR CA ATTRIBUTE OR THAL ATTRIBUTE OR BOTH
        ArrayList<HeartDiseaseMissingData> almd = new ArrayList<HeartDiseaseMissingData>();
        // SET OF COMPLETE RECORDS - WITH NO MISSING VALUES
        ArrayList<HeartDiseaseCompleteData> alcd = new ArrayList<HeartDiseaseCompleteData>();
        // READ THE RECORDS FROM PROCESSED.CLEVELAND FILE
        readMissingAndCompleteData(almd, alcd);
        

        // FIND THE VALUE OF K WITH HIGHEST ACCURACY FOR KNN DATA MUNGING ON CA & THAL ATTRIBUTE - BASED ON COMPLETE SET OF RECORDS
        int k1 = bestK_ca(alcd);
        int k2 = bestK_thal(alcd);

        // FIND MEAN SQUARED ERROR & SUM OF SQUARED DIFFERENCES BETWEEN KNOWN & PREDICTED VALUES WHEN REPLACING VALUES OF BARE NUCLEI WITH MEAN
        completeByMeanError(alcd);

    // COMPLETE MISSING VALUED IN ALMD BY KNN - ADDING TCOMPLETE RECORDS TO ALCD
        completeValues(almd, alcd, k1, k2);

    // ALTERNATE - COMPLETE MISSING VALUES IN ALMD BY REPLACING WITH MEAN
        completeValuesMean(alcd, almd);


            // PRINTING P -VALUES OF ATTRIBUTES
            printPValues(alcd);

        // FEATURE SELECTION BASED ON A SIGNIFICANCE LIMIT OF P-VALUE - NO SIMULATED ANNEALING
        normalFeatureSelection(alcd, 0.05); // CHECK THE ACCURACIES BY CROSS VALIDATION & DIRECT CALCULATION OF 0.05 OR O.068
        
        // FEATURE SELECTION BASED ON SIMULATED ANNEALING WITH VALUES RANGING FROM LOWER BOUND TO UPPER BOUND
        doFeatureSelection(alcd,  0.05, 0.7);

        // PLOT LEARNING CURVE - WITH TRAINING SIZE AND ACCURACY
        plotLearningCurve(alcd);

        // FIND BEST VALUE OF K FOR KNN OF DIAGNOSIS PREDICTION - ON NUMS ATTRIBUTE
        int bestK = bestKPredict(alcd);



        
    }
    public static int pluralityTest(ArrayList<Integer> neighbors){
        int count0 = 0;
        int count1 = 0;
        for (int i : neighbors) {
            if (i == 0) {
                count0++;
            } else if (i >= 1) {
                count1++;
            }
        }
        
    // Return the predicted classification based on the plurality test
            if (count1 >= count0) {
                return 1;
            } 
            return 0;
    }
    public static int PredictdistanceMetric(HeartDiseaseCompleteData item1, HeartDiseaseCompleteData item2){
        //ASSUMING ITEM1 HAS THE MISSING DATA IN 6TH INDEX
        int diff = 0;
        diff += abs(item1.getAge() - item2.getAge());
        diff += abs(item1.getChol() - item2.getChol());
        diff += abs(item1.getCp() - item2.getCp());
        diff += abs(item1.getExang()- item2.getExang());
        diff += abs(item1.getFbs() - item2.getFbs());
        diff += abs(item1.getOldpeak() - item2.getOldpeak());
        diff += abs(item1.getRestecg() - item2.getRestecg());
        diff += abs(item1.getThal() - item2.getThal());
        diff += abs(item1.getCa() - item2.getCa());
        diff += abs(item1.getSex() - item2.getSex());
        diff += abs(item1.getSlope() - item2.getSlope());
        diff += abs(item1.getThalach() - item2.getThalach());
        diff += abs(item1.getTrestbps() - item2.getTrestbps());
        return diff;
    }
    public static int PredictkNearestNeighbors(HeartDiseaseCompleteData item1, ArrayList<HeartDiseaseCompleteData> items, int k){
        ArrayList<Integer> neighbors = new ArrayList<>(); //CLASSIFICATION STORED
        HashMap<Integer, Integer> classfDist = new HashMap<>(); //DISTANCE->INDEX IN NEIGHBORS
        int distance = 0;

        for(HeartDiseaseCompleteData item: items){
            // System.out.println("In Knn" + items.size());
            //PROBLEM: NEIGHBORS REMOVES FIRST OCCURENCE INSTEAD OF THAT ITEM WITH LARGEST INDEX: SOLN = STORE INDEX, NOT CLASSIFICATION
            distance = PredictdistanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getNum());
                classfDist.put(distance, item.getNum());
            }    
            else{ 
                if(!classfDist.keySet().isEmpty() && Collections.max(classfDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getNum());
                    neighbors.remove(neighbors.indexOf(classfDist.get(Collections.max(classfDist.keySet()))));
                    classfDist.put(distance, item.getNum());
                    classfDist.remove(Collections.max(classfDist.keySet()));
                }
            }
        
        }

        //PLURALITY TEST
        return pluralityTest(neighbors);


    }
    public static double accuracyOfKPredict(int k, ArrayList<HeartDiseaseCompleteData> alcd){
        int correctPredictions =0;
        int totalCount = alcd.size();
        for(HeartDiseaseCompleteData item: alcd){
            //Actual value of bare nuclie
            int known = item.getNum();
            //Predicted value using passed k
            int predicted = PredictkNearestNeighbors(item, alcd, k);
            //Add squared difference
            if (known == 0 && predicted == 0 || known >= 1 && predicted >= 1){
                correctPredictions++;
            }
            
        }
        // System.out.println("SIZE IN K = " + totalCount);
        return (correctPredictions/(double)totalCount)*100;
    }
    //DOCUMENTED//
    public static int bestKPredict(ArrayList<HeartDiseaseCompleteData> alcd){
        System.out.println("NO OF RECORDS = " + alcd.size());
        HashMap<Integer, Double> kmap = new HashMap<>();
        System.out.println("\n\n\n ACCURACIES OF KNN \n\n");
        for(int k = 2; k<30; k++){
            kmap.put(k, accuracyOfKPredict(k, alcd));
            System.out.println( kmap.get(k));
        }
    
        int bestK = 2;
        for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
            if(ele.getValue() > kmap.get(bestK)){
                bestK = ele.getKey();
            }
        }
        return bestK;
    }
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
//DOCUMENTED//
public static double calculateAccuracy(double[] yTrue, double[] yPred) {
    int n = yTrue.length;
    int correctPredictions = 0;

    for (int i = 0; i < n; i++) {
        if((yTrue[i] == 0 && yPred[i] == 0) || (yTrue[i] >=1 && yPred[i] == 1)){
            correctPredictions ++;

        }
        // System.out.println("TRUE = " + (yTrue[i]>=1?1:0) + " Predicted = " + yPred[i]);
    }

    double accuracy = (double) correctPredictions / n;
    return accuracy;
}
//DOCUMENTED//
public static void plotLearningCurve(ArrayList<HeartDiseaseCompleteData> alcd){
    OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
    double[][] X = new double[alcd.size()][12];
    double[] Y = new double[alcd.size()];
    int i1=0;
    for(HeartDiseaseCompleteData item : alcd){
        X[i1][0] = item.getAge();
        X[i1][1] = item.getChol();
        X[i1][2] = item.getCp();
        X[i1][3] = item.getExang();
        X[i1][4] = item.getFbs();
        X[i1][5] = item.getThal();
        X[i1][6] = item.getOldpeak();
        X[i1][7] = item.getRestecg();
        X[i1][8] = item.getSex(); 
        X[i1][9] = item.getSlope();
        X[i1][10] = item.getThalach();
        X[i1][11] = item.getTrestbps();
        Y[i1] = item.getNum();
        i1++;
    }
    model.newSampleData(Y, X);
    // Define the range of the training set sizes to be tested

    int[] trainSizes = {20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170,
    180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300};
    System.out.println("LENGTH OF X = " + X.length);
        // Define arrays to store the accuracy and training set sizes
        double[] accuracy = new double[trainSizes.length];
        double[] trainSetSizes = new double[trainSizes.length];

        /* REMOVED SHUFFLING AS RESULTS WERE VARIED */
        // shuffle(X, Y);
        // Loop over the training set sizes
        for (int i = 0; i < trainSizes.length; i++) {
            int trainSize = trainSizes[i];
            trainSetSizes[i] = trainSize;
            double[][] X_train = new double[trainSize][X[0].length];
            double[][] X_test = new double[X.length - trainSize][X[0].length];
            double[] Y_train = new double[trainSize];
            double[] Y_test = new double[X.length - trainSize];
            int trainIdx = 0;
            int testIdx = 0;

            // Split the data into training and testing sets
            for (int j = 0; j < X.length; j++) {
                if (j < trainSize) {
                    // add data point to training set
                    X_train[trainIdx] = X[j];
                    Y_train[trainIdx] = Y[j];
                    trainIdx++;
                } else {
                    // add data point to testing set
                    X_test[testIdx] = X[j];
                    Y_test[testIdx] = Y[j];
                    testIdx++;
                }
            }

            // Train model on training set  
            model.newSampleData(Y_train, X_train);
            double[] beta = model.estimateRegressionParameters();
            double[] residuals = model.estimateResiduals();

            // Calculate the variance of the residuals
            double s2 = 0.0;
            for (int k1 = 0; k1 < residuals.length; k1++) {
                s2 += residuals[k1] * residuals[k1];
            }
            s2 /= residuals.length - X[0].length;

            // Calculate the standard errors of the regression coefficients
            double[] stdErrs = new double[X[0].length];
            for (int k1 = 0; k1 < X[0].length; k1++) {
                stdErrs[k1] = Math.sqrt(s2 * model.calculateResidualSumOfSquares() / ((X.length - X[0].length) * model.calculateTotalSumOfSquares()));
            }

            // Test model on testing set
            double[] predicted = predict(X_train, Y_train, X_test);

            for(int i2 = 0; i2<predicted.length; i2++){
                if (predicted[i2]<0.5){
                    predicted[i2] = 0;
                }
                else if(predicted[i2]>= 0.5){
                    predicted[i2] = 1;
                }
                
            }
            // Calculate accuracy of model on testing set
            accuracy[i] = calculateAccuracy(Y_test, predicted);
        }

        // Plot the learning curve
        for(int i = 0; i<trainSizes.length; i++){
            System.out.println( accuracy[i]);
        }
    }
//DOCUMENTED//
public static void normalFeatureSelection(ArrayList<HeartDiseaseCompleteData> alcd, double limit){
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
        double currentSolution = limit;
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
                j = 0; // reevaluate p-values
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
        for(int i1 = 0; i1<predicted.length; i1++){
            if (predicted[i1]<0.5){
                predicted[i1] = 0;
            }
            else if(predicted[i1]>= 0.5){
                predicted[i1] = 1;
            }
            
        }
        double[] temp = new double[alcd.size()];
        for(int i1 = 0; i1<alcd.size(); i1++){
            temp[i1] = Y[i1];
        }
        double accuracy = crossValidation(X,Y);
        double accuracy1 = calculateAccuracy(temp, predicted);
        System.out.println("P-value = 0.05 Accuracy = " + accuracy);
        System.out.println("P-value = 0.05 Direct Accuracy = " + accuracy1);        
    }
    //DOCUMENTED//
    public static double acceptanceProbability(double currentEnergy, double newEnergy, double temperature) {
        // If the new solution is better than the current solution, accept it
        if (newEnergy < currentEnergy) {
            return 1.0;
        }
        
        // Calculate the probability of accepting a worse solution based on the temperature and energy difference
        return Math.exp((currentEnergy-newEnergy) / temperature);
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
    public static void doFeatureSelection(ArrayList<HeartDiseaseCompleteData> alcd, double lowerBoundValue, double upperBoundValue){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        double[][] X = new double[alcd.size()][12];
        double[][] X1 = new double[alcd.size()][12]; /* COPY OF X FOR REINITIALISATION AFTER EACH ITERATION */
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
            
            X1[i][0] = item.getAge();
            X1[i][1] = item.getChol();
            X1[i][2] = item.getCp();
            X1[i][3] = item.getExang();
            X1[i][4] = item.getFbs();
            X1[i][5] = item.getThal();
            X1[i][6] = item.getOldpeak();
            X1[i][7] = item.getRestecg();
            X1[i][8] = item.getSex(); 
            X1[i][9] = item.getSlope();
            X1[i][10] = item.getThalach();
            X1[i][11] = item.getTrestbps();

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

        double lowerBound = lowerBoundValue;
        double upperBound =  upperBoundValue;
        
        Random random = new Random();
        double currentSolution = upperBoundValue;
        double temperature = INITIAL_TEMPERATURE;
        HashMap<Double, Double> pvalAccuracy = new HashMap<>();
        while (temperature > 1) {

                double newSolution = random.nextDouble() * (upperBound - lowerBound) + lowerBound;
                double acceptanceProbability = acceptanceProbability(currentSolution, newSolution, temperature);
                if (acceptanceProbability > random.nextDouble()) {
                    currentSolution = newSolution;
                }
                int e = 0;
                for(int j = 0; j<pValues.length;j++ ) {
                    if(pValues[j]>currentSolution) {
                        // System.out.println("ATTRIBUTE ELIMINATED " + pValues[j]);
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
                        j=0;
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
                for(int i1 = 0; i1<predicted.length; i1++){
                    if (predicted[i1]<0.5){
                        predicted[i1] = 0;
                    }
                    else if(predicted[i1]>= 0.5){
                        predicted[i1] = 1;
                    }
                    
                }
                //FIND ACCURACY
                double accuracy = calculateAccuracy(Y,predicted);
                // System.out.println("P-value = " + currentSolution  + " Accuracy = " + accuracy); 
                pvalAccuracy.put(currentSolution, accuracy);
                temperature *= 1 - COOLING_RATE;
                X = X1;
                pValues  = getPvalues(X, Y);
        }
        double pval = 0;
        double accuracy = 0;
        System.out.println("\n\n\n P-VALUES USED \n\n\n");
        for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
            System.out.println(entry.getKey());
        }
        System.out.println("\n\n\n ACCURACIES OBTAINED \n\n\n");
        for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
            System.out.println(entry.getValue());
        }
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
