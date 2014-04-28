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

import com.google.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.scope.OrganizationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.legacy.EntityManagerFacade;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import static org.junit.Assert.assertNotNull;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(JukitoRunner.class)
@UseModules(TestIndexModule.class)
public class EntityIndexStressTest {

    private static final Logger log = LoggerFactory.getLogger( 
            EntityIndexStressTest.class );

    @ClassRule
    public static CassandraRule cass = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;
        
    @Inject
    public EntityCollectionManagerFactory cmf;
    
    @Inject
    public EntityIndexFactory cif;

    @Test
    public void indexThousands() throws IOException {

        Id orgId = new SimpleId("organization");
        OrganizationScope orgScope = new OrganizationScopeImpl( orgId );
        Id appId = new SimpleId("application");
        CollectionScope appScope = new CollectionScopeImpl( orgId, appId, "test-app" );
        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "characters" );

        EntityManagerFacade em = new EntityManagerFacade( orgScope, appScope, cmf, cif );

        int limit = 2000;
        StopWatch timer = new StopWatch();
        timer.start();

        for ( int i = 1; i <= limit; i++ ) { 
            em.create("character", new LinkedHashMap<String, Object>() {{
                put( "username", RandomStringUtils.random(20) );
                put( "email", RandomStringUtils.random(20) );
                put( "location", new HashMap<String, Object>() {{
                    put("latitude", 140 );
                    put("longitude", 40 );
                }});
            }});
            if ( i % 1000 == 0 ) {
                log.info("   Wrote and indexed: " + i);
            }
        }
        timer.stop();
        log.info( "Total time to index {} entries {}ms", limit, timer.getTime() );
        timer.reset();

        em.refreshIndex();

        timer.start();

        Results results = em.searchCollection( 
            null, "characters", Query.fromQL("location > 10") );

        int count = 0;
        for ( Entity entity : results.getEntities() ) {
            assertNotNull("Returned has a id", entity.getId());
            assertNotNull("Returned has a version", entity.getVersion());
            count++;
            if ( count % 1000 == 0 ) {
                log.info("   Wrote and indexed: " + count);
            }
        }
        timer.stop();
        log.info( "Total time to query & read {} entries {}ms", count, timer.getTime() );
    }
}
