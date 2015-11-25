/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.util.*;

import java.util.*;

import static com.graphhopper.routing.util.PriorityCode.*;
import static com.graphhopper.util.Helper.keepIn;

/**
 * This generic flag encoder stores for each way its street classification. In order to reason in even more detail.
 * <p>
 * @author Peter Karich
 * @author Nop
 * @author ratrun
 */
public class BikeGenericFlagEncoder extends AbstractFlagEncoder
{

    private final DistanceCalc distCalc = Helper.DIST_EARTH;
    public static final int PUSHING_SECTION_SPEED = 4;

    // Pushing section highways are parts where you need to get off your bike and push it (German: Schiebestrecke)
    protected final HashSet<String> pushingSections = new HashSet<String>();
    protected final HashSet<String> oppositeLanes = new HashSet<String>();
    protected final Set<String> acceptedHighwayTags = new HashSet<String>();

    protected final Set<String> pavedSurfaceTags = new HashSet<String>();
    protected final Set<String> unpavedSurfaceTags = new HashSet<String>();

    private final Map<String, Float> surfaceSpeedFactors = new HashMap<String, Float>();
    private final Map<Integer, Integer> wayTypeSpeeds = new HashMap<Integer, Integer>();
    // convert network tag of bicycle routes into a way route code
    private final Map<String, Integer> bikeNetworkToCode = new HashMap<String, Integer>();
    protected EncodedValue relationCodeEncoder;
    protected EncodedValue wayTypeEncoder;
    //EncodedValue priorityWayEncoder;
    protected EncodedDoubleValue inclineSlopeEncoder;
    protected EncodedDoubleValue declineSlopeEncoder;
    protected EncodedDoubleValue inclineDistancePercentageEncoder;


    // Car speed limit which switches the preference from UNCHANGED to AVOID_IF_POSSIBLE
    private int avoidSpeedLimit;

    // This is the specific bicycle class
    private String specificBicycleClass;

    public BikeGenericFlagEncoder()
    {
        this(5, 2, 0);
    }

    public BikeGenericFlagEncoder( PMap properties )
    {
        this(
                (int) properties.getLong("speedBits", 5),
                properties.getDouble("speedFactor", 2),
                properties.getBool("turnCosts", false) ? 1 : 0
        );
        this.properties = properties;
        this.setBlockFords(properties.getBool("blockFords", true));
    }

    public BikeGenericFlagEncoder( String propertiesStr )
    {
        this(new PMap(propertiesStr));
    }

    protected BikeGenericFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts)
    {
        super(speedBits, speedFactor, maxTurnCosts);
        // strict set, usually vehicle and agricultural/forestry are ignored by cyclists
        restrictions.addAll(Arrays.asList("bicycle", "access"));
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");
        restrictedValues.add("military");

        intendedValues.add("yes");
        intendedValues.add("designated");
        intendedValues.add("official");
        intendedValues.add("permissive");

        oppositeLanes.add("opposite");
        oppositeLanes.add("opposite_lane");
        oppositeLanes.add("opposite_track");

        setBlockByDefault(false);
        potentialBarriers.add("gate");
        // potentialBarriers.add("lift_gate");
        potentialBarriers.add("swing_gate");

        absoluteBarriers.add("stile");
        absoluteBarriers.add("turnstile");

        // make intermodal connections possible but mark as pushing section
        acceptedRailways.add("platform");

        //Paved surface tags -> can be used also by racing bikes
        pavedSurfaceTags.add("paved");
        pavedSurfaceTags.add("asphalt");
        pavedSurfaceTags.add("metal");
        pavedSurfaceTags.add("concrete");
        pavedSurfaceTags.add("concrete:lanes");
        pavedSurfaceTags.add("concrete:plates");

        //Unpaved surface -> should be avoided for racing bikes
        unpavedSurfaceTags.add("sett");
        unpavedSurfaceTags.add("cobblestone");
        unpavedSurfaceTags.add("cobblestone:flattened");
        unpavedSurfaceTags.add("paving_stones");
        unpavedSurfaceTags.add("paving_stones:30");
        unpavedSurfaceTags.add("compacted");
        unpavedSurfaceTags.add("grass_paver");
        unpavedSurfaceTags.add("wood");
        unpavedSurfaceTags.add("unpaved");
        unpavedSurfaceTags.add("gravel");
        unpavedSurfaceTags.add("ground");
        unpavedSurfaceTags.add("dirt");
        unpavedSurfaceTags.add("grass");
        unpavedSurfaceTags.add("earth");
        unpavedSurfaceTags.add("fine_gravel");
        unpavedSurfaceTags.add("ice");
        unpavedSurfaceTags.add("mud");
        unpavedSurfaceTags.add("salt");
        unpavedSurfaceTags.add("sand");

        maxPossibleSpeed = 34;

        setSurfaceSpeedFactor("concrete:lanes", 0.9f);
        setSurfaceSpeedFactor("concrete:plates", 0.9f);
        setSurfaceSpeedFactor("metal", 0.9f);

        setSurfaceSpeedFactor("cobblestone", 1.2f);
        setSurfaceSpeedFactor("cobblestone:flattened", 1.2f);
        setSurfaceSpeedFactor("paving_stones", 1.2f);
        setSurfaceSpeedFactor("paving_stones:30", 1.2f);
        setSurfaceSpeedFactor("compacted", 1.2f);

        setSurfaceSpeedFactor("dirt", 0.8f);
        setSurfaceSpeedFactor("earth", 0.8f);
        setSurfaceSpeedFactor("grass", 0.8f);
        setSurfaceSpeedFactor("grass_paver", 0.8f);
        setSurfaceSpeedFactor("salt", 0.8f);
        setSurfaceSpeedFactor("sand", 0.8f);
        setSurfaceSpeedFactor("ice", 0.5f);
        setSurfaceSpeedFactor("mud", 0.6f);

        setWayTypeSpeed(WayType.MOTORWAY.getValue(), 18);
        setWayTypeSpeed(WayType.ROAD.getValue(), 18);
        setWayTypeSpeed(WayType.TERTIARY_ROAD.getValue(), 18);
        setWayTypeSpeed(WayType.UNCLASSIFIED_PAVED.getValue(), 16);
        setWayTypeSpeed(WayType.UNCLASSIFIED_UNPAVED.getValue(), 12);
        setWayTypeSpeed(WayType.SMALL_WAY_PAVED.getValue(), 16);
        setWayTypeSpeed(WayType.SMALL_WAY_UNPAVED.getValue(), 10);
        setWayTypeSpeed(WayType.TRACK_EASY.getValue(), 12);
        setWayTypeSpeed(WayType.TRACK_MIDDLE.getValue(), 10);
        setWayTypeSpeed(WayType.TRACK_HARD.getValue(), 8);
        setWayTypeSpeed(WayType.PATH_EASY.getValue(), 8);
        setWayTypeSpeed(WayType.PATH_MIDDLE.getValue(), 6);
        setWayTypeSpeed(WayType.PATH_HARD.getValue(), 4);
        setWayTypeSpeed(WayType.CYCLEWAY.getValue(), 18);
        setWayTypeSpeed(WayType.MTB_CYCLEWAY.getValue(), 14);
        setWayTypeSpeed(WayType.PUSHING_SECTION.getValue(), PUSHING_SECTION_SPEED);


        acceptedHighwayTags.add("living_street");
        acceptedHighwayTags.add("steps");
        acceptedHighwayTags.add("cycleway");
        acceptedHighwayTags.add("path");
        acceptedHighwayTags.add("footway");
        acceptedHighwayTags.add("pedestrian");
        acceptedHighwayTags.add("track");
        acceptedHighwayTags.add("service");
        acceptedHighwayTags.add("residential");
        acceptedHighwayTags.add("unclassified");
        acceptedHighwayTags.add("road");
        acceptedHighwayTags.add("trunk");
        acceptedHighwayTags.add("trunk_link");
        acceptedHighwayTags.add("primary");
        acceptedHighwayTags.add("primary_link");
        acceptedHighwayTags.add("secondary");
        acceptedHighwayTags.add("secondary_link");
        acceptedHighwayTags.add("tertiary");
        acceptedHighwayTags.add("tertiary_link");
        acceptedHighwayTags.add("trunk");
        acceptedHighwayTags.add("trunk_link");
        acceptedHighwayTags.add("motorway");
        acceptedHighwayTags.add("motorway_link");

        addPushingSection("footway");
        addPushingSection("pedestrian");
        addPushingSection("steps");

        setAvoidSpeedLimit(81);
    }

    @Override
    public int getVersion()
    {
        return 1;
    }

    @Override
    public int defineWayBits( int index, int shift )
    {
        // first two bits are reserved for route handling in superclass
        shift = super.defineWayBits(index, shift);
        speedEncoder = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, getWayTypeSpeed(14),
                maxPossibleSpeed);
        shift += speedEncoder.getBits();

        // 4 bits to store street classification
        wayTypeEncoder = new EncodedValue("WayType", shift, 4, 1, 0, 15, true);
        shift += wayTypeEncoder.getBits();

        // 6 bits to store incline
        inclineSlopeEncoder = new EncodedDoubleValue("InclineSlope", shift, 6, 1, 0, 40, true);
        shift += inclineSlopeEncoder.getBits();

        // 6 bits to store decline
        declineSlopeEncoder = new EncodedDoubleValue("DeclineSlope", shift, 6, 1, 0, 40, true);
        shift += declineSlopeEncoder.getBits();

        // 7 bits to store percentage of inclining distance
        inclineDistancePercentageEncoder = new EncodedDoubleValue("InclineDistancePercentage", shift, 7, 1, 50, 100, true);
        shift += inclineDistancePercentageEncoder.getBits();

        return shift;
    }

    @Override
    public int defineRelationBits( int index, int shift )
    {
        relationCodeEncoder = new EncodedValue("RelationCode", shift, 3, 1, 0, 7);
        return shift + relationCodeEncoder.getBits();
    }

    @Override
    public long reverseFlags( long flags )
    {
        // swap access
        flags = super.reverseFlags(flags);

        // swap slopes
        double incValue = inclineSlopeEncoder.getDoubleValue(flags);
        flags = inclineSlopeEncoder.setDoubleValue(flags, declineSlopeEncoder.getDoubleValue(flags));
        double inclineDistPercentage = 100 - inclineDistancePercentageEncoder.getDoubleValue(flags);
        flags = inclineDistancePercentageEncoder.setDoubleValue(flags, inclineDistPercentage);
        return declineSlopeEncoder.setDoubleValue(flags, incValue);
    }

    @Override
    public long acceptWay( OSMWay way )
    {
        String highwayValue = way.getTag("highway");
        if (highwayValue == null)
        {
            if (way.hasTag("route", ferries))
            {
                // if bike is NOT explictly tagged allow bike but only if foot is not specified
                String bikeTag = way.getTag("bicycle");
                if (bikeTag == null && !way.hasTag("foot") || "yes".equals(bikeTag))
                    return acceptBit | ferryBit;
            }

            // special case not for all acceptedRailways, only platform
            if (way.hasTag("railway", "platform"))
                return acceptBit;

            return 0;
        }

        if (!acceptedHighwayTags.contains(highwayValue))
            return 0;

        // use the way if it is tagged for bikes
        if (way.hasTag("bicycle", intendedValues))
            return acceptBit;

        // accept only if explicitely tagged for bike usage
        if ("motorway".equals(highwayValue) || "motorway_link".equals(highwayValue) || "trunk".equals(highwayValue) || "trunk_link".equals(highwayValue))
            return 0;

        if (way.hasTag("motorroad", "yes"))
            return 0;

        // do not use fords with normal bikes, flagged fords are in included above
        if (isBlockFords() && (way.hasTag("highway", "ford") || way.hasTag("ford")))
            return 0;

        // check access restrictions
        if (way.hasTag(restrictions, restrictedValues))
            return 0;

        // do not accept railways (sometimes incorrectly mapped!)
        if (way.hasTag("railway") && !way.hasTag("railway", acceptedRailways))
            return 0;

        String sacScale = way.getTag("sac_scale");
        if (sacScale != null)
        {
            if (!allowedSacScale(sacScale))
                return 0;
        }
        return acceptBit;
    }

    boolean allowedSacScale( String sacScale )
    {
        // other scales are nearly impossible by an ordinary bike, see http://wiki.openstreetmap.org/wiki/Key:sac_scale
        // Mountainhiking may be possible for downhill racers
        return "hiking".equals(sacScale) || "mountain_hiking".equals(sacScale)
                || "demanding_mountain_hiking".equals(sacScale);
    }

    @Override
    public long handleRelationTags( OSMRelation relation, long oldRelationFlags )
    {
        int code = -1;
        if (relation.hasTag("route", "bicycle"))
        {
            String network = relation.getTag("network");

            if(network == null)
                code = BicycleNetworkCode.UNCLASSIFIED.getValue();
            else {
                if (network.equalsIgnoreCase("icn"))
                    code = BicycleNetworkCode.INTERNATIONAL_CYCLING_NETWORK.getValue();
                else if (network.equalsIgnoreCase("ncn"))
                    code = BicycleNetworkCode.NATIONAL_CYCLING_NETWORK.getValue();
                else if (network.equalsIgnoreCase("rcn"))
                    code = BicycleNetworkCode.REGIONAL_CYCLING_ROUTES.getValue();
                else if (network.equalsIgnoreCase("lcn"))
                    code = BicycleNetworkCode.LOCAL_CYCLING_ROUTES.getValue();
                else if (network.equalsIgnoreCase("deprecated"))
                    code = BicycleNetworkCode.DEPRECATED.getValue();
            }

        } else if (relation.hasTag("route" ,"mtb"))
        {
            code = BicycleNetworkCode.MOUNTAIN_BIKE_ROUTE.getValue();
        } else if (relation.hasTag("route", "ferry"))
        {
            code = BicycleNetworkCode.FERRY.getValue();
        }

        if (code >= 0)
            return relationCodeEncoder.setValue(0, code);
        return oldRelationFlags;
    }

    @Override
    public long handleWayTags( OSMWay way, long allowed, long relationFlags )
    {
        if (!isAccept(allowed))
            return 0;

        long encoded = 0;
        if (!isFerry(allowed))
        {
            encoded = handleBikeRelated(way, encoded, relationFlags);

            double speed = getSpeed(way, encoded);

            // bike maxspeed handling is different from car as we don't increase speed
            speed = applyMaxSpeed(way, speed, false);
            encoded = handleSpeed(way, speed, encoded);

            boolean isRoundabout = way.hasTag("junction", "roundabout");
            if (isRoundabout)
            {
                encoded = setBool(encoded, K_ROUNDABOUT, true);
            }

        } else
        {
            encoded = handleFerryTags(way,
                    getWayTypeSpeed(WayType.SMALL_WAY_UNPAVED.getValue()),
                    getWayTypeSpeed(WayType.SMALL_WAY_PAVED.getValue()),
                    getWayTypeSpeed(WayType.ROAD.getValue()));
            encoded |= directionBitMask;
        }
        /*int priorityFromRelation = 0;
        if (relationFlags != 0)
            priorityFromRelation = (int) relationCodeEncoder.getValue(relationFlags);

        encoded = priorityWayEncoder.setValue(encoded, handlePriority(way, priorityFromRelation));*/
        return encoded;
    }

    int getSpeed( OSMWay way, long flags )
    {
        int speed = PUSHING_SECTION_SPEED;

        int wayType = (int) getWayType(flags);
        String surfaceTag = way.getTag("surface");

        Integer highwaySpeed = getWayTypeSpeed(wayType);

        if(highwaySpeed != null) {

            speed = highwaySpeed;

            if (!Helper.isEmpty(surfaceTag)) {
                Float surfaceSpeedFactor = surfaceSpeedFactors.get(surfaceTag);
                if (surfaceSpeedFactor != null && highwaySpeed != null) {
                    speed = Math.round(surfaceSpeedFactor * highwaySpeed);
                }
            }

            if (way.hasTag("highway", "living_street")) {
                speed = (int) Math.round(highwaySpeed * 0.5);
            }

            if(way.hasTag("highway", "steps")) {
                speed = (int) Math.round(highwaySpeed * 0.5);
            }
        }

        return speed;
    }

    @Override
    public InstructionAnnotation getAnnotation( long flags, Translation tr )
    {
        int wayType = (int) wayTypeEncoder.getValue(flags);
        String wayName = getWayName(wayType, tr);
        return new InstructionAnnotation(0, wayName);
    }

    String getWayName(int wayType, Translation tr )
    {
        String wayTypeName = "";
        switch (wayType)
        {
            case 0:
                wayTypeName = tr.tr("primary");
                break;
            case 1:
                wayTypeName = tr.tr("off_bike");
                break;
            case 2:
                wayTypeName = tr.tr("cycleway");
                break;
            case 3:
                wayTypeName = tr.tr("way");
                break;
        }

        return wayTypeName;
    }


    /**
     * Handle surface and wayType encoding
     */
    long handleBikeRelated( OSMWay way, long encoded, long partOfCycleRelation )
    {
        String surfaceTag = way.getTag("surface");
        String highway = way.getTag("highway");
        String trackType = way.getTag("tracktype");
        String sacScale = way.getTag("sac_scale");
        String smoothness = way.getTag("smoothness");
        String mtbScale = way.getTag("mtb:scale");

        // Populate bits at wayTypeMask with wayType            
        WayType wayType = WayType.SMALL_WAY_PAVED;
        boolean isPushingSection = isPushingSection(way);

        if (isPushingSection && !(partOfCycleRelation > BicycleNetworkCode.FERRY.getValue()) || "steps".equals(highway) || "ice".equals(surfaceTag))
            wayType = WayType.PUSHING_SECTION;
        else if ("motorway".equals(highway) || "motorway_link".equals(highway) || "trunk".equals(highway) || "trunk_link".equals(highway))
            wayType = WayType.MOTORWAY;
        else if ("primary".equals(highway) || "primary_link".equals(highway) || "secondary".equals(highway) || "secondary_link".equals(highway))
            wayType = WayType.ROAD;
        else if ("tertiary".equals(highway) || "tertiary_link".equals(highway))
            wayType = WayType.TERTIARY_ROAD;
        else if ("unclassified".equals(highway)) {
            if (unpavedSurfaceTags.contains(surfaceTag) || (trackType != null && !trackType.equals("grade1")))
                wayType = WayType.UNCLASSIFIED_UNPAVED;
            else
                wayType = WayType.UNCLASSIFIED_PAVED;
        }
        else if ("residential".equals(highway) || "living_street".equals(highway) || "service".equals(highway)){

            if(unpavedSurfaceTags.contains(surfaceTag) || (trackType != null && !trackType.equals("grade1")))
                wayType = WayType.SMALL_WAY_UNPAVED;
            else
                wayType = WayType.SMALL_WAY_PAVED;
        }
        else if ("track".equals(highway)) {
            if(("grade4".equals(trackType) || "grade5".equals(trackType)) && (surfaceTag == null || !pavedSurfaceTags.contains(surfaceTag)))
                wayType = WayType.TRACK_HARD;
            else if (("grade2".equals(trackType) || "grade3".equals(trackType)) && (surfaceTag == null || !pavedSurfaceTags.contains(surfaceTag) && !way.hasTag("bicycle", intendedValues)))
                wayType = WayType.TRACK_MIDDLE;
            else
                wayType = WayType.TRACK_EASY;
        }
        else if ("path".equals(highway)) {
            if("horrible".equals(smoothness) || "very_horrible".equals(smoothness) || "demanding_mountain_hiking".equals(sacScale) || "mountain_hiking".equals(sacScale) || "4".equals(mtbScale) || "5".equals(mtbScale))
                wayType = WayType.PATH_HARD;
            else if("bad".equals(smoothness) || "very_bad".equals(smoothness) || "hiking".equals(sacScale) || "1".equals(mtbScale) || "3".equals(mtbScale) && !pavedSurfaceTags.contains(surfaceTag) && !way.hasTag("bicycle", intendedValues))
                wayType = WayType.PATH_MIDDLE;
            else
                wayType = WayType.PATH_EASY;
        }

        if(partOfCycleRelation == BicycleNetworkCode.MOUNTAIN_BIKE_ROUTE.getValue()){
            wayType = WayType.MTB_CYCLEWAY;
        } else if ("cycleway".equals(highway) || way.hasTag("bicycle", "designated") || (partOfCycleRelation > BicycleNetworkCode.FERRY.getValue()))
        {
            wayType = WayType.CYCLEWAY;
        }

        return wayTypeEncoder.setValue(encoded, wayType.getValue());
    }

    @Override
    public void applyWayTags(OSMWay way, EdgeIteratorState edge) {
        PointList pl = edge.fetchWayGeometry(3);
        if (!pl.is3D())
            throw new IllegalStateException("To support speed calculation based on elevation data it is necessary to enable import of it.");

        long flags = edge.getFlags();

        double fwdIncline = 0;
        double fwdDecline = 0;
        double inclineDistancePercentage = 100;

        if (way.hasTag("tunnel", "yes") || way.hasTag("bridge", "yes") || way.hasTag("highway", "steps"))
        {
            // do not change speed
            // note: although tunnel can have a difference in elevation it is very unlikely that the elevation data is correct for a tunnel
        } else
        {
            // Decrease the speed for ele increase (incline), and decrease the speed for ele decrease (decline). The speed-decrease
            // has to be bigger (compared to the speed-increase) for the same elevation difference to simulate loosing energy and avoiding hills.
            // For the reverse speed this has to be the opposite but again keeping in mind that up+down difference.
            double incEleSum = 0, incDist2DSum = 0;
            double decEleSum = 0, decDist2DSum = 0;
            double prevLat = pl.getLatitude(0), prevLon = pl.getLongitude(0);
            double prevEle = pl.getElevation(0);
            double fullDist2D = 0;

            // get a more detailed elevation information, but due to bad SRTM data this does not make sense now.
            for (int i = 1; i < pl.size(); i++)
            {
                double lat = pl.getLatitude(i);
                double lon = pl.getLongitude(i);
                double ele = pl.getElevation(i);
                double eleDelta = ele - prevEle;
                double dist2D = distCalc.calcDist(prevLat, prevLon, lat, lon);
                if (eleDelta >= 0)
                {
                    incEleSum += eleDelta;
                    incDist2DSum += dist2D;
                } else if (eleDelta < 0)
                {
                    decEleSum += -eleDelta;
                    decDist2DSum += dist2D;
                }
                fullDist2D += dist2D;
                prevLat = lat;
                prevLon = lon;
                prevEle = ele;
            }
            // Calculate slop via tan(asin(height/distance)) but for rather smallish angles where we can assume tan a=a and sin a=a.
            // Then calculate a factor which decreases or increases the speed.
            // Do this via a simple quadratic equation where y(0)=1 and y(0.3)=1/4 for incline and y(0.3)=2 for decline

            if (Double.isInfinite(fullDist2D))
            {
                System.err.println("infinity distance? for way:" + way.getId());
                return;
            }

            // for short edges an incline makes no sense and for 0 distances could lead to NaN values for speed, see #432
            if (fullDist2D < 1)
                return;

            fwdIncline = incDist2DSum > 1 ? (incEleSum / incDist2DSum) * 100 : 0;
            fwdDecline = decDist2DSum > 1 ? (decEleSum / decDist2DSum) * 100 : 0;
            inclineDistancePercentage = keepIn(incDist2DSum / fullDist2D * 100, 0, 100);
        }

        fwdIncline = fwdIncline > 40 ? 40 : fwdIncline;
        fwdDecline = fwdDecline > 40 ? 40 : fwdDecline;

        flags = inclineSlopeEncoder.setDoubleValue(flags, fwdIncline);
        flags = declineSlopeEncoder.setDoubleValue(flags, fwdDecline);
        flags = inclineDistancePercentageEncoder.setDoubleValue(flags, inclineDistancePercentage);

        edge.setFlags(flags);
    }

    @Override
    public double getDouble( long flags, int key )
    {
        switch (key)
        {
            //case DynamicWeighting.PRIORITY_KEY:
            //return (double) priorityWayEncoder.getValue(flags) / BEST.getValue();
            case DynamicWeighting.INC_SLOPE_KEY:
                return inclineSlopeEncoder.getDoubleValue(flags);
            case DynamicWeighting.DEC_SLOPE_KEY:
                return declineSlopeEncoder.getDoubleValue(flags);
            case DynamicWeighting.INC_DIST_PERCENTAGE_KEY:
                return inclineDistancePercentageEncoder.getDoubleValue(flags);
            case DynamicWeighting.WAY_TYPE_KEY:
                return wayTypeEncoder.getValue(flags);
            default:
                return super.getDouble(flags, key);
        }
    }

    boolean isPushingSection( OSMWay way )
    {
        return way.hasTag("highway", pushingSections) || way.hasTag("railway", "platform");
    }

    protected long handleSpeed( OSMWay way, double speed, long encoded )
    {
        encoded = setSpeed(encoded, speed);

        // handle oneways        
        boolean isOneway = way.hasTag("oneway", oneways)
                || way.hasTag("oneway:bicycle", oneways)
                || way.hasTag("vehicle:backward")
                || way.hasTag("vehicle:forward")
                || way.hasTag("bicycle:forward");

        if ((isOneway || way.hasTag("junction", "roundabout"))
                && !way.hasTag("oneway:bicycle", "no")
                && !way.hasTag("bicycle:backward")
                && !way.hasTag("cycleway", oppositeLanes))
        {
            boolean isBackward = way.hasTag("oneway", "-1")
                    || way.hasTag("oneway:bicycle", "-1")
                    || way.hasTag("vehicle:forward", "no")
                    || way.hasTag("bicycle:forward", "no");
            if (isBackward)
                encoded |= backwardBit;
            else
                encoded |= forwardBit;

        } else
        {
            encoded |= directionBitMask;
        }
        return encoded;
    }

    protected enum WayType
    {
        MOTORWAY(0),
        ROAD(1),
        TERTIARY_ROAD(2),
        UNCLASSIFIED_PAVED(3),
        SMALL_WAY_PAVED(4),
        UNCLASSIFIED_UNPAVED(5),
        SMALL_WAY_UNPAVED(6),
        TRACK_EASY(7),
        TRACK_MIDDLE(8),
        TRACK_HARD(9),
        PATH_EASY(10),
        PATH_MIDDLE(11),
        PATH_HARD(12),
        CYCLEWAY(13),
        MTB_CYCLEWAY(14),
        PUSHING_SECTION(15);

        private final int value;

        WayType( int value )
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }

    protected void setWayTypeSpeed(int wayType, int speed)
    {
        wayTypeSpeeds.put(wayType, speed);
    }

    public int getWayTypeSpeed( int wayType )
    {
        return wayTypeSpeeds.get(wayType);
    }

    void setSurfaceSpeedFactor(String surface, float factor)
    {
        surfaceSpeedFactors.put(surface, factor);
    }

    void setCyclingNetworkPreference( String network, int code )
    {
        bikeNetworkToCode.put(network, code);
    }

    void addPushingSection( String highway )
    {
        pushingSections.add(highway);
    }

    @Override
    public boolean supports( Class<?> feature )
    {
        if (super.supports(feature))
            return true;

        return DynamicWeighting.class.isAssignableFrom(feature);
    }

    public void setAvoidSpeedLimit( int limit )
    {
        avoidSpeedLimit = limit;
    }

    public void setSpecificBicycleClass( String subkey )
    {
        specificBicycleClass = "class:bicycle:" + subkey.toString();
    }

    public double getWayType( long flags) {
        return this.wayTypeEncoder.getValue(flags);
    }

    @Override
    public String toString()
    {
        return "genbike";
    }
}
