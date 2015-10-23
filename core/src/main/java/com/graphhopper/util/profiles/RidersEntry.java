package com.graphhopper.util.profiles;

import java.io.Serializable;
import java.lang.IllegalArgumentException;
import java.lang.String;

public class RidersEntry implements Serializable{

    private float speed = 0;
    private float distance = 0;

    public RidersEntry(){};

    public void updateEntry(double speed, double distance) {

        this.distance += distance;

        if(this.distance == 0)
            throw new IllegalArgumentException("Cannot update RidersEntry with distance equal to ZERO.");

        this.speed = (float) ((this.speed * (this.distance - distance) + speed * distance) / this.distance);
    }

    @Override
    public String toString() {
        return "[s=" + speed + ", d=" + distance + "]";
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }
}
