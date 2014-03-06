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
package org.apache.usergrid.rest.management.organizations.applications;


import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;

import static org.junit.Assert.assertEquals;


/**
 *
 *
 */
public class ApplicationsIT extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void test10AppLimit() {

        int size = 11;

        Set<String> appNames = new HashSet<String>( size );

        for ( int i = 0; i < size; i++ ) {
            final String name = i + "";

            appNames.add( name );

            context.withApp( name ).createAppForOrg();
        }

        //now go through and ensure each entry is present

        final JsonNode apps = context.management().orgs().organization( context.getOrgName() ).apps().get();

        final JsonNode data = apps.get( "data" );

        final String orgName = context.getOrgName();


        final Set<String> copy = new HashSet<String> (appNames);

        for(String appName: copy){

            final String mapEntryName = String.format( "%s/%s", orgName.toLowerCase(),  appName.toLowerCase());

            JsonNode orgApp = data.get( mapEntryName);

            if(orgApp != null){
                appNames.remove( appName );
            }

        }

        assertEquals("All elements removed", 0, appNames.size());

    }
}
