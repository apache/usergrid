/*
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
 */

package org.apache.usergrid.persistence.qakka.api;

import com.codahale.metrics.Timer;
import com.google.inject.servlet.RequestScoped;
import org.apache.usergrid.persistence.qakka.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.SortedSet;


@Path("status")
@RequestScoped
public class StatusResource {
    private static final Logger logger = LoggerFactory.getLogger( StatusResource.class );

    private App app;

    @Inject
    public StatusResource( App app ) {
        this.app = app;
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Object status() {

        final DecimalFormat format = new DecimalFormat("##.###");
        final long nano = 1000000000;

        return new HashMap<String, Object>() {{
            put( "name", "Qakka" );
            try {
                put( "host", InetAddress.getLocalHost().getHostName() );
            } catch (UnknownHostException e) {
                put( "host", "unknown" );
            }
            SortedSet<String> names = app.getMetricRegistry().getNames();
            for (String name : names) {
                Timer t = app.getMetricRegistry().timer( name );
                put( name, new HashMap<String, Object>() {{
                    put( "count", ""            + t.getCount() );
                    put( "mean_rate", ""        + format.format( t.getMeanRate() ) );
                    put( "one_minute_rate", ""  + format.format( t.getOneMinuteRate() ) );
                    put( "five_minute_rate", "" + format.format( t.getFiveMinuteRate() ) );
                    put( "mean", ""             + format.format( t.getSnapshot().getMean() / nano ) );
                    put( "min", ""              + format.format( (double) t.getSnapshot().getMin() / nano ) );
                    put( "max", ""              + format.format( (double) t.getSnapshot().getMax() / nano ) );
                }} );
            }
        }};

    }
}
