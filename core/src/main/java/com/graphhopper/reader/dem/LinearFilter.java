package com.graphhopper.reader.dem;


public class LinearFilter implements Filter {

    private int[] pixels;
    private double[] kernel;

    public LinearFilter(int[] px, double[] kernel) {
        this.pixels = px;
        this.kernel = kernel;
    }


    @Override
    public int apply() {

        double result = 0;

        for(int i = 0; i < pixels.length; i++) {
            result += pixels[i] * kernel[i];
            System.out.println("Pixel: " + pixels[i]);
            System.out.println("Kernel: " + kernel[i]);

        }

        return (int) result;
    }
}
