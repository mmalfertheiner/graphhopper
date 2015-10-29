package com.graphhopper.util.profiles.operations;

import com.graphhopper.util.profiles.ProfileManager;

public class CreateProfileOperation implements Operation {

    String name;

    public CreateProfileOperation(String[] args){
        this.name = args[1];
    }

    @Override
    public void run() {
        ProfileManager aProfileManager = new ProfileManager();
        aProfileManager.createProfile(name);
    }
}
