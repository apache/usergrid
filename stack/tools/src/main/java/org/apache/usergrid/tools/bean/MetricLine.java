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


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.AggregateCounter;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;


/** @author zznate */
public class MetricLine {
    private final MetricSort metricSort;
    private final UUID appId;
    private final List<AggregateCounter> aggregateCounters;
    private long count = 0;


    /**
     * Package level access - intented to be used by {@link MetricQuery} only. Sets the value of count by iterating over
     * the {@link AggregateCounter} collection.
     */
    MetricLine( UUID appId, MetricSort metricSort, List<AggregateCounter> counters ) {
        Preconditions.checkArgument( appId != null, "appId was null" );
        Preconditions.checkArgument( counters != null, "Counters list cannot be null" );
        this.metricSort = metricSort;
        this.appId = appId;
        this.aggregateCounters = counters;
        if ( aggregateCounters.size() > 0 ) {
            for ( AggregateCounter ac : aggregateCounters ) {
                count += ac.getValue();
            }
        }
    }


    @Override
    public String toString() {
        return Objects.toStringHelper( this ).add( "appId", appId ).add( "metricSort", metricSort ).toString();
    }


    /** Compares metricSort and appId for equality */
    @Override
    public boolean equals( Object o ) {
        if ( o instanceof MetricLine ) {
            MetricLine oth = ( MetricLine ) o;
            return oth.getMetricSort().equals( metricSort ) && oth.getAppId().equals( appId );
        }
        return false;
    }


    @Override
    public int hashCode() {
        return Objects.hashCode( metricSort, appId );
    }


    public MetricSort getMetricSort() {
        return metricSort;
    }


    public long getCount() {
        return count;
    }


    public UUID getAppId() {
        return appId;
    }


    /** @return an Immutable list of our counters */
    public List<AggregateCounter> getAggregateCounters() {
        return ImmutableList.copyOf( aggregateCounters );
    }
}
