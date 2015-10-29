package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.profiles.RidersProfile;

import java.util.Map;

import static com.graphhopper.util.Helper.keepIn;

public class ProfileSpeedProvider implements SpeedProvider {

    protected final static double SPEED_CONV = 3.6;
    protected final FlagEncoder encoder;
    private final double maxSpeed;
    private Map<Integer, double[]> userSpeeds;


    public ProfileSpeedProvider(FlagEncoder flagEncoder, RidersProfile ridersProfile){
        this.encoder = flagEncoder;
        maxSpeed = encoder.getMaxSpeed() / SPEED_CONV;

        if(ridersProfile != null)
            this.userSpeeds = ridersProfile.getFilterSpeeds();
    }

    @Override
    public double calcSpeed(EdgeIteratorState edgeIteratorState, boolean reverse) {
        return getUserSpeed(edgeIteratorState, reverse);
    }

    private double getUserSpeed(EdgeIteratorState edgeState, boolean reverse){

        double speed = 0;
        int wayType = (int) encoder.getDouble(edgeState.getFlags(), DynamicWeighting.WAY_TYPE_KEY);

        if(userSpeeds != null){
            double[] speeds = userSpeeds.get(wayType);
            if(speeds != null){

                int incElevation = (int)encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY);
                int decElevation = (int)encoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY);
                double incDistPercentage = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;

                int incIndex = incElevation > RidersProfile.SLOPES / 2 ? RidersProfile.SLOPES : RidersProfile.SLOPES / 2 + incElevation;
                int decIndex = decElevation > RidersProfile.SLOPES / 2 ? 0 : RidersProfile.SLOPES / 2 - decElevation;

                double incSpeed = speeds[incIndex];
                double decSpeed = speeds[decIndex];

                double incDist2DSum = edgeState.getDistance() * incDistPercentage;
                double decDist2DSum = edgeState.getDistance() - incDist2DSum;

                if (!reverse)
                {
                    speed = keepIn((incSpeed * incDist2DSum + decSpeed * decDist2DSum) / edgeState.getDistance(), BikeGenericFlagEncoder.PUSHING_SECTION_SPEED / 2, 50);
                } else {
                    speed = keepIn((decSpeed * incDist2DSum + incSpeed * decDist2DSum) / edgeState.getDistance(), BikeGenericFlagEncoder.PUSHING_SECTION_SPEED / 2, 50);
                }

            }

        }

        if(speed == 0){
            speed = encoder.getSpeed(edgeState.getFlags());

            if (speed == 0)
                return 0;

            speed = adjustSpeed(speed, edgeState, reverse);
        }

        return speed;
    }

    private double adjustSpeed(double speed, EdgeIteratorState edgeState, boolean reverse) {

        double incElevation = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY) / 100;
        double decElevation = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY) / 100;
        double incDistPercentage = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;

        double incDist2DSum = edgeState.getDistance() * incDistPercentage;
        double decDist2DSum = edgeState.getDistance() - incDist2DSum;

        double adjustedSpeed = speed;

        if (!reverse)
        {
            // use weighted mean so that longer incline infuences speed more than shorter
            double fwdFaster = 1 + 30 * keepIn(decElevation, 0, 0.1);
            fwdFaster = Math.sqrt(fwdFaster);
            double fwdSlower = 1 - 5 * keepIn(incElevation, 0, 0.2);
            fwdSlower = fwdSlower * fwdSlower;
            adjustedSpeed = keepIn(speed * (fwdSlower * incDist2DSum + fwdFaster * decDist2DSum) / edgeState.getDistance(), BikeGenericFlagEncoder.PUSHING_SECTION_SPEED / 2, 50);
        } else {
            double fwdFaster = 1 + 30 * keepIn(incElevation, 0, 0.1);
            fwdFaster = Math.sqrt(fwdFaster);
            double fwdSlower = 1 - 5 * keepIn(decElevation, 0, 0.2);
            fwdSlower = fwdSlower * fwdSlower;
            adjustedSpeed = keepIn(speed * (fwdSlower * decDist2DSum + fwdFaster * incDist2DSum) / edgeState.getDistance(), BikeGenericFlagEncoder.PUSHING_SECTION_SPEED / 2, 50);
        }

        //System.out.println("NEW SPEED: " + Helper.round2(adjustedSpeed) + ", SPEED: " + speed + ", INC ELE: " + incElevation + ", DEC ELE: " + decElevation + ", PERCENTAGE: " + incDistPercentage);

        return adjustedSpeed;
    }

}
