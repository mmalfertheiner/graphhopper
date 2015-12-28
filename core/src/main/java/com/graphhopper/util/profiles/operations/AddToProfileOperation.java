package com.graphhopper.util.profiles.operations;


import com.graphhopper.GraphHopper;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.profiles.GPXDataExtractor;
import com.graphhopper.util.profiles.ProfileRepository;
import com.graphhopper.util.profiles.RidersProfile;
import com.graphhopper.matching.GPXFile;

public class AddToProfileOperation implements Operation {

    String name;
    String gpxFileName;

    GraphHopper hopper;

    public AddToProfileOperation(String[] args) {
        name = args[1];
        gpxFileName = args[2];

        CmdArgs cmdArgs = CmdArgs.read(args);
        cmdArgs = CmdArgs.readFromConfigAndMerge(cmdArgs, "config", "graphhopper.config");
        hopper = new GraphHopper().init(cmdArgs);
        hopper.importOrLoad();

    }

    @Override
    public void run() {

        ProfileRepository profileRepository = new ProfileRepository();

        RidersProfile ridersProfile = profileRepository.getProfile(name);

        if(ridersProfile == null) {
            System.err.println("Could not load profile " + name + ". Please check your profile name.");
            return;
        }

        GPXDataExtractor gpxDataExtractor = new GPXDataExtractor();
        gpxDataExtractor.setupMapMatching(hopper);
        gpxDataExtractor.configFilter(GPXDataExtractor.FILTER_KALMAN_COMBINED, 100);

        GPXFile gpxFile = new GPXFile().doImport("./" + gpxFileName);
        gpxDataExtractor.setFile(gpxFile);
        ridersProfile.update(gpxDataExtractor.extract());

        profileRepository.saveProfile(name);

    }
}
