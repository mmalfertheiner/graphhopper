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
    protected RidersProfile profile;

    /**
     * For now used only in BikeGenericFlagEncoder
     */
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
        profile = new ProfileManager().getProfile(user);

    }

    public DynamicWeighting(FlagEncoder encoder)
    {
        this(encoder, new PMap(0));
    }

    @Override
    public double calcWeight( EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId )
    {
        SpeedProvider speedProvider = new ProfileSpeedProvider(flagEncoder, profile);

        double speed = speedProvider.calcSpeed(edgeState, reverse);

        if (speed == 0)
            return Double.POSITIVE_INFINITY;

        double time = edgeState.getDistance() / speed * SPEED_CONV;

        // add direction penalties at start/stop/via points
        boolean penalizeEdge = edgeState.getBoolean(EdgeIteratorState.K_UNFAVORED_EDGE, reverse, false);
        if (penalizeEdge)
            time += heading_penalty;

        return time / (0.5 + Math.pow(getUserPreference(edgeState), 2));
    }

    private double getUserPreference(EdgeIteratorState edgeState) {

        int wayType = (int) flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.WAY_TYPE_KEY);
        int priority = PriorityCode.UNCHANGED.getValue();

        double incElevation = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY) / 100;
        double incDistPercentage = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;
        double incDist2DSum = edgeState.getDistance() * incDistPercentage;

        if(wayType == 0)
            priority = PriorityCode.AVOID_AT_ALL_COSTS.getValue();
        else if(wayType == 13 || wayType == 14)
            priority = PriorityCode.BEST.getValue();
        else if(wayType == 10) {

            priority = PriorityCode.REACH_DEST.getValue();

            if(incDist2DSum > 10 && incElevation > 0.02) {
                priority = PriorityCode.WORST.getValue();
            }

        } else if (wayType == 11 || wayType == 12) {
            //Should be considered only for Downhill racers
            priority = PriorityCode.WORST.getValue();
        } else if (wayType >= 2 && wayType <= 4){
            priority = PriorityCode.PREFER.getValue();
        } else if (wayType == 15){
            priority = PriorityCode.WORST.getValue();
        }

        return (double) priority / PriorityCode.BEST.getValue();

    }

    @Override
    public double getMinWeight(double distance) {
        return distance / flagEncoder.getMaxSpeed();
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