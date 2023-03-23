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

public class HeartDiseasePrediction {

    public static void main(String[] args) {
        DBConnection.createDatabase();
        DBConnection.createHeartDiseaseData();
        readAndAddData();
        
        ArrayList<HeartDiseaseMissingData> almd = new ArrayList<HeartDiseaseMissingData>();
        ArrayList<HeartDiseaseCompleteData> alcd = new ArrayList<HeartDiseaseCompleteData>();
        readMissingAndCompleteData(almd, alcd);
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
}
