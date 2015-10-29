package com.graphhopper.util.profiles;

import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RidersProfile implements Serializable{

    public transient final static int WAY_TYPES = 16;
    public transient final static int SLOPES = 60;

    private RidersEntry[][] speedMatrix = new RidersEntry[WAY_TYPES][SLOPES+1]; // 16 Way types, Steigung von -30 % bis + 30 %
    private double totalDistance;


    public RidersProfile(){};

    public RidersEntry getEntry(int wayType, int slope){
        return speedMatrix[wayType][slope+(SLOPES/2)];
    }

    public RidersEntry[] getEntries(int wayType) {
        return speedMatrix[wayType];
    }

    public Map<Integer, double[]> getFilterSpeeds(){

        Map<Integer, double[]> speedMap = new HashMap<Integer, double[]>();

        for( int i = 0; i < WAY_TYPES; i++ ){

            if(getDistance(i) > 10000) {
                speedMap.put(i, filterSpeeds(getEntries(i), i));
            }

        }

        return speedMap;
    }

    private double[] filterSpeeds(RidersEntry[] ridersEntries, int wayType) {

        ArrayList<WeightedObservedPoint> points = new ArrayList<WeightedObservedPoint>();

        double maxSpeed = maxSpeed(wayType);

        for ( int i = 0; i < ridersEntries.length; i++){
            if(ridersEntries[i] != null) {
                points.add(new WeightedObservedPoint(ridersEntries[i].getDistance(), i - SLOPES / 2, ridersEntries[i].getSpeed() / maxSpeed));
            }
        }

        final double[] coef = new SigmoidalFitter(new double[]{1, 0.5, -1}).fit(points);
        SigmoidFunction sigF = new SigmoidFunction();

        double[] result = new double[SLOPES + 1];

        int offset = SLOPES / 2;

        for( int i = - offset; i < offset + 1; i++){
            result[i + offset] = sigF.value(i, coef) * maxSpeed;
        }

        return result;
    }

    public double maxSpeed(int wayType){

        double max = 0.0;

        for (RidersEntry r : speedMatrix[wayType]){
            if(r != null && r.getSpeed() > max)
                max = r.getSpeed();
        }

        return max;
    }


    public double getSpeed(int wayType, int slope){

        RidersEntry entry = getEntry(wayType, slope);

        if(entry == null)
            return Double.NaN;

        return getEntry(wayType, slope).getSpeed();
    }

    public double getDistance(int wayType, int slope){

        RidersEntry entry = getEntry(wayType, slope);

        if(entry == null)
            return 0.0;

        return getEntry(wayType, slope).getDistance();
    }

    public double getDistance(int wayType){
        double totalDist = 0;

        for(int i = 0; i < speedMatrix[wayType].length; i++) {
            if(speedMatrix[wayType][i] != null)
                totalDist += speedMatrix[wayType][i].getDistance();
        }

        return totalDist;
    }

    public double getTotalDistance(){
        return totalDistance;
    }

    public double[] getWayTypePriority(){

        double[] distancesPerWayType = new double[WAY_TYPES];

        for (int i = 0; i < speedMatrix.length; i++) {
            for (int j = 0; j < speedMatrix[i].length; j++) {
                distancesPerWayType[i] += getDistance(i, j - (SLOPES / 2));
            }

            distancesPerWayType[i] = distancesPerWayType[i] / totalDistance * 100;
        }

        return distancesPerWayType;

    }

    public void update(TrackPart trackPart) {

        if(trackPart == null)
            return;

        int wayType = (int) trackPart.getWayType();
        int slope = (int) trackPart.getSlope();

        if (wayType < 0 || wayType > 15)
            throw new IllegalArgumentException("Waytype must be between 0 and 15, but was: " + wayType);

        if (slope < - (SLOPES/2))
            slope = - (SLOPES/2);

        if (slope > SLOPES/2)
            slope = SLOPES/2;

        RidersEntry ridersEntry = speedMatrix[wayType][slope+(SLOPES/2)];

        if(ridersEntry == null) {
            ridersEntry = new RidersEntry();
            speedMatrix[wayType][slope+(SLOPES/2)] = ridersEntry;
        }

        ridersEntry.updateEntry(trackPart.getSpeed(), trackPart.getDistance());
        totalDistance += trackPart.getDistance();

    }

    public int update(List<TrackPart> trackPartList) {

        int skipped = 0;

        for(TrackPart tp : trackPartList) {
            try{
                update(tp);
            } catch(IllegalArgumentException i){
                skipped++;
            }
        }

        return skipped;

    }

    @Override
    public java.lang.String toString() {

        String riderString = "";

        for (int i = 0; i < speedMatrix.length; i++) {
            riderString += "-----[ " + i + " ]------ \n";
            for (int j = 0; j < speedMatrix[i].length; j++) {
                if(speedMatrix[i][j] != null)
                    riderString += (j - SLOPES / 2) + ", " + speedMatrix[i][j].getSpeed() + ", " + speedMatrix[i][j].getDistance() + "\n";
            }
            riderString += "----------------------\n";
        }

        return riderString;
    }

}
