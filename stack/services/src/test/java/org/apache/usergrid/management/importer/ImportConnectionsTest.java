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

import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;
import org.apache.usergrid.persistence.index.query.Query;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertEquals;


@Concurrent
public class ImportConnectionsTest {

    private static final Logger logger = LoggerFactory.getLogger(ImportConnectionsTest.class);

    private static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static final ServiceITSetup setup =
        new ServiceITSetupImpl( cassandraResource, new ElasticSearchResource() );

    @Rule
    public NewOrgAppAdminRule newOrgAppAdminRule = new NewOrgAppAdminRule( setup );

    @Test
    public void testCreateAndSearchConnections() throws Exception {

        final int connectionCount = 10;

        EntityManager emMgmtApp = setup.getEmf()
            .getEntityManager(CpNamingUtils.MANAGEMENT_APPLICATION_ID);

        Import importEntity = new Import();
        importEntity = emMgmtApp.create( importEntity );

        UUID applicationId = newOrgAppAdminRule.getApplicationInfo().getId();

        for ( int i=0; i<connectionCount; i++ ) {
            FileImport fileImport = new FileImport("dummyFileName" + i, applicationId);
            fileImport = emMgmtApp.create(fileImport);
            emMgmtApp.createConnection(importEntity, "includes", fileImport);
        }

        int retries = 0;
        int maxRetries = 60;
        boolean done = false;
        while ( !done && retries++ < maxRetries ) {

            final int count = getConnectionCount(importEntity);
            if ( count == connectionCount ) {
                logger.debug("Count good!");
                done = true;
            } else {
                logger.debug("Waiting...");
                Thread.sleep(1000);
            }
        }
        if ( retries >= maxRetries ) {
            throw new RuntimeException("Max retries was reached");
        }

        assertEquals("did not get all connections",
            connectionCount, getConnectionCount( importEntity ));
    }

    private int getConnectionCount( final Import importRoot ) {

        try {
            EntityManager rootEm = setup.getEmf()
                .getEntityManager(CpNamingUtils.MANAGEMENT_APPLICATION_ID );

            Results entities = rootEm.getConnectedEntities(
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
}

