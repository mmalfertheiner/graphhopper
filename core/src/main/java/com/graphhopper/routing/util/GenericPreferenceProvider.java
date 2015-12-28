package com.graphhopper.routing.util;


public class GenericPreferenceProvider implements PreferenceProvider {

    @Override
    public int calcWayTypePreference(int wayType) {
        int preference = 0;

        switch (wayType) {

            case 0:
                preference = -4;
                break;
            case 1:
                preference = 0;
                break;
            case 2:
                preference = 1;
                break;
            case 3:
                preference = 1;
                break;
            case 4:
                preference = 1;
                break;
            case 5:
                preference = 1;
                break;
            case 6:
                preference = 1;
                break;
            case 7:
                preference = 0;
                break;
            case 8:
                preference = -1;
                break;
            case 9:
                preference = -1;
                break;
            case 10:
                preference = -1;
                break;
            case 11:
                preference = -2;
                break;
            case 12:
                preference = -2;
                break;
            case 13:
                preference = 3;
                break;
            case 14:
                preference = 3;
                break;
            case 15:
                preference = -4;
                break;
        }

        return preference;
    }

    @Override
    public int calcSurfacePreference(boolean pavedSurface) {
        int preference = -2;

        //Paved surface way types
        if(pavedSurface)
            preference = 0;

        return preference;
    }

    @Override
    public int calcSlopePreference(int wayType, double incSlope, double incDist, double decSlope, double decDist) {
        int preference = 0;

        if(wayType >= 8 && wayType <=12){
            if(incDist > 10 && incSlope > 0.03) {
                preference = -2;
            }

            if(decDist > 10 && decSlope > 0.2) {
                preference = -2;
            }
        }

        if(incDist > 10 && incSlope > 0.2) {
            preference = -2;
        }

        return preference;
    }
}
