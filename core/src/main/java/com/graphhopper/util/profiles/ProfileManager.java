package com.graphhopper.util.profiles;


import com.graphhopper.routing.util.BikeGenericFlagEncoder;
import com.graphhopper.routing.util.FlagEncoder;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    public ProfileManager init(String name, FlagEncoder flagEncoder){
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

        if(bestFit > 0)
            return userSpeeds.get(bestFit)[slopeIndex] * (flagEncoder.getWayTypeSpeed(wayType) / flagEncoder.getWayTypeSpeed(bestFit));

        return Double.NaN;

    }

    private double[] filterSpeeds(RidersEntry[] ridersEntries, int wayType, FlagEncoder flagEncoder) {

        ArrayList<WeightedObservedPoint> points = new ArrayList<WeightedObservedPoint>();

        double maxSpeed = ridersProfile.maxSpeed(wayType, flagEncoder.getMaxSpeed());

        for ( int i = 0; i < ridersEntries.length; i++){
            if(ridersEntries[i] != null) {
                points.add(new WeightedObservedPoint(ridersEntries[i].getDistance(), i - RidersProfile.SLOPES / 2, ridersEntries[i].getSpeed() / maxSpeed));
            }
        }

        final double[] coef = new SigmoidalFitter(new double[]{1, 0.5, -1}).fit(points);
        SigmoidFunction sigF = new SigmoidFunction();

        double[] result = new double[RidersProfile.SLOPES + 1];

        int offset = RidersProfile.SLOPES / 2;

        for( int i = - offset; i < offset + 1; i++){
            result[i + offset] = sigF.value(i, coef) * maxSpeed;
        }

        return result;
    }


}
