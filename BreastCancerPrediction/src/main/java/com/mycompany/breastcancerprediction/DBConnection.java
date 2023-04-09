/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.breastcancerprediction;


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
        String url = "jdbc:mysql://localhost:3306/bcp";
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
    public static void createBreastCancerData(){
        try{
            Connection con;
            con = DBConnection.getConnection();
            String p = "SELECT * FROM information_schema.tables WHERE table_schema = ? AND table_name = ? LIMIT 1;";
            PreparedStatement qw = con.prepareStatement(p);
            qw.setString(1, "bcp");
            qw.setString(2, "bcancerdata");
            ResultSet r = qw.executeQuery();
            if(r.next()){
                String sss = "drop table bcancerdata;";
                PreparedStatement ppp = con.prepareStatement(sss);
                ppp.executeUpdate();
            }
            
            String sq = "create table bcancerdata (codenumber int , "
                 + "clumpthickness int , "
                 + "csizeuni int , "
                 + "cshapeuni int , "
                 + "madhesion int , "
                 + "secs int , "
                 + "barenuclei varchar(2), "
                 + "blandchromatin int , "
                 + "normalnucleoli int, "
                 + "mitoses int, "
                 + "class int "
                 + ");";
             PreparedStatement pst = con.prepareStatement(sq);
             pst.executeUpdate(); 
            
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    
}
