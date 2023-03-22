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

public class BreastCancerPrediction {

    public static void main(String[] args) {
        
        DBConnection.createDatabase();
        DBConnection.createBreastCancerData();
        readAndAddData();
    }
    public static void readAndAddData(){
        Scanner sc = null;
        try{
            sc = new Scanner(new FileInputStream("breast-cancer-wisconsin.data"));
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
            }
            
        }
        sc.close();
    }
}
