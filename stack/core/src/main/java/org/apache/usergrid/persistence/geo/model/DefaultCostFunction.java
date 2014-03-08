/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.geo.model;


import org.apache.usergrid.persistence.geo.GeocellUtils;


/**
 * Default cost function used if no cost function is specified in Geocell.bestBboxSearchCells method.
 *
 * @author Alexandre Gellibert
 */
public class DefaultCostFunction implements CostFunction {

    /*
     * (non-Javadoc)
     * @see com.beoui.utils.CostFunction#defaultCostFunction(int, int)
     */
    public double defaultCostFunction( int numCells, int resolution ) {
        return numCells > Math.pow( GeocellUtils.GEOCELL_GRID_SIZE, 2 ) ? Double.MAX_VALUE : 0;
    }
}
