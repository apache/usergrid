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

package org.apache.usergrid.management.importer;


import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.PagingResultsIterator;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.index.query.Query;

import static org.junit.Assert.assertEquals;



public class ImportConnectionsTest {

    private static final Logger logger = LoggerFactory.getLogger(ImportConnectionsTest.class);

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static final ServiceITSetup setup =
        new ServiceITSetupImpl( );

    @Rule
    public NewOrgAppAdminRule newOrgAppAdminRule = new NewOrgAppAdminRule( setup );


    @Test
    @Ignore("Because getConnectedEntities() is broken")
    public void testCreateAndCountConnectionsViaGet() throws Exception {

        doTestCreateAndCountConnections(new ConnectionCounter() {
            @Override
            public int count(Import importEntity) {
                return getConnectionCountViaGet(importEntity);
            }
        });
    }


    @Test
    public void testCreateAndCountConnectionsViaSearch() throws Exception {

        doTestCreateAndCountConnections(new ConnectionCounter() {
            @Override
            public int count(Import importEntity) {
                return getConnectionCountViaSearch(importEntity);
            }
        });
    }


    interface ConnectionCounter {
        int count( Import importEntity );
    }


    public void doTestCreateAndCountConnections(
        ConnectionCounter counter) throws Exception {

        final int connectionCount = 15;

        EntityManager emMgmtApp = setup.getEmf().getEntityManager(setup.getEmf().getManagementAppId());

        Import importEntity = new Import();
        importEntity = emMgmtApp.create( importEntity );

        UUID applicationId = newOrgAppAdminRule.getApplicationInfo().getId();

        for ( int i=0; i<connectionCount; i++ ) {
            FileImport fileImport = new FileImport("dummyFileName" + i, applicationId);
            fileImport = emMgmtApp.create(fileImport);
            emMgmtApp.createConnection(importEntity, "includes", fileImport);
        }

        int retries = 0;
        int maxRetries = 20;
        boolean done = false;
        int count = 0;
        while ( !done && retries++ < maxRetries ) {

            count = counter.count( importEntity );
            if ( count == connectionCount ) {
                logger.debug("Count good!");
                done = true;
            } else {
                logger.debug("Got {} of {} Waiting...", count, connectionCount );
                Thread.sleep(1000);
            }
        }
        if ( retries >= maxRetries ) {
            throw new RuntimeException("Max retries was reached");
        }

        assertEquals("did not get all connections", connectionCount, count);
    }


    private int getConnectionCountViaGet( final Import importRoot ) {

        try {
            EntityManager emMgmtApp = setup.getEmf()
                .getEntityManager(setup.getEmf().getManagementAppId() );

            Results entities = emMgmtApp.getConnectedEntities(
                importRoot, "includes", null, Query.Level.ALL_PROPERTIES );

            PagingResultsIterator itr = new PagingResultsIterator( entities );
            int count = 0;
            while ( itr.hasNext() ) {
                itr.next();
                count++;
            }
            return count;
        }
        catch ( Exception e ) {
            logger.error( "application doesn't exist within the current context" );
            throw new RuntimeException( e );
        }
    }


    private int getConnectionCountViaSearch( final Import importRoot ) {

        try {
            EntityManager emMgmtApp = setup.getEmf()
                .getEntityManager(setup.getEmf().getManagementAppId() );

            Query query = Query.fromQL("select *");
            query.setEntityType("file_import");
            query.setConnectionType("includes");
            query.setLimit(10000);

            Results entities = emMgmtApp.searchConnectedEntities( importRoot, query );
            return entities.size();

//            PagingResultsIterator itr = new PagingResultsIterator( entities );
//            int count = 0;
//            while ( itr.hasNext() ) {
//                itr.next();
//                count++;
//            }
//            return count;
        }
        catch ( Exception e ) {
            logger.error( "application doesn't exist within the current context" );
            throw new RuntimeException( e );
        }
    }
}

