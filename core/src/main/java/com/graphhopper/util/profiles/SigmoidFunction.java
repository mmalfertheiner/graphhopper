package com.graphhopper.util.profiles;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import java.lang.Math;

public class SigmoidFunction implements ParametricUnivariateFunction {

    @Override
    public double value(double x, double... params) {

        double a = params[0];
        double b = params[1];
        double c = params[2];

        double value = 1 - ( a / (1 + Math.exp(-b * (x - c))));

        return value;
    }

    @Override
    public double[] gradient(double x, double... params) {

        double a = params[0];
        double b = params[1];
        double c = params[2];

        double[] gradient = new double[3];

        gradient[0] = - ( 1 / (Math.exp(b * ( c - x )) + 1));
        gradient[1] = (a * ( c - x ) * Math.exp( -(b * ( x - c )))) / Math.pow(Math.exp( -(b * ( x - c ))) + 1, 2);
        gradient[2] = (a * b * Math.exp( -(b * ( x - c )))) / Math.pow(Math.exp( -(b * ( x - c ))) + 1, 2);

        return gradient;
    }

}
