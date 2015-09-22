package com.graphhopper.util;

public interface SmoothingFilter {

    /**
     * This method starts the smoothing process of the filter and return the smoothed values.
     * @return
     */
    double[] smooth(double[] measurements);

    /**
     *
     * @return Array of smoothed values
     */
    double[] getFilteredValues();

}
