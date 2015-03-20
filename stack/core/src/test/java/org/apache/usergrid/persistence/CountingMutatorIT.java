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
package org.apache.usergrid.persistence;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.hector.CountingMutator;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.utils.UUIDUtils;

import static org.junit.Assert.assertEquals;

public class CountingMutatorIT extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( CountingMutatorIT.class );

    private int originalAmount;


    public CountingMutatorIT() {
        super();
    }


    @Before
    public void storeAmount(){
        originalAmount = CountingMutator.MAX_SIZE;
    }

    @After
    public void setSize(){
        CountingMutator.MAX_SIZE = originalAmount;
    }


    @Test
    public void testFlushingMutatorOnConnections() throws Exception {

        //temporarily set our max size to 10 for testing
        CountingMutator.MAX_SIZE = 10;

        UUID applicationId = setup.createApplication(
            "testOrganization", "testFlushingMutator-" + UUIDGenerator.newTimeUUID() );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );


        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "testuser" );
        properties.put( "username", "testuser" );
        properties.put( "email", "test@foo.bar" );
        Entity created = em.create( "user", properties );
        app.refreshIndex();

        Entity returned = em.get( created.getUuid() );

        int writeSize = ( int ) ( CountingMutator.MAX_SIZE*2.5);

        for(int i = 0; i < writeSize; i ++){
            Map<String, Object> connectedProps = new LinkedHashMap<String, Object>();
            final UUID uuid = UUIDUtils.newTimeUUID();
            connectedProps.put( "name", "testuser"+uuid);
            connectedProps.put( "username", "testuser"+uuid );
            connectedProps.put( "email", "test"+uuid+"@foo.bar" );


            Entity connectedEntity = em.create( "user", connectedProps );
            app.refreshIndex();

            // Connect from our new entity to our root one so it's updated when paging
            em.createConnection( connectedEntity, "following", returned );
        }

        //now verify our connections were created properly

        PagingResultsIterator itr = new PagingResultsIterator(em.getConnectingEntities(
                returned, "following", "user", Level.ALL_PROPERTIES, 1000 ));

        int count = 0;

        while(itr.hasNext()){
            itr.next();
            count++;
        }

        assertEquals("Correct number of connections created", writeSize, count);

        //now update the props on the entity to update the connections

        properties.put( "email", "test2@foo.bar" );
        em.updateProperties( returned, properties );

    }
}
