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
package org.apache.usergrid.persistence;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.usergrid.persistence.index.query.CounterResolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Query.CounterFilterPredicate;
import org.apache.usergrid.utils.JsonUtils;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.ListUtils.firstBoolean;
import static org.apache.usergrid.utils.ListUtils.firstInteger;
import static org.apache.usergrid.utils.ListUtils.firstLong;
import static org.apache.usergrid.utils.ListUtils.isEmpty;
import static org.apache.usergrid.utils.MapUtils.toMapList;


public class CounterQuery {

    public static final Logger logger = LoggerFactory.getLogger( CounterQuery.class );

    public static final int DEFAULT_MAX_RESULTS = 10;

    private int limit = 0;
    boolean limitSet = false;

    private Long startTime;
    private Long finishTime;
    private boolean pad;
    private CounterResolution resolution = CounterResolution.ALL;
    private List<String> categories;
    private List<CounterFilterPredicate> counterFilters;


    public CounterQuery() {
    }


    public CounterQuery( CounterQuery q ) {
        if ( q != null ) {
            limit = q.limit;
            limitSet = q.limitSet;
            startTime = q.startTime;
            finishTime = q.finishTime;
            resolution = q.resolution;
            pad = q.pad;
            categories = q.categories != null ? new ArrayList<String>( q.categories ) : null;
            counterFilters =
                    q.counterFilters != null ? new ArrayList<CounterFilterPredicate>( q.counterFilters ) : null;
        }
    }


    public static CounterQuery newQueryIfNull( CounterQuery query ) {
        if ( query == null ) {
            query = new CounterQuery();
        }
        return query;
    }


    public static CounterQuery fromJsonString( String json ) {
        Object o = JsonUtils.parse( json );
        if ( o instanceof Map ) {
            @SuppressWarnings({ "unchecked", "rawtypes" }) Map<String, List<String>> params =
                    cast( toMapList( ( Map ) o ) );
            return fromQueryParams( params );
        }
        return null;
    }


    public static CounterQuery fromQueryParams( Map<String, List<String>> params ) {

        CounterQuery q = null;
        CounterResolution resolution = null;
        List<CounterFilterPredicate> counterFilters = null;
        Integer limit = firstInteger( params.get( "limit" ) );
        Long startTime = firstLong( params.get( "start_time" ) );
        Long finishTime = firstLong( params.get( "end_time" ) );

        List<String> l = params.get( "resolution" );
        if ( !isEmpty( l ) ) {
            resolution = CounterResolution.fromString( l.get( 0 ) );
        }

        List<String> categories = params.get( "category" );

        l = params.get( "counter" );
        if ( !isEmpty( l ) ) {
            counterFilters = CounterFilterPredicate.fromList( l );
        }

        Boolean pad = firstBoolean( params.get( "pad" ) );

        if ( limit != null ) {
            q = newQueryIfNull( q );
            q.setLimit( limit );
        }

        if ( startTime != null ) {
            q = newQueryIfNull( q );
            q.setStartTime( startTime );
        }

        if ( finishTime != null ) {
            q = newQueryIfNull( q );
            q.setFinishTime( finishTime );
        }

        if ( resolution != null ) {
            q = newQueryIfNull( q );
            q.setResolution( resolution );
        }

        if ( categories != null ) {
            q = newQueryIfNull( q );
            q.setCategories( categories );
        }

        if ( counterFilters != null ) {
            q = newQueryIfNull( q );
            q.setCounterFilters( counterFilters );
        }

        if ( pad != null ) {
            q = newQueryIfNull( q );
            q.setPad( pad );
        }

        return q;
    }


    public int getLimit() {
        return getLimit( DEFAULT_MAX_RESULTS );
    }


    public int getLimit( int defaultMax ) {
        if ( limit <= 0 ) {
            return defaultMax > 0 ? defaultMax : DEFAULT_MAX_RESULTS;
        }
        return limit;
    }


    public void setLimit( int limit ) {
        limitSet = true;
        this.limit = limit;
    }


    public CounterQuery withLimit( int limit ) {
        limitSet = true;
        this.limit = limit;
        return this;
    }


    public boolean isLimitSet() {
        return limitSet;
    }


    public Long getStartTime() {
        return startTime;
    }


    public void setStartTime( Long startTime ) {
        this.startTime = startTime;
    }


    public CounterQuery withStartTime( Long startTime ) {
        this.startTime = startTime;
        return this;
    }


    public Long getFinishTime() {
        return finishTime;
    }


    public void setFinishTime( Long finishTime ) {
        this.finishTime = finishTime;
    }


    public CounterQuery withFinishTime( Long finishTime ) {
        this.finishTime = finishTime;
        return this;
    }


    public boolean isPad() {
        return pad;
    }


    public void setPad( boolean pad ) {
        this.pad = pad;
    }


    public CounterQuery withPad( boolean pad ) {
        this.pad = pad;
        return this;
    }


    public void setResolution( CounterResolution resolution ) {
        this.resolution = resolution;
    }


    public CounterResolution getResolution() {
        return resolution;
    }


    public CounterQuery withResolution( CounterResolution resolution ) {
        this.resolution = resolution;
        return this;
    }


    public List<String> getCategories() {
        return categories;
    }


    public CounterQuery addCategory( String category ) {
        if ( categories == null ) {
            categories = new ArrayList<String>();
        }
        categories.add( category );
        return this;
    }


    public void setCategories( List<String> categories ) {
        this.categories = categories;
    }


    public CounterQuery withCategories( List<String> categories ) {
        this.categories = categories;
        return this;
    }


    public List<CounterFilterPredicate> getCounterFilters() {
        return counterFilters;
    }


    public CounterQuery addCounterFilter( String counter ) {
        CounterFilterPredicate p = CounterFilterPredicate.fromString( counter );
        if ( p == null ) {
            return this;
        }
        if ( counterFilters == null ) {
            counterFilters = new ArrayList<CounterFilterPredicate>();
        }
        counterFilters.add( p );
        return this;
    }


    public void setCounterFilters( List<CounterFilterPredicate> counterFilters ) {
        this.counterFilters = counterFilters;
    }


    public CounterQuery withCounterFilters( List<CounterFilterPredicate> counterFilters ) {
        this.counterFilters = counterFilters;
        return this;
    }
}
