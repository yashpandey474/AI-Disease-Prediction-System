/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.heartdiseaseprediction;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 *
 * @author ohidu
 */
public class CustomOLS extends OLSMultipleLinearRegression {

    public double[][] getDesignMatrix() {
        RealMatrix X = getX();
        return X.getData();
    }
}
