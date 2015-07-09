package com.graphhopper.reader.dem;


import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.util.Helper;
import org.apache.xmlgraphics.image.codec.tiff.TIFFDecodeParam;
import org.apache.xmlgraphics.image.codec.tiff.TIFFImageDecoder;
import org.apache.xmlgraphics.image.codec.util.SeekableStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SouthTyrolProvider implements ElevationProvider{

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, HeightTile> cacheData = new HashMap<String, HeightTile>();
    private final int WIDTH = 5305;
    private File cacheDir = new File("/tmp/southTyrol");
    private Directory dir;
    private DAType daType = DAType.MMAP;
    final double precision = 1e7;
    private final double invPrecision = 1 / precision;
    private boolean calcMean = false;
    private boolean autoRemoveTemporary = true;

    private String dataFileName = "DTM-2p5m";

    /**
     * Creating temporary files can take a long time as we need to unpack tiff as well as to fill
     * our DataAccess object, so this option can be used to disable the default clear mechanism via
     * specifying 'false'.
     */
    public void setAutoRemoveTemporaryFiles( boolean autoRemoveTemporary )
    {
        this.autoRemoveTemporary = autoRemoveTemporary;
    }

    @Override
    public double getEle(double lat, double lon) {

        lat = (int) (lat * precision) / precision;
        lon = (int) (lon * precision) / precision;

        HeightTile demProvider = cacheData.get(dataFileName);

        if(demProvider == null) {

            if (!cacheDir.exists())
                cacheDir.mkdirs();

            int minLat = down(lat);
            int minLon = down(lon);

            demProvider = new HeightTile(minLat, minLon, WIDTH, precision, 1);
            demProvider.setCalcMean(calcMean);

            cacheData.put(dataFileName, demProvider);
            DataAccess heights = getDirectory().find(dataFileName + ".gh");
            demProvider.setHeights(heights);
            boolean loadExisting = false;
            try
            {
                loadExisting = heights.loadExisting();
            } catch (Exception ex)
            {
                logger.warn("cannot load " + dataFileName + ", error:" + ex.getMessage());
            }

            if (!loadExisting){

                String tifName = dataFileName + ".tif";
                File file = new File(cacheDir, tifName);

                // short == 2 bytes
                heights.create(2 * WIDTH * WIDTH);

                Raster raster;
                SeekableStream ss = null;

                try
                {
                    InputStream is = new FileInputStream(file);

                    ss = SeekableStream.wrapInputStream(is, true);
                    TIFFImageDecoder imageDecoder = new TIFFImageDecoder(ss, new TIFFDecodeParam());
                    raster = imageDecoder.decodeAsRaster();
                } catch (Exception e)
                {
                    throw new RuntimeException("Can't decode " + tifName, e);
                } finally
                {
                    if (ss != null)
                        Helper.close(ss);
                }

                // logger.info("start converting to our format");
                final int height = raster.getHeight();
                final int width = raster.getWidth();
                int x = 0, y = 0;
                try
                {
                    for (y = 0; y < height; y++)
                    {
                        for (x = 0; x < width; x++)
                        {
                            short val = (short) raster.getPixel(x, y, (int[]) null)[0];
                            if (val < -1000 || val > 12000)
                                val = Short.MIN_VALUE;

                            heights.setShort(2 * (y * WIDTH + x), val);
                        }
                    }
                    heights.flush();

                } catch (Exception ex)
                {
                    throw new RuntimeException("Problem at x:" + x + ", y:" + y, ex);
                }

            }

        }

        if (demProvider.isSeaLevel())
            return 0;

        return demProvider.getHeight(lat, lon);
    }

    int down( double val )
    {
        int intVal = (int) val;
        if (val >= 0 || intVal - val < invPrecision)
            return intVal;
        return intVal - 1;
    }

    private Directory getDirectory()
    {
        if (dir != null)
            return dir;

        return dir = new GHDirectory(cacheDir.getAbsolutePath(), daType);
    }

    @Override
    public ElevationProvider setBaseURL(String baseURL) {
        return null;
    }

    @Override
    public ElevationProvider setCacheDir(File cacheDir) {
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

    protected File getCacheDir()
    {
        return cacheDir;
    }

    @Override
    public ElevationProvider setDAType(DAType daType) {
        this.daType = daType;
        return this;
    }

    @Override
    public void setCalcMean(boolean calcMean) {
        this.calcMean = calcMean;
    }

    @Override
    public void release() {
        cacheData.clear();

        // for memory mapped type we create temporary unpacked files which should be removed
        if (autoRemoveTemporary && dir != null)
            dir.clear();
    }

    @Override
    public String toString()
    {
        return "SouthTyrol";
    }

    public static void main(String[] args){

        ElevationProvider elevationProvider = new SouthTyrolProvider();

        System.out.println(elevationProvider.getEle(46.544167, 11.562222));

    }

}
