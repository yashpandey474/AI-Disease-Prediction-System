/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.heartdiseaseprediction;

/**
 *
 * @author ohidu
 */
public class HeartDiseaseMissingData {
    
    double age, sex, cp, trestbps, chol, fbs, restecg, thalach, exang, oldpeak, slope, ca=-1, thal = -1;
    int num;
    public HeartDiseaseMissingData(
            double age, double sex, double cp, double trestbps, double chol, double fbs, double restecg,
            double thalach, double exang, double oldpeak, double slope, int num
    ){
        this.age = age;
        this.sex = sex;
        this.cp = cp;
        this.trestbps = trestbps;
        this.chol = chol;
        this.fbs = fbs;
        this.restecg = restecg;
        this.thalach = thalach;
        this.exang = exang;
        this.oldpeak = oldpeak;
        this.slope = slope;
        this.num = num;
    }
    
    public double getAge() {
        return age;
    }

    public void setAge(double age) {
        this.age = age;
    }

    public double getSex() {
        return sex;
    }

    public void setSex(double sex) {
        this.sex = sex;
    }

    public double getCp() {
        return cp;
    }

    public void setCp(double cp) {
        this.cp = cp;
    }

    public double getTrestbps() {
        return trestbps;
    }

    public void setTrestbps(double trestbps) {
        this.trestbps = trestbps;
    }

    public double getChol() {
        return chol;
    }

    public void setChol(double chol) {
        this.chol = chol;
    }

    public double getFbs() {
        return fbs;
    }

    public void setFbs(double fbs) {
        this.fbs = fbs;
    }

    public double getRestecg() {
        return restecg;
    }

    public void setRestecg(double restecg) {
        this.restecg = restecg;
    }

    public double getThalach() {
        return thalach;
    }

    public void setThalach(double thalach) {
        this.thalach = thalach;
    }

    public double getExang() {
        return exang;
    }

    public void setExang(double exang) {
        this.exang = exang;
    }

    public double getOldpeak() {
        return oldpeak;
    }

    public void setOldpeak(double oldpeak) {
        this.oldpeak = oldpeak;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public double getCa() {
        return ca;
    }

    public void setCa(double ca) {
        this.ca = ca;
    }

    public double getThal() {
        return thal;
    }

    public void setThal(double thal) {
        this.thal = thal;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
    
}
