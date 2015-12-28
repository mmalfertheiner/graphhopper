package com.graphhopper.util.profiles;


import java.io.Serializable;
import java.util.List;

public class RidersProfile implements Serializable{

    public transient final static int WAY_TYPES = 16;
    public transient final static int SLOPES = 60;

    private RidersEntry[][] speedMatrix = new RidersEntry[WAY_TYPES][SLOPES+1]; // 16 Way types, Steigung von -30 % bis + 30 %

    public RidersProfile(){};

    public RidersEntry getEntry(int wayType, int slope){
        return speedMatrix[wayType][slope+(SLOPES/2)];
    }

    public RidersEntry[] getEntries(int wayType) {
        return speedMatrix[wayType];
    }

    public double maxSpeed(int wayType, double wayTypeSpeed){

        double max = wayTypeSpeed * 2;

        for (RidersEntry r : speedMatrix[wayType]){
            if(r != null && r.getSpeed() > max)
                max = r.getSpeed();
        }

        return max;
    }


    public double getSpeed(int wayType, int slope){

        RidersEntry entry = getEntry(wayType, slope);

        if(entry == null)
            return Double.NaN;

        return getEntry(wayType, slope).getSpeed();
    }

    public double getDistance(int wayType, int slope){

        RidersEntry entry = getEntry(wayType, slope);

        if(entry == null)
            return 0.0;

        return getEntry(wayType, slope).getDistance();
    }


    public void update(TrackPart trackPart) {

        if(trackPart == null)
            return;

        int wayType = (int) trackPart.getWayType();
        int slope = (int) trackPart.getSlope();

        if (wayType < 0 || wayType > 15)
            throw new IllegalArgumentException("Waytype must be between 0 and 15, but was: " + wayType);

        if (slope < - (SLOPES/2))
            slope = - (SLOPES/2);

        if (slope > SLOPES/2)
            slope = SLOPES/2;

        RidersEntry ridersEntry = speedMatrix[wayType][slope+(SLOPES/2)];

        if(ridersEntry == null) {
            ridersEntry = new RidersEntry();
            speedMatrix[wayType][slope+(SLOPES/2)] = ridersEntry;
        }

        ridersEntry.updateEntry(trackPart.getSpeed(), trackPart.getDistance());

    }

    public int update(List<TrackPart> trackPartList) {

        int skipped = 0;

        for(TrackPart tp : trackPartList) {
            try{
                update(tp);
            } catch(IllegalArgumentException i){
                skipped++;
            }
        }

        return skipped;

    }

    @Override
    public java.lang.String toString() {

        String riderString = "";

        for (int i = 0; i < speedMatrix.length; i++) {
            riderString += "-----[ " + i + " ]------ \n";
            for (int j = 0; j < speedMatrix[i].length; j++) {
                if(speedMatrix[i][j] != null)
                    riderString += (j - SLOPES / 2) + ", " + speedMatrix[i][j].getSpeed() + ", " + speedMatrix[i][j].getDistance() + "\n";
            }
            riderString += "----------------------\n";
        }

        return riderString;
    }

}
