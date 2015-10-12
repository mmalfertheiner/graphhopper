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

/**
 * Used to store bike network types. Used in combination with DynamicWeighting to meet the users preferences
 * <p>
 * @author Martin Malfertheiner
 */
public enum BicycleNetworkCode
{
    UNCLASSIFIED(0),
    DEPRECATED(1),
    FERRY(2),
    INTERNATIONAL_CYCLING_NETWORK(3),
    NATIONAL_CYCLING_NETWORK(4),
    REGIONAL_CYCLING_ROUTES(5),
    LOCAL_CYCLING_ROUTES(6),
    MOUNTAIN_BIKE_ROUTE(7);

    private final int value;

    private BicycleNetworkCode(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

}
