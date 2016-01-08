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
package org.apache.usergrid.system;


import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.MapUtils;
import org.apache.usergrid.utils.TimeUtils;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.exceptions.HectorException;


/**
 * Provide a single spot for monitoring usergrid system health
 *
 * @author zznate
 */
public class UsergridSystemMonitor {
    private static final String TIMER_THRESHOLD_TRIGGERED_MSG =
            "TimerThreshold triggered on duration: %d \n%s\n----------------";
    private static final  Logger logger = LoggerFactory.getLogger( UsergridSystemMonitor.class );

    private final String buildNumber;
    private final Cluster cluster;
    /** The trigger point for printing debugging information. {@see #maybeLogPayload} */
    private long timerLogThreshold = 15 * 1000;
    public static final String LOG_THRESHOLD_PROPERTY = "metering.request.timer.log.threshold";


    /**
     * Must be instantiated with a build number and a cluster to be of any use. Properties can be null. Threshold
     * property must be a form compatible with {@link TimeUtils#millisFromDuration(String)}
     */
    public UsergridSystemMonitor( String buildNumber, Cluster cluster, Properties properties ) {
        this.buildNumber = buildNumber;
        this.cluster = cluster;
        if ( properties != null ) {
            timerLogThreshold = TimeUtils.millisFromDuration( properties.getProperty( LOG_THRESHOLD_PROPERTY, "15s" ) );
        }
    }


    /**
     * Wraps "describe_thrift_version API call as this hits a static string in Cassandra. This is the most lightweight
     * way to assure that Hector is alive and talking to the cluster.
     *
     * @return true if we have a lit connection to the cluster.
     */
    public boolean getIsCassandraAlive() {
        boolean isAlive = false;
        try {
            isAlive = cluster.describeThriftVersion() != null;
        }
        catch ( HectorException he ) {
            logger.error( "Could not communicate with Cassandra cluster", he );
        }
        return isAlive;
    }


    /** @return a string representing the build number */
    public String getBuildNumber() {
        return buildNumber;
    }


    /**
     * Uses {@link JsonUtils#mapToFormattedJsonString(Object)} against the object if the duration is greater than {@link
     * #timerLogThreshold}. When using the varargs form, the number of elements must be even such that key,value,key,
     * value mapping via {@link MapUtils#map(Object...)} can collect all the elements.
     * <p/>
     * Conversion to a map this way let's us lazy create the map if and only if the triggering threshold is true or we
     * are in debug mode.
     */
    public void maybeLogPayload( long duration, Object... objects ) {
        if ( duration > timerLogThreshold || logger.isDebugEnabled() ) {
            String message;
            if ( objects.length > 1 ) {
                message = formatMessage( duration, MapUtils.map( objects ) );
            }
            else {
                message = formatMessage( duration, objects );
            }
            logger.info( message );
        }
    }


    static String formatMessage( long duration, Object object ) {
        return String.format( TIMER_THRESHOLD_TRIGGERED_MSG, duration, JsonUtils.mapToFormattedJsonString( object ) );
    }
}
