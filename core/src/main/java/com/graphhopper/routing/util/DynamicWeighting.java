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

import java.util.Set;

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
    protected SpeedProvider speedProvider;
    protected PreferenceProvider preferenceProvider;
    protected ProfileManager profileManager;

    /**
     * For now used only in BikeGenericFlagEncoder
     */
    public static final int INC_SLOPE_KEY = 102;
    public static final int DEC_SLOPE_KEY = 103;
    public static final int INC_DIST_PERCENTAGE_KEY = 104;
    public static final int WAY_TYPE_KEY = 105;


    public DynamicWeighting(FlagEncoder encoder, PMap pMap, ProfileManager profileManager)
    {
        if (!encoder.isRegistered())
            throw new IllegalStateException("Make sure you add the FlagEncoder " + encoder + " to an EncodingManager before using it elsewhere");

        this.flagEncoder = encoder;
        heading_penalty = pMap.getDouble("heading_penalty", DEFAULT_HEADING_PENALTY);

        this.profileManager = profileManager;

        if(profileManager != null && profileManager.hasProfile()) {
            this.speedProvider = new ProfileSpeedProvider(encoder, profileManager);
            this.preferenceProvider = new ProfilePreferenceProvider(profileManager);
        } else {
            this.speedProvider = new EncoderSpeedProvider(encoder);
            this.preferenceProvider = new GenericPreferenceProvider();
        }
    }

    public DynamicWeighting(FlagEncoder encoder)
    {
        this(encoder, new PMap(0), null);
    }

    @Override
    public double calcWeight( EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId )
    {
        double speed = speedProvider.calcSpeed(edgeState, reverse);

        if (speed == 0)
            return Double.POSITIVE_INFINITY;

        double time = edgeState.getDistance() / speed * SPEED_CONV;

        // add direction penalties at start/stop/via points
        boolean penalizeEdge = edgeState.getBoolean(EdgeIteratorState.K_UNFAVORED_EDGE, reverse, false);
        if (penalizeEdge)
            time += heading_penalty;

        return time / Math.pow((0.5 + getEdgePreference(edgeState, reverse)), 2);
    }

    protected double getEdgePreference(EdgeIteratorState edgeState, boolean reverse) {

        int wayType = (int) flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.WAY_TYPE_KEY);
        int priority = PriorityCode.UNCHANGED.getValue();
        double incDistPercentage = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_DIST_PERCENTAGE_KEY) / 100;
        boolean pavedSurface = ((wayType >= 1 && wayType <= 4) || wayType == 7 || wayType == 13);

        double incSlope;
        double incDist2DSum;
        double decSlope;
        double decDist2DSum;

        if(reverse){
            incSlope = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY) / 100;
            decSlope = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY) / 100;
            incDist2DSum = (1 - incDistPercentage) * edgeState.getDistance();
            decDist2DSum = edgeState.getDistance() - incDist2DSum;
        } else {
            incSlope = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.INC_SLOPE_KEY) / 100;
            decSlope = flagEncoder.getDouble(edgeState.getFlags(), DynamicWeighting.DEC_SLOPE_KEY) / 100;
            incDist2DSum = edgeState.getDistance() * incDistPercentage;
            decDist2DSum = edgeState.getDistance() - incDist2DSum;
        }

        priority += preferenceProvider.calcWayTypePreference(wayType);
        priority += preferenceProvider.calcSurfacePreference(pavedSurface);
        priority += preferenceProvider.calcSlopePreference(wayType, incSlope, incDist2DSum, decSlope, decDist2DSum);

        System.out.println("WAYTYPE: " + wayType + ", INC SLOPE: " + incSlope + ", DEC SLOPE: " + decSlope +", PRIORITY: " + Helper.keepIn(priority, PriorityCode.WORST.getValue(), PriorityCode.BEST.getValue()));

        return Helper.keepIn(priority, PriorityCode.WORST.getValue(), PriorityCode.BEST.getValue()) / PriorityCode.BEST.getValue();

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
