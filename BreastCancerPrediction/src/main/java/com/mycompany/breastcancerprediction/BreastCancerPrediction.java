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
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;


public class BreastCancerPrediction {
    private static final double INITIAL_TEMPERATURE = 1000;
    private static final double COOLING_RATE = 0.003;
    private static final int ITERATIONS_PER_TEMPERATURE = 100;

    public static void main(String[] args) {
        
        DBConnection.createDatabase();
        DBConnection.createBreastCancerData();
        readAndAddData();
        
        ArrayList<BreastCancerMissingData> almd = new ArrayList<BreastCancerMissingData>();
        ArrayList<BreastCancerCompleteData> alcd = new ArrayList<BreastCancerCompleteData>();
        readMissingAndCompleteData(almd, alcd);
        //TESTING = ALMD
        // System.out.println("MISSING = " + almd.size());
        //TRAINING = ALCD

        // doFeatureSelection(alcd
        int predictK = bestKPredict(alcd);
        System.out.println("OPTIMAL K FOR PREDICTION: " + predictK);
        //Finding best value of K for KNN
        // int k = bestK(alcd);
        // completeByMeanError(alcd);
        // System.out.println("K = " + k);
        // //Completing values based on KNN
        // completeValues(almd, alcd, k);
        // normalFeatureSelection(alcd);
        
        // doFeatureSelection(alcd);
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
        diff += abs(item1.getClassification() - item2.getClassification());
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
    public static int  PredictkNearestNeighbors(BreastCancerCompleteData item1, ArrayList<BreastCancerCompleteData> items, int k){
        ArrayList<Integer> neighbors = new ArrayList<>(); //CLASSIFICATION STORED
        HashMap<Integer, Integer> classfDist = new HashMap<>(); //DISTANCE->INDEX IN NEIGHBORS
        int distance = 0;

        for(BreastCancerCompleteData item: items){
            // System.out.println("In Knn" + items.size());
            //PROBLEM: NEIGHBORS REMOVES FIRST OCCURENCE INSTEAD OF THAT ITEM WITH LARGEST INDEX: SOLN = STORE INDEX, NOT CLASSIFICATION
            distance = PredictdistanceMetric(item1, item);
            if(neighbors.size()<k && distance!=0){
                neighbors.add(item.getClassification());
                classfDist.put(distance, neighbors.size()-1);
            }    
            else{ 
                if(!classfDist.keySet().isEmpty() && Collections.max(classfDist.keySet())>distance && distance!=0){                    
                    neighbors.add(item.getClassification());
                    neighbors.remove((classfDist.get(Collections.max(classfDist.keySet()))));
                    classfDist.put(distance, neighbors.size());
                    classfDist.remove(Collections.max(classfDist.keySet()));
                }
            }
        
        }
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
        return sum/(double)totalCount;
    }
    public static int bestKPredict(ArrayList<BreastCancerCompleteData> alcd){
        HashMap<Integer, Double> kmap = new HashMap<>();
        for(int k = 1; k<26; k++){
            kmap.put(k, accuracyOfKPredict(k, alcd));
            System.out.println("K = " + k + "Accuracy = " + kmap.get(k));
        }
    
        int bestK = 1;
        for(Map.Entry<Integer, Double> ele: kmap.entrySet()){
            if(ele.getValue() > kmap.get(bestK)){
                bestK = ele.getKey();
            }
        }
        return bestK;
    }
    // public static void checkKNNPredict(ArrayList<BreastCancerCompleteData> alcd){
    //     int k = bestKpredict(alcd);
    //     for(BreastCancerCompleteData cd: alcd){
    //         int classf = PredictkNearestNeighbors(cd, alcd, k)
    //     }
    // }
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
            System.out.println("SIZE IN MEAN = " + alcd.size());
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
                System.out.println(accuracy[i]);
            }

    }
    public static double crossValidation( double[][] X, double[] Y){
            OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
            // double[][] X = new double[alcd.size()][9];
            // double[] Y = new double[alcd.size()];
            // int i1=0;
            // for(BreastCancerCompleteData item : alcd){
            //     X[i1][0] = item.getClumpThickness();
            //     X[i1][1] = item.getcSizeUni();
            //     X[i1][2] = item.getcShapeUni();
            //     X[i1][3] = item.getmAdhesion();
            //     X[i1][4] = item.getSecs();
            //     X[i1][5] = item.getBareNuclei();
            //     X[i1][6] = item.blandChromatin;
            //     X[i1][7] = item.getNormalNucleoli();
            //     X[i1][8] = item.getMitoses(); 
            //     Y[i1] = item.getClassification();
            //     i1++;
            // }
            // model.newSampleData(Y, X);
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
    private static void shuffle(double[][] x, double[] y) {
    
    
    }
    public static void normalFeatureSelection(ArrayList<BreastCancerCompleteData> alcd){
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
                pValues = getPvalues(X, Y); // recalculate p-values
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
        double accuracy = calculateAccuracy(Y,predicted);
        System.out.println("P-value = 0.05 Accuracy = " + accuracy);        
    }
    public static double acceptanceProbability(double currentEnergy, double newEnergy, double temperature) {
        // If the new solution is better than the current solution, accept it
        if (newEnergy < currentEnergy) {
            return 1.0;
        }
        
        // Calculate the probability of accepting a worse solution based on the temperature and energy difference
        return Math.exp((newEnergy-currentEnergy) / temperature);
    }
    public static void doFeatureSelection(ArrayList<BreastCancerCompleteData> alcd){
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        double[][] X = new double[alcd.size()][9];
        double[][] X2 = new double[alcd.size()][9];
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
    
        
        double[] pValues = getPvalues(X, Y);
        X2 = Arrays.copyOf(X, X.length);
        System.out.println("\n\nP-VALUES = ");
        for(int j = 0; j<pValues.length; j++){
            System.out.println(pValues[j]);
        }
        // double lowerBound = 0.05E-200;
        // double upperBound =  0.075E-30;
        // Random random = new Random();
        // double currentSolution = 0.075E-40;
        // double temperature = INITIAL_TEMPERATURE;
        // HashMap<Double, Double> pvalAccuracy = new HashMap<>();
        // while (temperature > 1) {

        //     double decreaseFactor = (currentSolution - lowerBound) / (upperBound - lowerBound);
        //     currentSolution = upperBound - (decreaseFactor * (upperBound - lowerBound));
            
        //     // generate a new solution within the bounds
        //     double newSolution = random.nextDouble() * (upperBound - lowerBound) + lowerBound;
        //     double acceptanceProbability = acceptanceProbability(currentSolution, newSolution, temperature);
        //         if (acceptanceProbability > random.nextDouble()) {
        //             currentSolution = newSolution;
        //         }
        //         for(int j = 0; j<pValues.length;j++ ) {
        //             if(pValues[j]>currentSolution) {
        //                 // System.out.println("ATTRIBUTE ELIMINATED " + pValues[j]);
        //                 // remove the j-th column from the X matrix
        //                 double[][] temp = new double[X.length][X[0].length - 1];
        //                 for(int m = 0; m < X.length; m++) {
        //                     int w = 0;
        //                     for(int n = 0; n < X[0].length && w<X[0].length-1; n++) {
        //                         if(n != j) {
        //                             temp[m][w] = X[m][n];
        //                             w++;
        //                         }
        //                     }
        //                 }
        //                 j=0;
        //                 X = temp; // update X matrix with the new one without the j-th column
        //                 pValues = getPvalues(X, Y); // recalculate p-values
        //             }
        //         }
                    

        //         double accuracy = crossValidation(X,Y);
        //         System.out.println("P-values = " + currentSolution  + " Accuracy = " + accuracy); 
        //         pvalAccuracy.put(currentSolution, accuracy);
        //         temperature *= 1 - COOLING_RATE;

        //         //REINITIALISE MATRIX AND PVALUES
        //         X = X2;
        //         pValues  = getPvalues(X, Y);
        // }
        // double pval = 0;
        // double accuracy = 0;
        // for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
        //     if(entry.getValue() > accuracy){
        //         accuracy = entry.getValue();
        //         pval = entry.getKey();
        //     }
        // }
        // System.out.println("\n\nP VALUES:         ");
        // for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
        //     System.out.println(entry.getKey());
        // }
        // System.out.println("\n\n ACCURACIES:    ");
        // for(Map.Entry<Double, Double> entry: pvalAccuracy.entrySet()){
        //     System.out.println(entry.getValue());
        // }
        // System.out.println("Best accuracy at p-value: "+pval +"of accuracy: " + accuracy);
        //accuracy
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



    //DATA AUGMENTATION TECHNIQUES
    //1. ADDING RANDOM NOISE
    public BreastCancerCompleteData addNoise(BreastCancerCompleteData record, double stdDev) {
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
    public BreastCancerCompleteData scale(BreastCancerCompleteData record, double factor) {
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
    public BreastCancerCompleteData combine(BreastCancerCompleteData record1, BreastCancerCompleteData record2) {
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
        int cls = record1.classification;
        return new BreastCancerCompleteData(cn, ct, csu, cshu, ma, secs, bn, bc, nn, m, cls);
    }


}



