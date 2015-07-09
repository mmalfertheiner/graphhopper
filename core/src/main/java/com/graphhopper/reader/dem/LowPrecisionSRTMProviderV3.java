package com.graphhopper.reader.dem;

import java.io.File;
import java.io.IOException;

public class LowPrecisionSRTMProviderV3 extends SRTMProvider
{

    public static void main( String[] args ) throws IOException
    {
        SRTMProvider provider = new LowPrecisionSRTMProviderV3();
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
    }

    public LowPrecisionSRTMProviderV3()
    {
        WIDTH = 1201;
        baseUrl = "http://e4ftl01.cr.usgs.gov//MODV6_Dal_D/SRTM/SRTMGL3.003/2000.02.11/";
        setCacheDir(new File("/tmp/srtm3degV3"));
    }

    String getFileString( double lat, double lon )
    {
        int minLat = Math.abs(down(lat));
        int minLon = Math.abs(down(lon));

        return String.format("%s%02d%s%03d.SRTMGL3",
                (lat >= 0) ? "N" : "S",
                minLat,
                (lon >= 0) ? "E" : "W",
                minLon
        );
    }

    @Override
    public String toString()
    {
        return "SRTM3V3";
    }
}