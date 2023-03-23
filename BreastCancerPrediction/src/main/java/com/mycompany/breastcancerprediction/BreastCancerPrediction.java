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

public class BreastCancerPrediction {
    
    public static void main(String[] args) {
        
        DBConnection.createDatabase();
        DBConnection.createBreastCancerData();
        readAndAddData();
        
        ArrayList<BreastCancerMissingData> almd = new ArrayList<BreastCancerMissingData>();
        ArrayList<BreastCancerCompleteData> alcd = new ArrayList<BreastCancerCompleteData>();
        readMissingAndCompleteData(almd, alcd);
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
}
