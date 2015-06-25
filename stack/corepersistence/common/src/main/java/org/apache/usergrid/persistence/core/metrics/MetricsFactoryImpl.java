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
package org.apache.usergrid.persistence.core.metrics;


import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Singleton class to manage metrics.
 */
@Singleton
public class MetricsFactoryImpl implements MetricsFactory {


    private MetricRegistry registry;
    private GraphiteReporter graphiteReporter;
    private JmxReporter jmxReporter;
    private ConcurrentHashMap<String, Metric> hashMap;
    private static final Logger LOG = LoggerFactory.getLogger( MetricsFactoryImpl.class );


    @Inject
    public MetricsFactoryImpl( MetricsFig metricsFig ) {
        registry = new MetricRegistry();
        String metricsHost = metricsFig.getHost();
        if ( !metricsHost.equals( "false" ) ) {
            Graphite graphite = new Graphite( new InetSocketAddress( metricsHost, 2003 ) );
            graphiteReporter = GraphiteReporter.forRegistry( registry ).prefixedWith( "usergrid-metrics" )
                                               .convertRatesTo( TimeUnit.SECONDS )
                                               .convertDurationsTo( TimeUnit.MILLISECONDS ).filter( MetricFilter.ALL )
                                               .build( graphite );
            graphiteReporter.start( 30, TimeUnit.SECONDS );
        }
        else {
            LOG.warn( "MetricsService:Logger not started." );
        }
        hashMap = new ConcurrentHashMap<String, Metric>();

        jmxReporter = JmxReporter.forRegistry( registry ).build();
        jmxReporter.start();
    }


    @Override
    public MetricRegistry getRegistry() {
        return registry;
    }


    @Override
    public Timer getTimer( Class<?> klass, String name ) {
        return getMetric( Timer.class, klass, name );
    }


    @Override
    public Histogram getHistogram( Class<?> klass, String name ) {
        return getMetric( Histogram.class, klass, name );
    }


    @Override
    public Counter getCounter( Class<?> klass, String name ) {
        return getMetric( Counter.class, klass, name );
    }


    @Override
    public Meter getMeter( Class<?> klass, String name ) {
        return getMetric( Meter.class, klass, name );
    }


    @Override
    public void addGauge( final Class<?> clazz, final String name, final Gauge<?> gauge ) {

        this.getRegistry().register( MetricRegistry.name( clazz, name ), gauge );
    }


    private <T> T getMetric( Class<T> metricClass, Class<?> klass, String name ) {
        String key = metricClass.getName() + klass.getName() + name;
        Metric metric = hashMap.get( key );
        if ( metric == null ) {
            if ( metricClass == Histogram.class ) {
                metric = this.getRegistry().histogram( MetricRegistry.name( klass, name ) );
            }
            if ( metricClass == Timer.class ) {
                metric = this.getRegistry().timer( MetricRegistry.name( klass, name ) );
            }
            if ( metricClass == Meter.class ) {
                metric = this.getRegistry().meter( MetricRegistry.name( klass, name ) );
            }
            if ( metricClass == Counter.class ) {
                metric = this.getRegistry().counter( MetricRegistry.name( klass, name ) );
            }


            hashMap.put( key, metric );
        }
        return ( T ) metric;
    }


    /**
     *
     * @param metricClass
     * @param klass
     * @param name
     * @return
     */
    private String getKey( Class<?> metricClass, Class<?> klass, String name ) {
        return metricClass.getName() + klass.getName() + name;
    }
}
