/*
 *  Licensed to Peter Karich under one or more contributor license
 *  agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  Peter Karich licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the
 *  License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.reader.dem;

import com.graphhopper.storage.*;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.Downloader;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Elevation data from NASA (SRTM). Downloaded from http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/
 * <p>
 * Important information about SRTM: the coordinates of the lower-left corner of tile N40W118 are 40
 * degrees north latitude and 118 degrees west longitude. To be more exact, these coordinates refer
 * to the geometric center of the lower left sample, which in the case of SRTM3 data will be about
 * 90 meters in extent.
 * <p>
 * @author Peter Karich
 */
public abstract class SRTMProvider implements ElevationProvider
{

    private static final BitUtil BIT_UTIL = BitUtil.BIG;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected int WIDTH;
    private Directory dir;
    private DAType daType = DAType.MMAP;
    private Downloader downloader = new Downloader("GraphHopper SRTMReader").setTimeout(10000);
    private File cacheDir = new File("/tmp/srtm");
    // use a map as an array is not quite useful if we want to hold only parts of the world
    protected final TIntObjectHashMap<HeightTile> cacheData = new TIntObjectHashMap<HeightTile>();
    protected final double precision = 1e7;
    protected final double invPrecision = 1 / precision;
    // mirror: base = "http://mirror.ufs.ac.za/datasets/SRTM3/"
    private boolean calcMean = false;
    private boolean calcMedian = false;
    protected String baseUrl;

    @Override
    public void setCalcMean( boolean calcMean )
    {
        this.calcMean = calcMean;
    }

    @Override
    public void setCalcMedian( boolean calcMedian) {
        this.calcMedian = calcMedian;
    }

    // use int key instead of string for lower memory usage
    protected int calcIntKey( double lat, double lon )
    {
        // we could use LinearKeyAlgo but this is simpler as we only need integer precision:
        return (down(lat) + 90) * 1000 + down(lon) + 180;
    }

    public void setDownloader( Downloader downloader )
    {
        this.downloader = downloader;
    }

    @Override
    public ElevationProvider setCacheDir( File cacheDir )
    {
        if (cacheDir.exists() && !cacheDir.isDirectory())
            throw new IllegalArgumentException("Cache path has to be a directory");

        try
        {
            this.cacheDir = cacheDir.getCanonicalFile();
        } catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
        return this;
    }

    @Override
    public ElevationProvider setBaseURL( String baseUrl )
    {
        if (baseUrl == null || baseUrl.isEmpty())
            throw new IllegalArgumentException("baseUrl cannot be empty");

        this.baseUrl = baseUrl;
        return this;
    }

    @Override
    public ElevationProvider setDAType( DAType daType )
    {
        this.daType = daType;
        return this;
    }

    int down( double val )
    {
        int intVal = (int) val;
        if (val >= 0 || intVal - val < invPrecision)
            return intVal;
        return intVal - 1;
    }

    abstract String getFileString(double lat, double lon);

    @Override
    public double getEle( double lat, double lon )
    {
        lat = (int) (lat * precision) / precision;
        lon = (int) (lon * precision) / precision;
        int intKey = calcIntKey(lat, lon);
        HeightTile demProvider = cacheData.get(intKey);
        if (demProvider == null)
        {
            if (!cacheDir.exists())
                cacheDir.mkdirs();

            String fileDetails = getFileString(lat, lon);
            if (fileDetails == null)
                return 0;

            int minLat = down(lat);
            int minLon = down(lon);
            demProvider = new HeightTile(minLat, minLon, WIDTH, precision, 1);
            demProvider.setCalcMean(calcMean);
            demProvider.setCalcMedian(calcMedian);
            cacheData.put(intKey, demProvider);
            DataAccess heights = getDirectory().find("dem" + intKey);
            demProvider.setHeights(heights);
            boolean loadExisting = false;

            try
            {
                loadExisting = heights.loadExisting();
            } catch (Exception ex)
            {
                logger.warn("cannot load dem" + intKey + ", error:" + ex.getMessage());
            }

            if (!loadExisting)
            {
                byte[] bytes = new byte[2 * WIDTH * WIDTH];
                heights.create(bytes.length);
                try
                {
                    File file = downloadFile(fileDetails);

                    processFile(heights, bytes, file);

                    // demProvider.toImage("x" + file.getName() + ".png");
                    // TODO remove hgt and zip?
                } catch (Exception ex)
                {
                    throw new RuntimeException(ex);
                }
            } // loadExisting
        }

        return demProvider.getHeight(lat, lon);
    }

    private File downloadFile(String fileDetails) throws IOException, InterruptedException {
        String zippedURL = baseUrl + fileDetails + ".hgt.zip";
        File file = new File(cacheDir, new File(zippedURL).getName());

        // get zip file if not already in cacheDir - unzip later and in-memory only!
        if (!file.exists())
        {
            for (int i = 0; i < 3; i++)
            {
                try
                {
                    downloader.downloadFile(zippedURL, file.getAbsolutePath());
                    break;
                } catch (SocketTimeoutException ex)
                {
                    // just try again after a little nap
                    Thread.sleep(2000);
                    continue;
                } catch (FileNotFoundException ex)
                {
                    // now try different URL (with point!), necessary if mirror is used
                    zippedURL = baseUrl + "/" + fileDetails + ".hgt.zip";
                    continue;
                }
            }
        }
        return file;
    }

    private void processFile(DataAccess heights, byte[] bytes, File file) throws IOException {
        InputStream is;

        is = new FileInputStream(file);
        ZipInputStream zis = new ZipInputStream(is);
        zis.getNextEntry();
        BufferedInputStream buff = new BufferedInputStream(zis);
        int len;
        while ((len = buff.read(bytes)) > 0)
        {
            for (int bytePos = 0; bytePos < len; bytePos += 2)
            {
                short val = BIT_UTIL.toShort(bytes, bytePos);
                if (val < -1000 || val > 12000)
                    val = Short.MIN_VALUE;

                heights.setShort(bytePos, val);
            }
        }
        heights.flush();
    }

    @Override
    public void release()
    {
        cacheData.clear();

        // for memory mapped type we create temporary unpacked files which should be removed
        if (dir != null)
            dir.clear();
    }

    private Directory getDirectory()
    {
        if (dir != null)
            return dir;

        logger.info(this.toString() + " Elevation Provider, from: " + baseUrl + ", to: " + cacheDir + ", as: " + daType);
        return dir = new GHDirectory(cacheDir.getAbsolutePath(), daType);
    }
}
