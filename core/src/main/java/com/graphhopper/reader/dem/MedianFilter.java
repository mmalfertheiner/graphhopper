package com.graphhopper.reader.dem;

import java.util.Arrays;


public class MedianFilter implements Filter {

    private int[] pixels;

    public MedianFilter(int[] px){
        this.pixels = px;
    }

    @Override
    public int apply() {

        Arrays.sort(this.pixels);

        int medianIndex = (int) (this.pixels.length / 2);

        return this.pixels[medianIndex];

    }
}
