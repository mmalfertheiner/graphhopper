package com.graphhopper.util.profiles.operations;

import com.graphhopper.util.profiles.ProfileRepository;
import com.graphhopper.util.profiles.RidersProfile;

public class PrintProfileOperation implements Operation {

    String name;

    public PrintProfileOperation(String[] args) {
        this.name = args[1];
    }

    @Override
    public void run() {

        ProfileRepository profileRepository = new ProfileRepository();
        RidersProfile ridersProfile = profileRepository.getProfile(name);

        System.out.println("Profile of: " + name);

        if(ridersProfile != null){
            System.out.println(ridersProfile);
        }

    }
}
