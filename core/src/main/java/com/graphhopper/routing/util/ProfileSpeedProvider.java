package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.profiles.ProfileManager;
import com.graphhopper.util.profiles.RidersProfile;

import static com.graphhopper.util.Helper.keepIn;

public class ProfileSpeedProvider extends EncoderSpeedProvider {

    private ProfileManager profileManager;

    public ProfileSpeedProvider(FlagEncoder flagEncoder, ProfileManager profileManager){
        super(flagEncoder);
        this.profileManager = profileManager;
    }

    @Override
    public double calcSpeed(EdgeIteratorState edgeIteratorState, boolean reverse) {
        return getUserSpeed(edgeIteratorState, reverse);
    }

    private double getUserSpeed(EdgeIteratorState edgeState, boolean reverse){

        double speed = 0;
        int wayType = (int) encoder.getDouble(edgeState.getFlags(), DynamicWeighting.WAY_TYPE_KEY);

        if(profileManager.hasFilteredSpeeds()){

            int incElevation;
            int decElevation;
            double incDistPercentage;

            if(reverse){
                incElevation = (int) encoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY);
                decElevation = (int) encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY);
                incDistPercentage =  1 - (encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100);
            } else {
                incElevation = (int) encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY);
                decElevation = (int) encoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY);
                incDistPercentage = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;
            }

            int incIndex = incElevation > RidersProfile.SLOPES / 2 ? RidersProfile.SLOPES : RidersProfile.SLOPES / 2 + incElevation;
            int decIndex = decElevation > RidersProfile.SLOPES / 2 ? 0 : RidersProfile.SLOPES / 2 - decElevation;

            double incSpeed = profileManager.getSpeedPerSlope(wayType, incIndex, encoder.getSpeed(edgeState.getFlags()), (BikeGenericFlagEncoder) encoder);
            double decSpeed = profileManager.getSpeedPerSlope(wayType, decIndex, encoder.getSpeed(edgeState.getFlags()), (BikeGenericFlagEncoder) encoder);
            double incDist2DSum = edgeState.getDistance() * incDistPercentage;
            double decDist2DSum = edgeState.getDistance() - incDist2DSum;

            speed = keepIn((incSpeed * incDist2DSum + decSpeed * decDist2DSum) / edgeState.getDistance(), BikeGenericFlagEncoder.PUSHING_SECTION_SPEED / 2, 50);

        }

        if(speed == 0){
            speed = super.calcSpeed(edgeState, reverse);
        }

        return speed;
    }

}
