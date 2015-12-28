package com.graphhopper.routing.util;


public interface PreferenceProvider {

    int calcWayTypePreference(int wayType);

    int calcSurfacePreference(boolean pavedSurface);

    int calcSlopePreference(int wayType, double incSlope, double incDist, double decSlope, double decDist);

}