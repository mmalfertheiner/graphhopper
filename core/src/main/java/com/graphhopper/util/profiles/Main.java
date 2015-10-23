package com.graphhopper.util.profiles;

import com.graphhopper.util.profiles.operations.AddToProfileOperation;
import com.graphhopper.util.profiles.operations.CreateProfileOperation;
import com.graphhopper.util.profiles.operations.Operation;
import com.graphhopper.util.profiles.operations.PrintProfileOperation;

public class Main {

    public static void main(String[] args) {

        String operationType = args[0];
        Operation op = null;

        System.out.println("Operation: " + args[0]);

        if(operationType.equalsIgnoreCase("create")){
            op = new CreateProfileOperation(args);
        } else if (operationType.equalsIgnoreCase("add")){
            op = new AddToProfileOperation(args);
        } else if (operationType.equalsIgnoreCase("print")){
            op = new PrintProfileOperation(args);
        }

        if(op != null)
            op.run();

        /*
        // import OpenStreetMap data
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile("./" + pbfFileName);
        hopper.setGraphHopperLocation("./cache");
        BikeGenericFlagEncoder encoder = new BikeGenericFlagEncoder();

        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.setElevation(true);
        hopper.setElevationProvider(new HighPrecisionSRTMProvider());
        hopper.setCHEnable(false);
        hopper.setPreciseIndexResolution(300);
        hopper.importOrLoad();

        // create MapMatching object, can and should be shared accross threads
        GraphHopperStorage graph = hopper.getGraphHopperStorage();
        LocationIndexMatch locationIndex = new LocationIndexMatch(graph, (LocationIndexTree) hopper.getLocationIndex());


        MapMatching mapMatching = new MapMatching(graph, locationIndex, encoder);
        mapMatching.setForceRepair(true);
        mapMatching.setMaxSearchMultiplier(100);

        // do the actual matching, get the GPX entries from a file or via stream

        GPXDataExtractor gpxDataExtractor = new GPXDataExtractor();
        gpxDataExtractor.setupMapMatching(mapMatching, encoder);
        gpxDataExtractor.configFilter(GPXDataExtractor.FILTER_KALMAN_COMBINED, 100);

        String[] files = null;

        files = new String[]{ "Garmin_150629_Seis_Seiser_Alm.gpx",
                "Garmin_150629_Seiser_Alm_Seis.gpx",
                "Garmin_150704_Konstantin_Seis.gpx",
                "Garmin_150704_Seis_Konstantin.gpx",
                "Garmin_150706_Ritsch_Kompatsch.gpx",
                "Garmin_150706_Seis_Pufls_Seiser_Alm.gpx",
                "Garmin_150706_Seiser_Alm_Seis.gpx",
                "Garmin_160915_Seis_Fursch.gpx",
                "Garmin_200915_Kastelruth_Seis.gpx",
                "Garmin_200915_Seis_Kastelruth.gpx"};

        //files = new String[]{"Sella_Ronda_-_Ed._2014.gpx", "Sellaronda_hero_2014.gpx" };

        GPXFile gpxFile;
        RidersProfile ridersProfile = new RidersProfile();
        int skippedTrackParts = 0;

        for(int i = 0; i < files.length; i++){
            gpxFile = new GPXFile().doImport("./track-data/" + files[i]);
            gpxDataExtractor.setFile(gpxFile);
            skippedTrackParts += ridersProfile.update(gpxDataExtractor.extract());
        }


        /*int waytype = 0;

        for(int i = -30; i < 30; i++) {

            if(ridersProfile.getEntry(waytype,i) == null)
                continue;

            System.out.println(i + " " + ridersProfile.getEntry(waytype,i).getSpeed());

        }

        //System.out.println(ridersProfile);
        System.out.println(ridersProfile.getTotalDistance());
        System.out.println(ridersProfile.getDistance(0));

        double[] tmp = ridersProfile.getWayTypePriority();
        double totalDist = ridersProfile.getTotalDistance();

        double distancePerWay = tmp[14] * totalDist / 100;

        System.out.println("Skipped: " + skippedTrackParts);



        Map<Integer, double[]> speedsMap = ridersProfile.getFilterSpeeds();

        int waytype = 0;


        for(double[] speeds : speedsMap.values()) {

            System.out.println("-----------------------------");
            int i = -30;

            for (double speed : speeds) {
                System.out.println(i + ", " + speed);
                i++;
            }
        }
        */
    }


    private void setup(){

    }
}
