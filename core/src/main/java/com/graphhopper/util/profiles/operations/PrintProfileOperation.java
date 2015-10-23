package com.graphhopper.util.profiles.operations;

import com.graphhopper.util.profiles.ProfileManager;
import com.graphhopper.util.profiles.RidersProfile;

public class PrintProfileOperation implements Operation {

    String name;

    public PrintProfileOperation(String[] args) {
        this.name = args[1];
    }

    @Override
    public void run() {

        ProfileManager profileManager = new ProfileManager();
        RidersProfile ridersProfile = profileManager.getProfile(name);

        System.out.println("Profile of: " + name);

        if(ridersProfile != null){
            System.out.println(ridersProfile.getFilterSpeeds());
        }

    }
}
