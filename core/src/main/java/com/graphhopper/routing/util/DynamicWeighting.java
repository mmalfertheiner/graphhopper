/*
 *  Licensed to Peter Karich under one or more contributor license
 *  agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  Peter Karich licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the
 *  License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import com.graphhopper.util.profiles.ProfileManager;
import com.graphhopper.util.profiles.RidersProfile;

import java.util.Map;

import static com.graphhopper.util.Helper.keepIn;

/**
 * Special weighting for (motor)bike
 * <p>
 * @author Peter Karich
 */
public class DynamicWeighting implements Weighting
{

    protected final static double SPEED_CONV = 3.6;
    final static double DEFAULT_HEADING_PENALTY = 300; //[s]
    private final double heading_penalty;
    protected final FlagEncoder flagEncoder;
    private final double maxSpeed;
    private Map<Integer, double[]> userSpeeds;

    /**
     * For now used only in BikeGenericFlagEncoder
     */
    public static final int PRIORITY_KEY = 101;
    public static final int INC_SLOPE_KEY = 102;
    public static final int DEC_SLOPE_KEY = 103;
    public static final int INC_DIST_PERCENTAGE_KEY = 104;
    public static final int WAY_TYPE_KEY = 105;


    public DynamicWeighting(FlagEncoder encoder, PMap pMap)
    {
        if (!encoder.isRegistered())
            throw new IllegalStateException("Make sure you add the FlagEncoder " + encoder + " to an EncodingManager before using it elsewhere");

        this.flagEncoder = encoder;
        heading_penalty = pMap.getDouble("heading_penalty", DEFAULT_HEADING_PENALTY);
        String user = pMap.get("profile", "");
        maxSpeed = encoder.getMaxSpeed() / SPEED_CONV;

        RidersProfile profile = new ProfileManager().getProfile(user);
        if(profile != null)
            userSpeeds = profile.getFilterSpeeds();

    }

    public DynamicWeighting(FlagEncoder encoder)
    {
        this(encoder, new PMap(0));
    }

    @Override
    public double calcWeight( EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId )
    {
        double speed = getUserSpeed(edgeState, reverse);

        if (speed == 0)
            return Double.POSITIVE_INFINITY;

        double time = edgeState.getDistance() / speed * SPEED_CONV;

        // add direction penalties at start/stop/via points
        boolean penalizeEdge = edgeState.getBoolean(EdgeIteratorState.K_UNFAVORED_EDGE, reverse, false);
        if (penalizeEdge)
            time += heading_penalty;

        return time / (0.5 + getUserPreference(edgeState));
    }

    private double getUserSpeed(EdgeIteratorState edgeState, boolean reverse){

        double speed = 0;
        int wayType = (int) flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.WAY_TYPE_KEY);

        if(userSpeeds != null){
            double[] speeds = userSpeeds.get(wayType);
            if(speeds != null){

                int incElevation = (int)flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY);
                int decElevation = (int)flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY);
                double incDistPercentage = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;

                double incSpeed = speeds[RidersProfile.SLOPES / 2 + incElevation];
                double decSpeed = speeds[RidersProfile.SLOPES / 2 - decElevation];

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
            speed = flagEncoder.getSpeed(edgeState.getFlags());

            if (speed == 0)
                return Double.POSITIVE_INFINITY;

            speed = adjustSpeed(speed, edgeState, reverse);
        }

        return speed;
    }


    private double getUserPreference(EdgeIteratorState edgeState) {

        int wayType = (int) flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.WAY_TYPE_KEY);
        int priority = PriorityCode.UNCHANGED.getValue();

        double incElevation = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY) / 100;
        double incDistPercentage = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;
        double incDist2DSum = edgeState.getDistance() * incDistPercentage;

        if(wayType == 13 || wayType == 14)
            priority = PriorityCode.BEST.getValue();
        else if(wayType >= 10 && wayType <= 12) {

            priority = PriorityCode.AVOID_IF_POSSIBLE.getValue();

            if(incDist2DSum > 10 && incElevation > 0.02) {
                priority = PriorityCode.AVOID_AT_ALL_COSTS.getValue();
                //System.out.println(wayType + ": elevation: " + incElevation + ": " + incDist2DSum);

                if(incElevation > 0.1){
                    priority = PriorityCode.WORST.getValue();
                }
            }
        } else if (wayType >= 2 && wayType <= 6){
            priority = PriorityCode.PREFER.getValue();
        } else if (wayType == 15){
            priority = PriorityCode.WORST.getValue();
        }

        return (double) priority / PriorityCode.BEST.getValue();

    }

    private double adjustSpeed(double speed, EdgeIteratorState edgeState, boolean reverse) {

        double incElevation = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY) / 100;
        double decElevation = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY) / 100;
        double incDistPercentage = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;

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

    @Override
    public double getMinWeight(double distance) {
        return distance / maxSpeed;
    }

    @Override
    public FlagEncoder getFlagEncoder()
    {
        return flagEncoder;
    }

    @Override
    public String toString()
    {
        return "DYNAMIC|" + flagEncoder;
    }
}
