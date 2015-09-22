package com.graphhopper.util;

public class MeanFilter implements SmoothingFilter {


    private double[] smoothedMeasurements;
    private double[] distances;
    private double minDistance;

    public MeanFilter(double[] distances, double minDistance) {
        setDistances(distances);
        this.minDistance = minDistance;
    }

    public void setDistances(double[] distances) {

        if(distances == null)
            throw new IllegalArgumentException("Distances can not be null");


        this.distances = distances;
    }

    public void validateMeasurements(final double[] measurements){

        if (distances.length + 1 != measurements.length)
            throw new IllegalArgumentException("Distances must have exactly one entry less than measurements, but distances was " + distances.length + " and measurements is " + measurements.length);

    }

    @Override
    public double[] smooth(final double[] measurements) {

        validateMeasurements(measurements);

        double tmpDistanceRight;
        double tmpDistanceLeft;
        double elevation;
        smoothedMeasurements = new double[measurements.length];

        for(int i = 0; i < measurements.length; i++){

            elevation = measurements[i];
            int countR = 1;
            int countL = 0;

            if(i < distances.length) {

                tmpDistanceRight = distances[i];

                while(tmpDistanceRight < minDistance){

                    elevation += measurements[i+countR];

                    if(i+countR >= distances.length){
                        countR++;
                        break;
                    }

                    tmpDistanceRight += distances[i+countR];
                    countR++;
                }
            }

            if(i > 0) {

                tmpDistanceLeft = distances[i - 1];

                while (tmpDistanceLeft < minDistance) {
                    elevation += measurements[i - countL - 1];

                    if (i - countL - 1 == 0) {
                        countL++;
                        break;
                    }

                    tmpDistanceLeft += distances[i - countL - 2];
                    countL++;
                }
            }

            smoothedMeasurements[i] = Helper.round2(elevation / (countR+countL));

        }

        return getFilteredValues();
    }

    @Override
    public double[] getFilteredValues() {
        if(smoothedMeasurements != null){

            return smoothedMeasurements;

        }
        return null;
    }

    public static void main(String[] args){

        double[] measurements = new double[]{2,4,6,4,2,2,2,6,8};
        double[] distance = new double[]{1,2,3,1,1,3,2,2};

        MeanFilter meanFilter = new MeanFilter(distance, 3);

        double[] result = meanFilter.smooth(measurements);

        for(double r : result){
            System.out.print(" " + r);
        }

    }
}
