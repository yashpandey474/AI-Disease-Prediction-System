/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.heartdiseaseprediction;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;


public class DBConnection {
    public static void createDatabase(){
        try {
            boolean exists=false;
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            String databaseName = "bcp";
            String username = "root";
            String password = "rootpass";

            String url = "jdbc:mysql://localhost:3306/mysql?zeroDateTimeBehavior=CONVERT_TO_NULL";
            Connection conn = DriverManager.getConnection(url,username, password);
            ResultSet rs = conn.getMetaData().getCatalogs();
            while (rs.next()){
              String exdb = rs.getString(1);
                if(exdb.equals(databaseName)){
                    exists=true;
                    break;
                }
            }
            rs.close();

            if(!exists){
                String sql = "CREATE DATABASE " + databaseName;
                Statement statement = conn.createStatement();
                statement.executeUpdate(sql);
                statement.close();
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Connection getConnection(){
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/bcp";
        String username = "root";
        String password = "rootpass";
        try{
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url,username,password);
            return conn;
        }
        catch(Exception e){
            System.out.println(e);
        }
        return null;
    }
    public static void createHeartDiseaseData(){
        try{
            Connection con;
            con = DBConnection.getConnection();
            String p = "SELECT * FROM information_schema.tables WHERE table_schema = ? AND table_name = ? LIMIT 1;";
            PreparedStatement qw = con.prepareStatement(p);
            qw.setString(1, "bcp");
            qw.setString(2, "bcancerdata");
            ResultSet r = qw.executeQuery();
            if(!r.next()){
               String sq = "create table bcancerdata ("
                       + "age int, "
                    + "sex varchar(2) not null, "
                    + "cp varchar(2) not null, "
                    + "trestbps varchar(2) not null, "
                    + "chol varchar(2) not null, "
                    + "fbs varchar(2) not null, "
                    + "restecg varchar(2) not null, "
                    + "thalach varchar(2) not null, "
                    + "exang varchar(2) not null, "
                    + "oldpeak varchar(2) not null, "
                    + "slope varchar(2) not null, "
                    + "ca varchar(2) not null, "
                    + "thal varchar(2) not null, "
                    + "num varchar(2) not null "
                    + ");";
                PreparedStatement pst = con.prepareStatement(sq);
                pst.executeUpdate(); 
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    
}
