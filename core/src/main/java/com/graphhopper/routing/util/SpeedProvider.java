package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

public interface SpeedProvider {

    double calcSpeed(EdgeIteratorState edgeIteratorState, boolean reverse);

}
