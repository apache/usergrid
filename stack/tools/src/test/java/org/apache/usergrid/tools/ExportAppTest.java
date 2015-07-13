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
package org.apache.usergrid.tools;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ExportAppTest {
    static final Logger logger = LoggerFactory.getLogger( ExportAppTest.class );

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );

    @org.junit.Test
    public void testBasicOperation() throws Exception {
       
        String rand = RandomStringUtils.randomAlphanumeric( 10 );
        
        // create app with some data

        OrganizationOwnerInfo orgInfo = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + rand, "user_" + rand, rand.toUpperCase(), rand + "@example.com", rand );

        ApplicationInfo appInfo = setup.getMgmtSvc().createApplication(
                orgInfo.getOrganization().getUuid(), "app_" + rand );

        EntityManager em = setup.getEmf().getEntityManager( appInfo.getId() );
        
        // create 10 connected things

        List<Entity> connectedThings = new ArrayList<Entity>();
        String connectedType = "connected_thing";
        em.createApplicationCollection(connectedType);
        for ( int j=0; j<10; j++) {
            final String name = "connected_thing_" + j;
            connectedThings.add( em.create( connectedType, new HashMap<String, Object>() {{
                put( "name", name );
            }} ) );
        }
       
        // create 10 collections of 10 things, every other thing is connected to the connected things
        
        for ( int i=0; i<10; i++) {
            String type = "thing_"+i;
            em.createApplicationCollection(type);
            for ( int j=0; j<10; j++) {
                final String name = "thing_" + j;
                Entity source = em.create(type, new HashMap<String, Object>() {{ put("name", name); }});
                if ( j % 2 == 0 ) {
                    for ( Entity target : connectedThings ) {
                        em.createConnection( source, "has", target );
                    }
                }
            }
        }
        
        // export to file

        String directoryName = "./target/export" + rand;

        ExportApp exportApp = new ExportApp();
        exportApp.startTool( new String[]{
                "-application", appInfo.getName(),
                "-host", "localhost:" + ServiceITSuite.cassandraResource.getRpcPort(),
                "-outputDir", directoryName
        }, false );
        
    }
}