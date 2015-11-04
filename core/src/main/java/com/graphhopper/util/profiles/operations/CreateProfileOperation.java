package com.graphhopper.util.profiles.operations;

import com.graphhopper.util.profiles.ProfileRepository;

public class CreateProfileOperation implements Operation {

    String name;

    public CreateProfileOperation(String[] args){
        this.name = args[1];
    }

    @Override
    public void run() {
        ProfileRepository profileRepository = new ProfileRepository();
        profileRepository.createProfile(name);
    }
}
