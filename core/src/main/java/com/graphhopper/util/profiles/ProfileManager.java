package com.graphhopper.util.profiles;


import com.graphhopper.routing.util.BikeGenericFlagEncoder;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.graphhopper.util.Helper.keepIn;

public class ProfileManager {

    private ProfileRepository profileRepository;
    private RidersProfile ridersProfile;

    private double totalDistance;
    private int bestFit = -1;
    private short[] counts = new short[RidersProfile.WAY_TYPES];
    private double[] distances = new double[RidersProfile.WAY_TYPES];
    private Map<Integer, double[]> userSpeeds;

    public ProfileManager(ProfileRepository profileRepository){
        this.profileRepository = profileRepository;
    }

    public ProfileManager init(String name, BikeGenericFlagEncoder flagEncoder){
        this.ridersProfile = profileRepository.getProfile(name);

        if(hasProfile()){

            this.userSpeeds = new HashMap<Integer, double[]>();
            double availableData = 0;

            for(int i = 0; i < RidersProfile.WAY_TYPES; i++){

                RidersEntry[] tmpEntries = this.ridersProfile.getEntries(i);

                for (int j = 0; j < RidersProfile.SLOPES+1; j++) {

                    if(tmpEntries[j] != null) {
                        counts[i]++;
                        distances[i] += tmpEntries[j].getDistance();
                        totalDistance += tmpEntries[j].getDistance();
                    }

                }


                if(hasSpeedProfile(i)) {

                    if(counts[i] * distances[i] > availableData){
                        bestFit = i;
                        availableData = counts[i] * distances[i];
                    }

                    this.userSpeeds.put(i, filterSpeeds(tmpEntries, i, flagEncoder));
                }

            }

        }

        return this;

    }

    public boolean hasProfile(){
        return this.ridersProfile != null;
    }

    public boolean hasSpeedProfile(int wayType){
        return distances[wayType] > 10000 && counts[wayType] > 5;
    }

    public boolean hasFilteredSpeeds(){
        return bestFit > 0;
    }

    public double getSpeedPerSlope(int wayType, int slopeIndex, BikeGenericFlagEncoder flagEncoder) {

        if(!hasProfile())
            return Double.NaN;

        if(hasSpeedProfile(wayType))
            return userSpeeds.get(wayType)[slopeIndex];

        if(bestFit > 0){
            double adjustment = (double) flagEncoder.getWayTypeSpeed(wayType) / flagEncoder.getWayTypeSpeed(bestFit);
            return userSpeeds.get(bestFit)[slopeIndex] * adjustment;
        }

        return Double.NaN;

    }

    private double[] filterSpeeds(RidersEntry[] ridersEntries, int wayType, BikeGenericFlagEncoder flagEncoder) {

        ArrayList<WeightedObservedPoint> points = new ArrayList<WeightedObservedPoint>();

        double maxSpeed = ridersProfile.maxSpeed(wayType, flagEncoder.getMaxSpeed());

        addControlPoints(points, wayType, flagEncoder.getWayTypeSpeed(wayType), maxSpeed);

        for ( int i = 0; i < ridersEntries.length; i++){
            if(ridersEntries[i] != null) {
                points.add(new WeightedObservedPoint(ridersEntries[i].getDistance(), i - RidersProfile.SLOPES / 2, ridersEntries[i].getSpeed() / maxSpeed));
            }
        }

        //addControlPoints(points, wayType, flagEncoder.getWayTypeSpeed(wayType), maxSpeed);

        final double[] coef = new SigmoidalFitter(new double[]{1, 0.5, -1}).fit(points);
        SigmoidFunction sigF = new SigmoidFunction();

        double[] result = new double[RidersProfile.SLOPES + 1];

        int offset = RidersProfile.SLOPES / 2;

        for( int i = - offset; i < offset + 1; i++){
            result[i + offset] = sigF.value(i, coef) * maxSpeed;
        }

        return result;
    }

    private void addControlPoints(ArrayList<WeightedObservedPoint> points, int wayType, double baseSpeed, double maxSpeed) {

        double weight = totalDistance / 1000;

        //Add control points from +10 to -10 (this is the critical zone)

        for (int i = -12; i < 0; i++) {
            double fwdFaster = Math.sqrt(1 + 30 * (i * (-1) / 100));
            double speed = keepIn(fwdFaster * baseSpeed, baseSpeed, maxSpeed);
            points.add(new WeightedObservedPoint(weight, i, speed / maxSpeed));
        }

        for (int i = 0; i <= 12; i++) {
            double fwdSlower = Math.sqrt(1 - 5 * (i/100));
            double speed = keepIn(fwdSlower * baseSpeed, 2, baseSpeed);
            points.add(new WeightedObservedPoint(weight, i, speed / maxSpeed));
        }

    }


}
