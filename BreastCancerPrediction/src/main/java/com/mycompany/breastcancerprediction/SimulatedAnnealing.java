/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.mycompany.breastcancerprediction;
import java.util.Random;

public class SimulatedAnnealing {
    private static final double INITIAL_TEMPERATURE = 1000;
    private static final double COOLING_RATE = 0.003;
    private static final int ITERATIONS_PER_TEMPERATURE = 100;

    public static double findMinimum(Function function, double lowerBound, double upperBound) {
        Random random = new Random();
        double currentSolution = random.nextDouble() * (upperBound - lowerBound) + lowerBound;
        double currentCost = function.evaluate(currentSolution);
        double temperature = INITIAL_TEMPERATURE;
        while (temperature > 1) {
            for (int i = 0; i < ITERATIONS_PER_TEMPERATURE; i++) {
                double newSolution = random.nextDouble() * (upperBound - lowerBound) + lowerBound;
                double newCost = function.evaluate(newSolution);
                double acceptanceProbability = acceptanceProbability(currentCost, newCost, temperature);
                if (acceptanceProbability > random.nextDouble()) {
                    currentSolution = newSolution;
                    currentCost = newCost;
                }
            }
            temperature *= 1 - COOLING_RATE;
        }
        return currentSolution;
    }

    private static double acceptanceProbability(double currentCost, double newCost, double temperature) {
        if (newCost < currentCost) {
            return 1;
        }
        return Math.exp((currentCost - newCost) / temperature);
    }

    public interface Function {
        double evaluate(double x);
    }
    
}
