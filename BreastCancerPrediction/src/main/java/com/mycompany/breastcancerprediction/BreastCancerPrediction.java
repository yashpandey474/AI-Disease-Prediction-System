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
import java.lang.reflect.Array;
import java.io.*;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ode.nonstiff.AdamsNordsieckTransformer;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;


public class BreastCancerPrediction {
    private static final double INITIAL_TEMPERATURE = 1000;
    private static final double COOLING_RATE = 0.03;
    private static final int ITERATIONS_PER_TEMPERATURE = 100;

    public static void main(String[] args) {
        
        DBConnection.createDatabase();
        DBConnection.createBreastCancerData();
        readAndAddData();
        


     // FORMING THE SET OF RECORDS WITH MISSING DATA IN BARE NUCLEI FIELD
        ArrayList<BreastCancerMissingData> almd = new ArrayList<BreastCancerMissingData>();
    // FORMING THE SET OF RECORDS WITH COMPLETE DATA - NO MISSING VALUES
        ArrayList<BreastCancerCompleteData> alcd = new ArrayList<BreastCancerCompleteData>();
    // READING DATA FROM BREAST-CANCER-WINSCONCIN FILE
        readMissingAndCompleteData(almd, alcd);

    // FINDING BEST VALUE OF K FOR KNN - DATA MUNGING OF BARE NUCLEI FIELD 
        int k = bestK(alcd);
        
    // FIND MEAN SQUARED ERROR & SUM OF SQUARED DIFFERENCES BETWEEN KNOWN & PREDICTED VALUES WHEN REPLACING VALUES OF BARE NUCLEI WITH MEAN
        completeByMeanError(alcd);

    // COMPLETE MISSING VALUED IN ALMD BY KNN - ADDING TCOMPLETE RECORDS TO ALCD
        completeValues(almd, alcd, k);

    // ALTERNATE - COMPLETE MISSING VALUES IN ALMD BY REPLACING WITH MEAN
        completeValuesMean(alcd, almd);

    // PRINTING P -VALUES OF ATTRIBUTES
        printPValues(alcd);

    // FEATURE SELECTION BASED ON A P-VALUE SIGNIGFICANCE LIMIT [NO SIMULATED ANNEALING. USED TO TEST ACCURACY FOR 0.05 & 0.005 SEPARATELY]
        normalFeatureSelection(alcd, 0.05);

    // FEATURE SELECTION USING SIMULATED ANNEALING TO FIND P-VALUE WITH BEST ACCURACY IN A GIVEN RANGE
        doFeatureSelection(alcd, 0.001, 0.3);
    
    // LEARNING CURVE - VALUES OF ACCURACIES FOR DIFFERENT TRAINING SIZES - USE TO PLOT GRAPH
        plotLearningCurve(alcd);

    // FIND BEST VALUE OF K FOR KNN OF DIAGNOSIS PREDICTION - ON NUMS ATTRIBUTE
        int bestK = bestKPredict(alcd);

    

    
    }
    public static int listAverage(ArrayList<Integer> arr){
        int sum = 0;
        for(Integer i: arr){
            sum+=i;
        }
        if(arr.isEmpty()){
            //-1 indictes the array passed is empty
            return -1;
        }
        return sum/arr.size();
    }
    public static void completeValues(ArrayList<BreastCancerMissingData> testing, ArrayList<BreastCancerCompleteData> training, int k){
         ArrayList<BreastCancerCompleteData> replica = training;
        for(BreastCancerMissingData item: testing){
            int predicted = listAverage(kNearestNeighbors(item, replica, k));
            item.setBareNuclei(predicted);
            BreastCancerCompleteData item1 = new BreastCancerCompleteData(item.codeNumber, item.clumpThickness, item.cSizeUni, item.cShapeUni, item.mAdhesion, item.secs, item.bareNuclei, 
            item.blandChromatin,  item.normalNucleoli, item.mitoses, item.classification);
            training.add(item1);
        }
    }
    public static void readAndAddData(){
        Scanner sc = null;
        try{
            sc = new Scanner(new FileInputStream("C:/AIprojectRep/BreastCancerPrediction/src/main/java/com/mycompany/breastcancerprediction/breast-cancer-wisconsin.txt"));
        }
        catch (FileNotFoundException e){
            System.out.println("File breast-cancer-wisconsin.txt was not found");
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
    //DOCUMENTED//
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

    //DOCUMENTED//
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

    //DOCUMENTED//
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
            //If first k items have not been added
            if(neighbors.size()<k && distance!=0){
                //Add bare nuclei value to neighbors
                neighbors.add(item.getBareNuclei());
                //Add distance and bare nuclei value to hashmap
                nuclieDist.put(distance, item.getBareNuclei());
            }    
            else{
                //If already k items added and distance is less than max added distance 
                if(!nuclieDist.keySet().isEmpty() && Collections.max(nuclieDist.keySet())>distance && distance!=0){                    
                    //Add to neighbors
                    neighbors.add(item.getBareNuclei());
                    //Remove bare nuclei value with maximum distance
                    neighbors.remove(neighbors.indexOf(nuclieDist.get(Collections.max(nuclieDist.keySet()))));
                    //Remove the one with maxmimum distance and add new one
                    nuclieDist.put(distance, item.getBareNuclei());
                    nuclieDist.remove(Collections.max(nuclieDist.keySet()));
                }
            }
        
        }

        return neighbors;
    }
    public static int PredictdistanceMetric(BreastCancerCompleteData item1, BreastCancerCompleteData item2){
        //ASSUMING ITEM1 HAS THE MISSING DATA IN 6TH INDEX
        int diff = 0;
        diff += abs(item1.getBlandChromatin() - item2.getBlandChromatin());
        diff += abs(item1.getClumpThickness() - item2.getClumpThickness());
        diff += abs(item1.getMitoses()- item2.getMitoses());
        diff += abs(item1.getNormalNucleoli() - item2.getNormalNucleoli());
        diff += abs(item1.getSecs() - item2.getSecs());
        diff += abs(item1.getcShapeUni() - item2.getcShapeUni());
        diff += abs(item1.getcSizeUni() - item2.getcSizeUni());
        diff += abs(item1.getmAdhesion() - item2.getmAdhesion());
        diff += abs(item1.getBareNuclei() - item2.getBareNuclei());
        return diff;
    }

    private static int abs(int i) {
        if(i>0){
            return i;
        }    
        return -i;
    }
    public static int pluralityTest(ArrayList<Integer> neighbors){
        int count2 = 0;
        int count4 = 0;
        for (int i : neighbors) {
            if (i == 2) {
                count2++;
            } else if (i == 4) {
                count4++;
            }
        }
        
    // Return the predicted classification based on the plurality test
            if (count2 > count4) {
                return 2;
            } 
            return 4;
    }
    public static int PredictkNearestNeighbors(BreastCancerCompleteData item1, ArrayList<BreastCancerCompleteData> items, int k){
        ArrayList<Integer> neighbors = new ArrayList<>(); //CLASSIFICATION STORED
        HashMap<Integer, Integer> classfDist = new HashMap<>(); //DISTANCE->INDEX IN NEIGHBORS
        int distance = 0;

        for(BreastCancerCompleteData item: items){
            // System.out.println("In Knn" + items.size());
            //PROBLEM: NEIGHBORS REMOVES FIRST OCCURENCE INSTEAD OF THAT ITEM WITH LARGEST INDEX: SOLN = STORE INDEX, NOT CLASSIFICATION
            distance = PredictdistanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getClassification());
                classfDist.put(distance, item.getClassification());
            }    
            else{ 
                if(!classfDist.keySet().isEmpty() && Collections.max(classfDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getClassification());
                    neighbors.remove(neighbors.indexOf(classfDist.get(Collections.max(classfDist.keySet()))));
                    classfDist.put(distance, item.getClassification());
                    classfDist.remove(Collections.max(classfDist.keySet()));
                }
            }
        
        }

        //PLURALITY TEST
        return pluralityTest(neighbors);


    }
    public static double accuracyOfKPredict(int k, ArrayList<BreastCancerCompleteData> alcd){
        int sum =0;
        int totalCount = alcd.size();
        for(BreastCancerCompleteData item: alcd){
            //Actual value of bare nuclie
            int known = item.getClassification();
            //Predicted value using passed k
            int predicted = PredictkNearestNeighbors(item, alcd, k);
            //Add squared difference
            if (known == predicted){
                sum++;
            }
            
        }
        // System.out.println("SIZE IN K = " + totalCount);
        return (sum/(double)totalCount)*100;
    }
    //DOCUMENTED//
    public static int bestKPredict(ArrayList<BreastCancerCompleteData> alcd){
        System.out.println("NO OF RECORDS = " + alcd.size());
        HashMap<Integer, Double> kmap = new HashMap<>();
        for(int k = 2; k<30; k++){
            kmap.put(k, accuracyOfKPredict(k, alcd));
            System.out.println(kmap.get(k));
        }
    
        int bestK = 2;
        for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
            if(ele.getValue() > kmap.get(bestK)){
                bestK = ele.getKey();
            }
        }
        System.out.println("BEST: K = " + bestK + "ACCURACY = " + kmap.get(bestK));
        return bestK;

    }

    public static void completeValuesMean(ArrayList<BreastCancerCompleteData> alcd, ArrayList<BreastCancerMissingData> almd){
        //REPLACING VALUES BY MEAN/MODE
        int sum = 0;
        int n = alcd.size();

        for(BreastCancerCompleteData cd: alcd){
            sum += cd.getBareNuclei();
        }
        sum = sum/n;
        for(BreastCancerMissingData item: almd){
            item.bareNuclei = sum;
            alcd.add(new BreastCancerCompleteData(item.codeNumber, item.clumpThickness, item.cSizeUni, item.cShapeUni, item.mAdhesion, item.secs, item.bareNuclei, 
            item.blandChromatin,  item.normalNucleoli, item.mitoses, item.classification));
        }
    }
    
    public static double percent(int a, int b){
        return (a/b)*100;
    }
    
    // public static ArrayList<BreastCancerCompleteData> cloneList(ArrayList<BreastCancerCompleteData> arr){
    //     ArrayList<BreastCancerCompleteData> replica = new ArrayList<>();
    //     for(BreastCancerCompleteData item: arr){
    //         replica.add(item);
    //     }
    //     return replica;
    // }

    //DOCUMENTED//
    public static double accuracyOfK(int k, ArrayList<BreastCancerCompleteData> trainingSet){
        //Store sum of differences between predicted & known
        int sumDiff =0;
        int totalCount = trainingSet.size();
        for(BreastCancerCompleteData item: trainingSet){
            //Actual value of bare nuclie
            int known = item.getBareNuclei();
            //Predicted value using passed k
            int predicted = listAverage(kNearestNeighbors(item, trainingSet, k));
            //Add squared difference
            double squaredDiff = Math.pow(predicted - known, 2);
            sumDiff += squaredDiff;
        }
        // System.out.println("SIZE IN K = " + totalCount);
        return sumDiff/(double)totalCount;
    }
    //DOCUMENTED//
    public static int bestK(ArrayList<BreastCancerCompleteData> trainingSet){
        HashMap<Integer, Double> kmap = new HashMap<>();
        for(int k = 1; k<26; k++){
            kmap.put(k, accuracyOfK(k, trainingSet));
            System.out.println(kmap.get(k));
        }
    
        int bestK = 1;
        for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
            if(ele.getValue() < kmap.get(bestK)){
                bestK = ele.getKey();
            }
        }
        return bestK;
    }
    //DOCUMENTED//
    public static void completeByMeanError(ArrayList<BreastCancerCompleteData> alcd){
            int n = alcd.size();
            double mean = 0;
            for (BreastCancerCompleteData cd : alcd) {
                mean += cd.getBareNuclei();
            }
            mean /= alcd.size();
            double sumSquares = 0;
            for (BreastCancerCompleteData cd : alcd) {
                int known = cd.getBareNuclei();
                double predicted = mean;
                double squaredDiff = Math.pow(predicted - known, 2);
                sumSquares += squaredDiff;
            }
            System.out.println("BY MEAN, DIFFERENCE = :" + sumSquares);
            System.out.println("BY MEAN, MSE = " + sumSquares/(double)alcd.size());
        
    }
    public static double meanSquareError(double[] Y, double[] Predicted){
        double sumSquaredError = 0.0;
        int n = Y.length;

        for (int i = 0; i < n; i++) {
            double error = Y[i] - Predicted[i];
            sumSquaredError += error * error;
        }

        double mse = sumSquaredError / n;
        return mse;
    }

    public static double calculateAccuracy(double[] yTrue, double[] yPred) {
        int n = yTrue.length;
        int correctPredictions = 0;

        for (int i = 0; i < n; i++) {
            if (yTrue[i] == yPred[i]) {
                correctPredictions++;
            }
        }

        double accuracy = (double) correctPredictions/n;
        return accuracy;
    }
    public static void shuffle(double[] array1, double[] array2) {
        Random rnd = new Random();
        for (int i = array1.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Swap array1 values at i and index
            double temp = array1[i];
            array1[i] = array1[index];
            array1[index] = temp;
            // Swap array2 values at i and index
            temp = array2[i];
            array2[i] = array2[index];
            array2[index] = temp;
        }
    }    
    public static void plotLearningCurve(ArrayList<BreastCancerCompleteData> alcd){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        double[][] X = new double[alcd.size()][9];
        double[] Y = new double[alcd.size()];
        int i1=0;
        for(BreastCancerCompleteData item : alcd){
            X[i1][0] = item.getClumpThickness();
            X[i1][1] = item.getcSizeUni();
            X[i1][2] = item.getcShapeUni();
            X[i1][3] = item.getmAdhesion();
            X[i1][4] = item.getSecs();
            X[i1][5] = item.getBareNuclei();
            X[i1][6] = item.blandChromatin;
            X[i1][7] = item.getNormalNucleoli();
            X[i1][8] = item.getMitoses(); 
            Y[i1] = item.getClassification();
            i1++;
        }
        model.newSampleData(Y, X);
        // Define the range of the training set sizes to be tested
    
        int[] trainSizes = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 120, 130, 170, 200, 240, 280, 320, 360, 400, 450, 500, 550, 600, 650, 680};
        System.out.println("LENGTH OF X = " + X.length);
            // Define arrays to store the accuracy and training set sizes
            double[] accuracy = new double[trainSizes.length];
            double[] trainSetSizes = new double[trainSizes.length];
            shuffle(X, Y);
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

                for (int j = 0; j < predicted.length; j++) {
                    if (predicted[j] > 3) {
                        predicted[j] = 4;
                    } else {
                        predicted[j] = 2;
                    }
                }

                // Calculate accuracy of model on testing set
                accuracy[i] = calculateAccuracy(Y_test, predicted);
            }

            // Plot the learning curve
            for(int i = 0; i<trainSizes.length; i++){
                System.out.println("TRAINING SET SIZE = " + trainSizes[i] + " ACCURACY = " + accuracy[i]);
            }

    }
    public static void LearningCurveKnnCrossValidation(ArrayList<BreastCancerCompleteData> alcd){
        //POPULATE X & Y ARRAYS
        //X-> ATTRIBUTES AND VALUES
        //Y-> CLASSIFICATIONS
         double[][] X = new double[alcd.size()][9];
            double[] Y = new double[alcd.size()];
            int i1=0;
            for(BreastCancerCompleteData item : alcd){
                X[i1][0] = item.getClumpThickness();
                X[i1][1] = item.getcSizeUni();
                X[i1][2] = item.getcShapeUni();
                X[i1][3] = item.getmAdhesion();
                X[i1][4] = item.getSecs();
                X[i1][5] = item.getBareNuclei();
                X[i1][6] = item.blandChromatin;
                X[i1][7] = item.getNormalNucleoli();
                X[i1][8] = item.getMitoses(); 
                Y[i1] = item.getClassification();
                i1++;
            }
            int[] trainSizes = {15, 25, 35, 45, 65, 75, 85, 95, 100, 120, 130, 170, 200, 240, 280, 320, 360, 400, 450, 500, 550, 600, 650, 680};
        System.out.println("LENGTH OF X = " + X.length);
            // Define arrays to store the accuracy and training set sizes
            double[] accuracy = new double[trainSizes.length];
            double[] trainSetSizes = new double[trainSizes.length];
            shuffle(X, Y);
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

            

            //TRAINING IN KNN === FINDING OPTIMAL K
            //TESTING IN KNN === PREDICTING VALUES USING OPTIMAL K

            //CONVERTING X_tEST AND Y_TEST TO AN ARRAYLIST
            ArrayList<BreastCancerCompleteData> testSet = new ArrayList<>();
            ArrayList<BreastCancerCompleteData> trainSet = new ArrayList<>();
            int j = 0;
            for (int i2 = 0; i2 < X_train.length; i2++) {
                BreastCancerCompleteData dataPoint = new BreastCancerCompleteData(j++,
                (int) X_train[i2][0], (int) X_train[i2][1], (int) X_train[i2][2],
                (int) X_train[i2][3], (int) X_train[i2][4], (int) X_train[i2][5],
                (int) X_train[i2][6], (int) X_train[i2][7], (int) X_train[i2][8],
                (int) Y_train[i2]
                );
                trainSet.add(dataPoint);
            }
            for (int i2 = 0; i2 < X_test.length; i2++) {
                BreastCancerCompleteData dataPoint = new BreastCancerCompleteData(j++,
                (int) X_test[i2][0], (int) X_test[i2][1], (int) X_test[i2][2],
                (int) X_test[i2][3], (int) X_test[i2][4], (int) X_test[i2][5],
                (int) X_test[i2][6], (int) X_test[i2][7], (int) X_test[i2][8],
                (int) Y_test[i2]
                );
                testSet.add(dataPoint);
            }
            
            int k1 = bestKPredict(trainSet);
            System.out.println("OPTIMAL K FOUND = " + k1);
            accuracy[i] = accuracyOfKPredict(k1, testSet);
            System.out.println("TEST ACCURACY = " + accuracy[i]);
        }
        System.out.println("\n\nFINAL ACCURACIES \n\n");
        for(int i = 0; i<trainSizes.length; i++){
            System.out.println(accuracy[i]);
        }
       
            
    }
    public static void knnCrossValidation(ArrayList<BreastCancerCompleteData> alcd){
        //POPULATE X & Y ARRAYS
        //X-> ATTRIBUTES AND VALUES
        //Y-> CLASSIFICATIONS
         double[][] X = new double[alcd.size()][9];
            double[] Y = new double[alcd.size()];
            int i1=0;
            for(BreastCancerCompleteData item : alcd){
                X[i1][0] = item.getClumpThickness();
                X[i1][1] = item.getcSizeUni();
                X[i1][2] = item.getcShapeUni();
                X[i1][3] = item.getmAdhesion();
                X[i1][4] = item.getSecs();
                X[i1][5] = item.getBareNuclei();
                X[i1][6] = item.blandChromatin;
                X[i1][7] = item.getNormalNucleoli();
                X[i1][8] = item.getMitoses(); 
                Y[i1] = item.getClassification();
                i1++;
            }
            int k = 5; // number of folds
            int n = X.length; // size of data set
            int foldSize = n / k; // size of each fold
            double[] accuracy = new double[k]; // array to store accuracy of each fold

            // shuffle the data set randomly
            shuffle(X, Y);

            // GENERATE THE TRAINING AND TESTING SETS
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
            

            //TRAINING IN KNN === FINDING OPTIMAL K
            //TESTING IN KNN === PREDICTING VALUES USING OPTIMAL K

            //CONVERTING X_tEST AND Y_TEST TO AN ARRAYLIST
            ArrayList<BreastCancerCompleteData> testSet = new ArrayList<>();
            ArrayList<BreastCancerCompleteData> trainSet = new ArrayList<>();
            int j = 0;
            for (int i2 = 0; i2 < X_train.length; i2++) {
                BreastCancerCompleteData dataPoint = new BreastCancerCompleteData(j++,
                (int) X_train[i2][0], (int) X_train[i2][1], (int) X_train[i2][2],
                (int) X_train[i2][3], (int) X_train[i2][4], (int) X_train[i2][5],
                (int) X_train[i2][6], (int) X_train[i2][7], (int) X_train[i2][8],
                (int) Y_train[i2]
                );
                trainSet.add(dataPoint);
            }
            for (int i2 = 0; i2 < X_test.length; i2++) {
                BreastCancerCompleteData dataPoint = new BreastCancerCompleteData(j++,
                (int) X_test[i2][0], (int) X_test[i2][1], (int) X_test[i2][2],
                (int) X_test[i2][3], (int) X_test[i2][4], (int) X_test[i2][5],
                (int) X_test[i2][6], (int) X_test[i2][7], (int) X_test[i2][8],
                (int) Y_test[i2]
                );
                testSet.add(dataPoint);
            }
            
            int k1 = bestKPredict(trainSet);
            System.out.println("OPTIMAL K FOUND = " + k1);
            accuracy[i] = accuracyOfKPredict(k1, testSet);
            System.out.println("TEST ACCURACY = " + accuracy[i]);
        }
        double sum = 0;
            for (int i = 0; i < k; i++) {
                sum += accuracy[i];
            }
            double averageAccuracy = sum / k;
            System.out.println("Average accuracy: " + averageAccuracy);
            // return averageAccuracy
            
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
                    if (predicted[j] > 3) {
                        predicted[j] = 4;
                    } else {
                        predicted[j] = 2;
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
    public static void printPValues(ArrayList<BreastCancerCompleteData> alcd){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
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
        model.newSampleData(Y, X);
        double[] pValues = getPvalues(X, Y);
        System.out.println("\n\nP - VALUES: ");
        for(int j = 0; j<pValues.length; j++){
            System.out.println(pValues[j]);
        }
        System.out.println("\n\nEnd of P-values");
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
    //DOCUMENTED//
    public static void normalFeatureSelection(ArrayList<BreastCancerCompleteData> alcd, double limit){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
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
        model.newSampleData(Y, X);
        double[] pValues = getPvalues(X, Y);
        double currentSolution = limit;
        for(int j = 0; j < pValues.length;j++ ) {
            if(pValues[j] > currentSolution) {
                System.out.println("ATTRIBUTE ELIMINATED "+ pValues[j]);
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
        
        //FIND ACCURACY
        model.newSampleData(Y, X);
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
                double[] predicted = predict(X, Y, X);
                for (int j = 0; j < predicted.length; j++) {
                    if (predicted[j] > 3) {
                        predicted[j] = 4;
                    } else {
                        predicted[j] = 2;
                    }
                }
        double[] temp = new double[alcd.size()];
        for(int i1 = 0; i1<alcd.size(); i1++){
            temp[i1] = Y[i1];
        }
        double accuracy = crossValidation(X,Y);
        double accuracy1 = calculateAccuracy(temp, predicted);
        System.out.println("P-value = " + limit+ " Accuracy = " + accuracy);
        System.out.println("P-value = " + limit + " Direct Accuracy = " + accuracy1);        
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
    //DOCUMENTED//
    public static void doFeatureSelection(ArrayList<BreastCancerCompleteData> alcd, double lowerBoundValue, double upperBoundValue){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        double[][] X = new double[alcd.size()][9];
        double[][] X1 = new double[alcd.size()][9];
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


            X1[i][0] = item.getClumpThickness();
            X1[i][1] = item.getcSizeUni();
            X1[i][2] = item.getcShapeUni();
            X1[i][3] = item.getmAdhesion();
            X1[i][4] = item.getSecs();
            X1[i][5] = item.getBareNuclei();
            X1[i][6] = item.blandChromatin;
            X1[i][7] = item.getNormalNucleoli();
            X1[i][8] = item.getMitoses(); 
            Y[i] = item.getClassification();
            i++;
        }
    
        model.newSampleData(Y, X);
        double[] pValues = getPvalues(X, Y);
        System.out.println("P-VALUES");
        for(int j = 0; j<pValues.length; j++){
            System.out.println(pValues[j]);
        }
        System.out.println("END OF P-VALUES");
        double lowerBound = lowerBoundValue;
        double upperBound =  upperBoundValue;
        Random random = new Random();
        double currentSolution = upperBoundValue;
        double temperature = INITIAL_TEMPERATURE;
        HashMap<Double, Double> pvalAccuracy = new HashMap<>();
        while (temperature > 1) {
            
            // generate a new solution within the bounds
            double newSolution = random.nextDouble() * (upperBound - lowerBound) + lowerBound;
                double acceptanceProbability = acceptanceProbability(currentSolution, newSolution, temperature);
                if (acceptanceProbability > random.nextDouble()) {
                    currentSolution = newSolution;
                }
                for(int j = 0; j<pValues.length;j++ ) {
                    if(pValues[j]>currentSolution) {
                        System.out.println("ATTRIBUTE ELIMINATED " + pValues[j]);
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
                for (int j = 0; j < predicted.length; j++) {
                    if (predicted[j] >= 3) {
                        predicted[j] = 4;
                    } else {
                        predicted[j] = 2;
                    }
                }
                //FIND ACCURACY
                double accuracy = calculateAccuracy(Y,predicted);
                System.out.println("P-value = " + currentSolution  + " Accuracy = " + accuracy); 
                pvalAccuracy.put(currentSolution, accuracy);
                temperature *= 1 - COOLING_RATE;
                X = X1;
                System.out.println("LENGTH = " + X1[0].length);
                pValues  = getPvalues(X, Y);
        }
        double pval = 0;
        double accuracy = 0;
        System.out.println("\n\n P-VALUES \n\n");
        for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
            System.out.println(entry.getKey());
        }
        System.out.println("\n\n ACCURACIES \n\n");
        for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
            System.out.println(entry.getValue());
        }
        System.out.println("END OF ACCURACIES");
        for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
            if(entry.getValue() > accuracy){
                accuracy = entry.getValue();
                pval = entry.getKey();
            }
        }
        System.out.println("Best accuracy at p-value: "+pval +"of accuracy: " + accuracy);
    
    }

    //DOCUMENTED//
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
    //DOCUMENTED//
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


    //DOCUMENTED//
    //DATA AUGMENTATION TECHNIQUES
    //1. ADDING RANDOM NOISE
    public static BreastCancerCompleteData addNoise(BreastCancerCompleteData record, double stdDev) {
        Random rand = new Random();
        int cn = record.codeNumber;
        int ct = (int) Math.round(record.clumpThickness + rand.nextGaussian() * stdDev);
        int csu = (int) Math.round(record.cSizeUni + rand.nextGaussian() * stdDev);
        int cshu = (int) Math.round(record.cShapeUni + rand.nextGaussian() * stdDev);
        int ma = (int) Math.round(record.mAdhesion + rand.nextGaussian() * stdDev);
        int secs = (int) Math.round(record.secs + rand.nextGaussian() * stdDev);
        int bn = (int) Math.round(record.bareNuclei + rand.nextGaussian() * stdDev);
        int bc = (int) Math.round(record.blandChromatin + rand.nextGaussian() * stdDev);
        int nn = (int) Math.round(record.normalNucleoli + rand.nextGaussian() * stdDev);
        int m = (int) Math.round(record.mitoses + rand.nextGaussian() * stdDev);
        int cls = record.classification;
        return new BreastCancerCompleteData(cn, ct, csu, cshu, ma, secs, bn, bc, nn, m, cls);
    }

    //2. SCALING BY A FACTOR
    public static BreastCancerCompleteData scale(BreastCancerCompleteData record, double factor) {
        int cn = record.codeNumber;
        int ct = (int) Math.round(record.clumpThickness * factor);
        int csu = (int) Math.round(record.cSizeUni * factor);
        int cshu = (int) Math.round(record.cShapeUni * factor);
        int ma = (int) Math.round(record.mAdhesion * factor);
        int secs = (int) Math.round(record.secs * factor);
        int bn = (int) Math.round(record.bareNuclei * factor);
        int bc = (int) Math.round(record.blandChromatin * factor);
        int nn = (int) Math.round(record.normalNucleoli * factor);
        int m = (int) Math.round(record.mitoses * factor);
        int cls = record.classification;
        return new BreastCancerCompleteData(cn, ct, csu, cshu, ma, secs, bn, bc, nn, m, cls);
    }

    //3. COMBINE TWO RECORDS FOR A NEW RECORD
    public static BreastCancerCompleteData combine(BreastCancerCompleteData record1, BreastCancerCompleteData record2) {
        int cn = record1.codeNumber;
        int ct = Math.max(record1.clumpThickness, record2.clumpThickness);
        int csu = Math.max(record1.cSizeUni, record2.cSizeUni);
        int cshu = Math.max(record1.cShapeUni, record2.cShapeUni);
        int ma = Math.max(record1.mAdhesion, record2.mAdhesion);
        int secs = Math.max(record1.secs, record2.secs);
        int bn = Math.max(record1.bareNuclei, record2.bareNuclei);
        int bc = Math.max(record1.blandChromatin, record2.blandChromatin);
        int nn = Math.max(record1.normalNucleoli, record2.normalNucleoli);
        int m = Math.max(record1.mitoses, record2.mitoses);
        int cls = Math.max(record1.classification, record2.classification);
        return new BreastCancerCompleteData(cn, ct, csu, cshu, ma, secs, bn, bc, nn, m, cls);
    }

    public static ArrayList<BreastCancerCompleteData> augmentDataNoise(ArrayList<BreastCancerCompleteData> alcd){
        double noiseStdDev = 0.05;
        ArrayList<BreastCancerCompleteData> augmented = new ArrayList<>();
        for(BreastCancerCompleteData cd: alcd){
            augmented.add(cd);
            augmented.add(addNoise(cd, noiseStdDev));
        }
        return augmented;
    }
    
}



