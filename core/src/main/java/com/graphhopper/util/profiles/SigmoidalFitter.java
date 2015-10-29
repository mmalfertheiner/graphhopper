package com.graphhopper.util.profiles;

import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class SigmoidalFitter extends AbstractCurveFitter{

    private double[] initialGuess;


    public SigmoidalFitter(double[] initialGuess){
        this.initialGuess = initialGuess;
    }


    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {


        final int len = points.size();
        final double[] target  = new double[len];
        final double[] weights = new double[len];

        int i = 0;

        for(WeightedObservedPoint point : points) {
            target[i]  = point.getY();
            weights[i] = point.getWeight();
            i += 1;
        }

        final AbstractCurveFitter.TheoreticalValuesFunction model = new
                AbstractCurveFitter.TheoreticalValuesFunction(new SigmoidFunction(), points);

        final DiagonalMatrix weightMatrix = new DiagonalMatrix(weights);

        return new LeastSquaresBuilder().
                maxEvaluations(Integer.MAX_VALUE).
                maxIterations(Integer.MAX_VALUE).
                start(initialGuess).
                target(target).
                weight(weightMatrix).
                model(model.getModelFunction(), model.getModelFunctionJacobian()).
                build();
    }

    public static void main(String[] args) {
        SigmoidalFitter fitter = new SigmoidalFitter(new double[] {1, 0.5, -1});
        ArrayList<WeightedObservedPoint> points = new ArrayList<WeightedObservedPoint>();

        // Add points here; for instance,
        WeightedObservedPoint point1 = new WeightedObservedPoint(222.0084, -11, 33.30126 / 37.36653);
        WeightedObservedPoint point2 = new WeightedObservedPoint(409.9835, -10, 35.188465 / 37.36653);
        WeightedObservedPoint point3 = new WeightedObservedPoint(1675.1172, -9, 35.625652 / 37.36653);
        WeightedObservedPoint point4 = new WeightedObservedPoint(1075.3177, -8, 37.36653 / 37.36653);
        WeightedObservedPoint point5 = new WeightedObservedPoint(60.274113, -7, 6.7808375 / 37.36653);
        WeightedObservedPoint point6 = new WeightedObservedPoint(651.5845, -6, 28.758934 / 37.36653);
        WeightedObservedPoint point7 = new WeightedObservedPoint(205.16458, -5, 30.774687 / 37.36653);
        WeightedObservedPoint point8 = new WeightedObservedPoint(171.65067, -4, 30.89712 / 37.36653);
        WeightedObservedPoint point9 = new WeightedObservedPoint(389.28305, -3, 24.898178 / 37.36653);
        WeightedObservedPoint point10 = new WeightedObservedPoint(204.34619, -2, 13.880118 / 37.36653);
        WeightedObservedPoint point11 = new WeightedObservedPoint(209.81961, -1, 11.989692 / 37.36653);
        WeightedObservedPoint point12 = new WeightedObservedPoint(203.6042, 0, 15.595216 / 37.36653);
        WeightedObservedPoint point13 = new WeightedObservedPoint(619.88995, 1, 14.0128 / 37.36653);
        WeightedObservedPoint point14 = new WeightedObservedPoint(455.45798, 2, 10.182568 / 37.36653);
        WeightedObservedPoint point15 = new WeightedObservedPoint(205.16792, 3, 12.9579735 / 37.36653);
        WeightedObservedPoint point16 = new WeightedObservedPoint(451.05057, 4, 12.913956 / 37.36653);
        WeightedObservedPoint point17 = new WeightedObservedPoint(471.85242, 5, 7.089638 / 37.36653);
        WeightedObservedPoint point18 = new WeightedObservedPoint(646.1104, 6, 9.526044 / 37.36653);
        WeightedObservedPoint point19 = new WeightedObservedPoint(689.3907, 7, 8.498449 / 37.36653);
        WeightedObservedPoint point20 = new WeightedObservedPoint(208.12926, 8, 9.250189 / 37.36653);
        WeightedObservedPoint point21 = new WeightedObservedPoint(1262.8378, 9, 7.8886223 / 37.36653);
        WeightedObservedPoint point22 = new WeightedObservedPoint(214.3265, 10, 7.9543853 / 37.36653);
        WeightedObservedPoint point23 = new WeightedObservedPoint(617.08203, 11, 7.3262196 / 37.36653);
        WeightedObservedPoint point24 = new WeightedObservedPoint(219.78712, 12, 5.733577 / 37.36653);
        WeightedObservedPoint point25 = new WeightedObservedPoint(202.69017, 13, 6.0807056 / 37.36653);
        WeightedObservedPoint point26 = new WeightedObservedPoint(216.81699, 30, 6.7873144 / 37.36653);

        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);
        points.add(point5);
        points.add(point6);
        points.add(point7);
        points.add(point8);
        points.add(point9);
        points.add(point10);
        points.add(point11);
        points.add(point12);
        points.add(point13);
        points.add(point14);
        points.add(point15);
        points.add(point16);
        points.add(point17);
        points.add(point18);
        points.add(point19);
        points.add(point20);
        points.add(point21);
        points.add(point22);
        points.add(point23);
        points.add(point24);
        points.add(point25);
        points.add(point26);

        final double coeffs[] = fitter.fit(points);
        System.out.println(Arrays.toString(coeffs));
    }
}
