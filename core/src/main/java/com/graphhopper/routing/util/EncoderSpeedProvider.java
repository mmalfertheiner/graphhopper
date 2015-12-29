package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;

import static com.graphhopper.util.Helper.keepIn;

public class EncoderSpeedProvider implements SpeedProvider {

    protected FlagEncoder encoder;

    public EncoderSpeedProvider(FlagEncoder encoder){
        this.encoder = encoder;
    }

    @Override
    public double calcSpeed(EdgeIteratorState edgeIteratorState, boolean reverse) {
        double speed = encoder.getSpeed(edgeIteratorState.getFlags());

        if (speed == 0)
            return 0;

        return adjustSpeed(speed, edgeIteratorState, reverse);
    }

    protected double adjustSpeed(double speed, EdgeIteratorState edgeState, boolean reverse) {

        double incElevation = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY) / 100;
        double decElevation = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY) / 100;
        double incDistPercentage = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;

        double adjustedSpeed = speed;

        if(reverse)
        {
            incElevation = decElevation;
            decElevation = encoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY) / 100;
            incDistPercentage = 1.0 - incDistPercentage;
        }


        // use weighted mean so that longer incline infuences speed more than shorter
        double fwdFaster = 1 + 30 * keepIn(decElevation, 0, 0.2);
        fwdFaster = Math.sqrt(fwdFaster);
        double fwdSlower = 1 - 5 * keepIn(incElevation, 0, 0.2);
        fwdSlower = fwdSlower * fwdSlower;
        double incDist2DSum = edgeState.getDistance() * incDistPercentage;
        double decDist2DSum = edgeState.getDistance() - incDist2DSum;
        adjustedSpeed = keepIn(speed * (fwdSlower * incDist2DSum + fwdFaster * decDist2DSum) / edgeState.getDistance(), BikeGenericFlagEncoder.PUSHING_SECTION_SPEED / 2, 50);

        return adjustedSpeed;
    }

}
