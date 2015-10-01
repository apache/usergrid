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
package org.apache.usergrid.tools.bean;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.AggregateCounter;
import org.apache.usergrid.persistence.AggregateCounterSet;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;

import com.google.common.base.Preconditions;
import org.apache.usergrid.persistence.index.query.CounterResolution;


/** @author zznate */
public class MetricQuery {

    private final UUID appId;
    private final MetricSort metricSort;
    private CounterResolution counterResolution = CounterResolution.DAY;
    private long startDate = 0;
    private long endDate = 0;
    private boolean padding = false;


    private MetricQuery( UUID appId, MetricSort metricSort ) {
        this.appId = appId;
        this.metricSort = metricSort;
    }


    public static MetricQuery getInstance( UUID appId, MetricSort metricSort ) {
        return new MetricQuery( appId, metricSort );
    }


    public MetricQuery resolution( CounterResolution counterResolution ) {
        this.counterResolution = counterResolution;
        return this;
    }


    public MetricQuery startDate( long startDate ) {
        this.startDate = startDate;
        return this;
    }


    public MetricQuery endDate( long endDate ) {
        this.endDate = endDate;
        return this;
    }


    public MetricQuery pad() {
        this.padding = true;
        return this;
    }


    /** @return A List (potentially empty) of the AggregateCounter values found. */
    public MetricLine execute( EntityManager entityManager ) throws Exception {
        Query query = new Query();
        query.addCounterFilter( metricSort.queryFilter() ); // TODO MetricSort.queryFilter
        if ( startDate > 0 ) {
            query.setStartTime( startDate );
        }
        if ( endDate > 0 ) {
            Preconditions
                    .checkArgument( endDate > startDate, "The endDate (%s) must be greater than the startDate (%s)",
                            endDate, startDate );
        }
        else {
            endDate = System.currentTimeMillis();
        }
        query.setFinishTime( endDate );
        query.setResolution( counterResolution );
        query.setPad( padding );
        Results r = entityManager.getAggregateCounters( query );

        List<AggregateCounterSet> qc = r.getCounters();
        List<AggregateCounter> counters = new ArrayList();
        if ( qc != null && qc.size() > 0 ) {
            if ( qc.get( 0 ) != null && qc.get( 0 ).getValues() != null ) {
                counters.addAll( qc.get( 0 ).getValues() );
            }
        }
        return new MetricLine( appId, metricSort, counters );
    }
}
