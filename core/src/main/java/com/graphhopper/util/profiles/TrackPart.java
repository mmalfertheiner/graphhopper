package com.graphhopper.util.profiles;

import com.graphhopper.util.GPXEntry;

import java.util.List;

public class TrackPart {

    private double distance;
    private List<GPXEntry> gpxEntryList;
    private double slope;
    private double speed;
    private int wayType = -1;

    public TrackPart(List<GPXEntry> gpxEntryList, double distance, double slope, double speed) {
        this.gpxEntryList = gpxEntryList;
        this.distance = distance;
        this.slope = slope;
        this.speed = speed;
    }

    public List<GPXEntry> getGpxEntryList() {
        return gpxEntryList;
    }

    public void setGpxEntryList(List<GPXEntry> gpxEntryList) {
        this.gpxEntryList = gpxEntryList;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getWayType() {
        return wayType;
    }

    public void setWayType(int wayType) {
        this.wayType = wayType;
    }
}
