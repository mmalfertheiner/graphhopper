package com.graphhopper.reader.dem;

import com.graphhopper.util.Helper;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LowPrecisionSRTMProvider extends SRTMProvider
{
    private final TIntObjectHashMap<String> areas = new TIntObjectHashMap<String>();

    public static void main( String[] args ) throws IOException
    {
        SRTMProvider provider = new LowPrecisionSRTMProvider();
        // 1046
        System.out.println(provider.getEle(47.468668, 14.575127));
        // 1113
        System.out.println(provider.getEle(47.467753, 14.573911));

        // 1946
        System.out.println(provider.getEle(46.468835, 12.578777));

        // 845
        System.out.println(provider.getEle(48.469123, 9.576393));

        // 1113 vs new:
        provider.setCalcMean(true);
        System.out.println(provider.getEle(47.467753, 14.573911));

        System.out.println(provider.getEle(45.95115, 10.884705));
    }

    public LowPrecisionSRTMProvider()
    {
        WIDTH = 1201;
        baseUrl = "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/";
        // move to explicit calls?
        init();
    }

    /**
     * The URLs are a bit ugly and so we need to find out which area name a certain lat,lon
     * coordinate has.
     */
    private SRTMProvider init()
    {
        try
        {
            String strs[] =
                    {
                            "Africa", "Australia", "Eurasia", "Islands", "North_America", "South_America"
                    };
            for (String str : strs)
            {
                InputStream is = getClass().getResourceAsStream(str + "_names.txt");
                for (String line : Helper.readFile(new InputStreamReader(is, Helper.UTF_CS)))
                {
                    int lat = Integer.parseInt(line.substring(1, 3));
                    if (line.substring(0, 1).charAt(0) == 'S')
                        lat = -lat;

                    int lon = Integer.parseInt(line.substring(4, 7));
                    if (line.substring(3, 4).charAt(0) == 'W')
                        lon = -lon;

                    int intKey = calcIntKey(lat, lon);
                    String key = areas.put(intKey, str);
                    if (key != null)
                        throw new IllegalStateException("do not overwrite existing! key " + intKey + " " + key + " vs. " + str);
                }
            }
            return this;
        } catch (Exception ex)
        {
            throw new IllegalStateException("Cannot load area names from classpath", ex);
        }
    }

    String getFileString( double lat, double lon )
    {
        int intKey = calcIntKey(lat, lon);
        String str = areas.get(intKey);
        if (str == null)
            return null;

        int minLat = Math.abs(down(lat));
        int minLon = Math.abs(down(lon));
        str += "/";
        if (lat >= 0)
            str += "N";
        else
            str += "S";

        if (minLat < 10)
            str += "0";
        str += minLat;

        if (lon >= 0)
            str += "E";
        else
            str += "W";

        if (minLon < 10)
            str += "0";
        if (minLon < 100)
            str += "0";
        str += minLon;
        return str;
    }

    @Override
    public String toString()
    {
        return "SRTM";
    }
}