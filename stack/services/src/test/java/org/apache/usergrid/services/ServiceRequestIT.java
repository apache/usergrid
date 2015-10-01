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
package org.apache.usergrid.services;


import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;

import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;

import static org.apache.usergrid.services.ServiceParameter.filter;
import static org.apache.usergrid.services.ServiceParameter.parameters;



public class ServiceRequestIT extends AbstractServiceIT {

    private static final Logger logger = LoggerFactory.getLogger( ServiceRequestIT.class );


    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @Test
    public void testPaths() throws Exception {


        ServiceManager services = setup.getSmf().getServiceManager( app.getId() );

        ServiceRequest path = services.newRequest( ServiceAction.GET, parameters( "users", "bob" ), null );
        // path = path.addSegment("users", "bob");
        logger.info( "" + path.getParameters() );

        Map<List<String>, List<String>> replaceParameters = new LinkedHashMap<List<String>, List<String>>();
        replaceParameters.put( Arrays.asList( "users" ), Arrays.asList( "connecting", "users" ) );
        List<ServiceParameter> p = filter( path.getParameters(), replaceParameters );
        // path = path.addSegment("messages", "bob");
        logger.info( "" + p );

        path = services.newRequest( ServiceAction.GET, parameters( "users", UUID.randomUUID(), "messages" ), null );
        logger.info( "" + path.getParameters() );

        logger.info( "\\1" );
        replaceParameters = new LinkedHashMap<List<String>, List<String>>();
        replaceParameters.put( Arrays.asList( "users", "$id" ), Arrays.asList( "connecting", "\\1", "users" ) );
        p = filter( path.getParameters(), replaceParameters );
        logger.info( "" + p );
    }
}
