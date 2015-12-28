package com.graphhopper.util.profiles;


import com.graphhopper.GraphHopper;
import com.graphhopper.matching.*;

import com.graphhopper.reader.dem.HighPrecisionSRTMProvider;
import com.graphhopper.routing.util.BikeGenericFlagEncoder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GPXDataExtractor {

    private final DistanceCalc distCalc = Helper.DIST_EARTH;

    public static final int FILTER_KALMAN_FORWARD = 1;
    public static final int FILTER_KALMAN_BACKWARD = 2;
    public static final int FILTER_KALMAN_COMBINED = 3;
    public static final int FILTER_MEAN = 4;

    private List<GPXEntry> inputGPXEntries;
    private List<TrackPart> trackParts;
    private MapMatching mapMatching;
    private BikeGenericFlagEncoder encoder;
    private int filterType = FILTER_KALMAN_COMBINED;
    private int filterDistance = 60;

    private double[] distances;
    private double[] elevations;

    public GPXDataExtractor() {}

    public void setFile(GPXFile gpxFile){
        this.inputGPXEntries = gpxFile.getEntries();
        this.distances = new double[inputGPXEntries.size() - 1];
        this.elevations = new double[inputGPXEntries.size()];
    }

    public void configFilter(int filterType, int filterDistance) {
        this.filterType = filterType;
        this.filterDistance = filterDistance;
    }

    public void setupMapMatching(GraphHopper hopper){

        // import OpenStreetMap data
        /*CmdArgs args = CmdArgs.readFromConfig("config", "graphhopper.config");
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(pbfFileName);
        String graphHopperLocation = Helper.pruneFileEnd(pbfFileName) + "-gh";
        hopper.setGraphHopperLocation(graphHopperLocation);
        encoder = new BikeGenericFlagEncoder();

        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.setElevation(true);
        hopper.setElevationProvider(new HighPrecisionSRTMProvider());
        hopper.setCHEnable(false);
        hopper.setPreciseIndexResolution(300);
        hopper.importOrLoad();
        */

        // create MapMatching object, can and should be shared accross threads
        GraphHopperStorage graph = hopper.getGraphHopperStorage();
        LocationIndexMatch locationIndex = new LocationIndexMatch(graph, (LocationIndexTree) hopper.getLocationIndex());
        encoder = (BikeGenericFlagEncoder) hopper.getEncodingManager().getEncoder("genbike");

        mapMatching = new MapMatching(graph, locationIndex, encoder);
        mapMatching.setForceRepair(true);
        mapMatching.setMaxSearchMultiplier(100);

    }

    public boolean isMapMatchingEnabled(){
        return (mapMatching != null && encoder != null);
    }

    public List<TrackPart> extract(){

        if(this.inputGPXEntries == null)
            return Collections.emptyList();

        processGPXData();

        if(isMapMatchingEnabled())
            processOSMData();

        return trackParts;
    }


    private double[] filterMeasurements(final double[] measurements){

        SmoothingFilter smoothingFilter;

        switch(filterType){
            case FILTER_KALMAN_FORWARD:
                smoothingFilter = new SimpleKalmanFilter(SimpleKalmanFilter.FORWARD, 6, distances, filterDistance);
                break;
            case FILTER_KALMAN_BACKWARD:
                smoothingFilter = new SimpleKalmanFilter(SimpleKalmanFilter.BACKWARD, 6, distances, filterDistance);
                break;
            case FILTER_KALMAN_COMBINED:
                smoothingFilter = new SimpleKalmanFilter(SimpleKalmanFilter.COMBINED, 6, distances, filterDistance);
                break;
            case FILTER_MEAN:
                smoothingFilter = new MeanFilter(distances, filterDistance);
                break;
            default:
                smoothingFilter = new SimpleKalmanFilter(SimpleKalmanFilter.FORWARD, 6, distances, filterDistance);
                break;
        }

        return smoothingFilter.smooth(measurements);

    }


    private void processGPXData(){

        // 1. Step: Extract elevation and distance from GPX file

        elevations[0] = inputGPXEntries.get(0).getEle();

        for (int i = 1; i < inputGPXEntries.size(); i++) {

            double firstLat = inputGPXEntries.get(i-1).getLat();
            double firstLon = inputGPXEntries.get(i-1).getLon();
            double secondLat = inputGPXEntries.get(i).getLat();
            double secondLon = inputGPXEntries.get(i).getLon();

            distances[i-1] = distCalc.calcDist(firstLat, firstLon, secondLat, secondLon);
            elevations[i] = inputGPXEntries.get(i).getEle();

        }

        // 2. Step: Filter elevations
        elevations = filterMeasurements(elevations);

        // 3. Step: Split GPX into > 200 m track parts
        List<GPXEntry> gpxEntryList;
        trackParts = new ArrayList<TrackPart>();

        for (int i = 0; i < inputGPXEntries.size(); i++){

            int next = 1;
            gpxEntryList = new ArrayList<GPXEntry>();
            gpxEntryList.add(inputGPXEntries.get(i));
            gpxEntryList.add(inputGPXEntries.get(i + next));

            double distance = distances[i];
            double elevation = elevations[i+next] - elevations[i];

            while(distance < 200 && i+next+1 < inputGPXEntries.size()) {
                next++;
                gpxEntryList.add(inputGPXEntries.get(i+next));
                distance += distances[i+next-1];
                elevation += elevations[i+next] - elevations[i+next-1];
            }

            double slope = elevation / distance * 100;
            distance = Math.sqrt((elevation * elevation) + (distance * distance)); // Update distance to 3D distance
            double speed = distance / ((gpxEntryList.get(gpxEntryList.size() -1).getTime() - gpxEntryList.get(0).getTime()) / 1000) * 3.6;

            trackParts.add(new TrackPart(gpxEntryList, distance, slope, speed));

            if(i+next+1 == inputGPXEntries.size())
                break;

            i = i+next-1;

        }

    }

    private void processOSMData() {

        for (TrackPart tp : trackParts) {

            try {
                MatchResult mr = mapMatching.doWork(tp.getGpxEntryList());
                List<EdgeMatch> matches = mr.getEdgeMatches();

                for (EdgeMatch match : matches) {
                    long flags = match.getEdgeState().getFlags();
                    tp.setWayType((int) encoder.getWayType(flags));
                }
            } catch (Exception e) { // Skip track parts, which could not be matched to an edge in OSM}
            }

        }
    }

}
