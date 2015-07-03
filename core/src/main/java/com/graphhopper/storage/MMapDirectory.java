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
package com.graphhopper.storage;

/**
 * Manages memory mapped DataAccess objects.
 * <p/>
 * @author Peter Karich
 * @see MMapDataAccess
 */
public class MMapDirectory extends GHDirectory
{
    // reserve the empty constructor for direct mapped memory
    private MMapDirectory()
    {
        this("");
        throw new IllegalStateException("reserved for direct mapped memory");
    }

    public MMapDirectory( String _location )
    {
        super(_location, DAType.MMAP);
    }
}
