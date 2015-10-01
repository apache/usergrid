/*
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.apache.usergrid.persistence.index.impl;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortOrder;


/**
 * Helper class for creating geo sort fields
 */
public class GeoSortFields {


    private final Map<String, GeoDistanceSortBuilder> geoSorts = new HashMap<>();


    /**
     * Add a geo sort field to this sorts
     * @param name
     * @param sortBuilder
     */
    public void addField(final String name, final GeoDistanceSortBuilder sortBuilder){
        this.geoSorts.put( name, sortBuilder );
    }


    /**
     * Return true if we have a sort of for this key
     * @param name
     * @return
     */
    public boolean contains(final String name){
        return geoSorts.containsKey( name );
    }


    /**
     * Return true if empty
     * @return
     */
    public boolean isEmpty(){
        return geoSorts.isEmpty();
    }


    /**
     * Returns all fields in this geo sort
     * @return
     */
    public Set<String> fields(){
        return geoSorts.keySet();
    }

    /**
     * Apply the ordering to our geo sorts. Note this will modify the object stored in this geoSort
     * @param name
     * @param sortOrder
     */
    public GeoDistanceSortBuilder applyOrder( final String name, SortOrder sortOrder ){

        final GeoDistanceSortBuilder geoDistanceSortBuilder = geoSorts.get( name );

        geoDistanceSortBuilder.order( sortOrder );

        return geoDistanceSortBuilder;
    }

}
