package com.graphhopper.routing.util;


import com.graphhopper.util.profiles.ProfileManager;

public class ProfilePreferenceProvider extends GenericPreferenceProvider {

    ProfileManager profileManager;

    public ProfilePreferenceProvider(ProfileManager profileManager){
        this.profileManager = profileManager;
    }

    @Override
    public int calcWayTypePreference(int wayType) {
        int preference = 0;

        //Special treatment for nogos and bike tracks
        if(wayType == 0 || wayType == 15)
            return -4;
        else if(wayType == 13 || wayType == 14)
            return 3;

        if(profileManager.getWayTypePreference(wayType) >= 0.5)
            preference = 2;
        else if(profileManager.getWayTypePreference(wayType) >= 0.2)
            preference = 1;
        else if(profileManager.getWayTypePreference(wayType) >= 0.05)
            preference = 0;
        else
            preference = -1;

        return preference;
    }

    @Override
    public int calcSurfacePreference(boolean pavedSurface) {
        int preference = 0;

        if(profileManager.prefersPavedSurface() && !pavedSurface)
            preference = -2;
        else if(!profileManager.prefersPavedSurface() && pavedSurface)
            preference = -2;

        //System.out.println("PREFERENCE: " + preference + ", profileManager: " + profileManager.prefersPavedSurface());

        return preference;
    }

}
