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
            String databaseName = "hdp";
            String username = "root";
            String password = "yash2003";

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
        String url = "jdbc:mysql://localhost:3306/hdp";
        String username = "root";
        String password = "yash2003";
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
            qw.setString(1, "hdp");
            qw.setString(2, "heartdata");
            ResultSet r = qw.executeQuery();
            if(r.next()){
                String sss = "drop table heartdata;";
                PreparedStatement ppp = con.prepareStatement(sss);
                ppp.executeUpdate();
            }
            if(!r.next()){
               String sq = "create table heartdata ("
                       + "age numeric(10,2), "
                    + "sex numeric(10,2), "
                    + "cp numeric(10,2), "
                    + "trestbps numeric(10,2), "
                    + "chol numeric(10,2), "
                    + "fbs numeric(10,2), "
                    + "restecg numeric(10,2), "
                    + "thalach numeric(10,2), "
                    + "exang numeric(10,2), "
                    + "oldpeak numeric(10,2), "
                    + "slope numeric(10,2), "
                    + "ca varchar(10), "
                    + "thal varchar(10), "
                    + "num int"
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
