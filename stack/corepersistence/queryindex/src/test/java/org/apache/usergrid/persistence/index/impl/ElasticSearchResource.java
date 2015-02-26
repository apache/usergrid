/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.usergrid.persistence.index.impl;


import java.util.Properties;

import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.EnvironResource;

import org.apache.usergrid.persistence.index.IndexFig;


/**
 * Sets elasticsearch variables into the environment
 *
 * TODO make static
 */
public class ElasticSearchResource extends EnvironResource {


    private static int port;
    private static String host;


    public ElasticSearchResource() {
        super( Env.UNIT );
        try {
            Properties props = new Properties();
            props.load( ClassLoader.getSystemResourceAsStream( "project.properties" ) );
            host = props.getProperty( "elasticsearch.host", "127.0.0.1" );
            port = Integer.valueOf( props.getProperty( "elasticsearch.port", "9300" ) ).intValue();
        }
        catch ( Exception ex ) {
            throw new RuntimeException( "Error getting properties", ex );
        }
    }


    /**
     * Start the resources
     */
    public void start() {
        before();
    }


    @Override
    protected void before() {
        System.setProperty( IndexFig.ELASTICSEARCH_HOSTS, host );
        System.setProperty( IndexFig.ELASTICSEARCH_PORT, port + "" );
    }


    public static int getPort() {
        return port;
    }
}
