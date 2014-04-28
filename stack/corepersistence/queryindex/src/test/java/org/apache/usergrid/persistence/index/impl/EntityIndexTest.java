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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.impl.OrganizationScopeImpl;
import org.apache.usergrid.persistence.collection.util.EntityBuilder;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(JukitoRunner.class)
@UseModules({ TestIndexModule.class })
public class EntityIndexTest {

    private static final Logger log = LoggerFactory.getLogger( EntityIndexTest.class );
    
    @ClassRule
    public static CassandraRule cass = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;
        
    @Inject
    public EntityIndexFactory cif;    
    
    @Inject
    public EntityCollectionManagerFactory cmf;

    @Test
    public void testIndex() throws IOException {

        Id orgId = new SimpleId("organization");
        OrganizationScope orgScope = new OrganizationScopeImpl( orgId );
        Id appId = new SimpleId("application");
        CollectionScope appScope = new CollectionScopeImpl( orgId, appId, "test-app" );
        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "contacts" );

        EntityIndex entityIndex = cif.createEntityIndex(orgScope, appScope );
        EntityCollectionManager entityManager = cmf.createCollectionManager( scope );

        InputStream is = this.getClass().getResourceAsStream( "/sample-large.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> sampleJson = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        int count = 0;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Object o : sampleJson ) {

            Map<String, Object> item = (Map<String, Object>)o;

            Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), scope.getName()));
            entity = EntityBuilder.fromMap( scope.getName(), entity, item );
            EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );

            entity = entityManager.write( entity ).toBlockingObservable().last();

            entityIndex.index( scope, entity );

            count++;
        }
        timer.stop();
        log.info( "Total time to index {} entries {}ms, average {}ms/entry", 
            count, timer.getTime(), timer.getTime() / count );

        entityIndex.refresh();

        testQueries( entityIndex, scope );
    }

    
    @Test 
    public void testDeindex() {

        Id orgId = new SimpleId("organization");
        OrganizationScope orgScope = new OrganizationScopeImpl( orgId );
        Id appId = new SimpleId("application");
        CollectionScope appScope = new CollectionScopeImpl( orgId, appId, "test-app" );
        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "fastcars" );

        EntityIndex entityIndex = cif.createEntityIndex(orgScope, appScope );
        EntityCollectionManager entityManager = cmf.createCollectionManager( scope );

        Map entityMap = new HashMap() {{
            put("name", "Ferrari 212 Inter");
            put("introduced", 1952);
            put("topspeed", 215);
        }};

        Entity entity = EntityBuilder.fromMap( scope.getName(), entityMap );
        EntityUtils.setId( entity, new SimpleId( "fastcar" ));
        entity = entityManager.write( entity ).toBlockingObservable().last();
        entityIndex.index( scope, entity );

        entityIndex.refresh();

        Results results = entityIndex.search( scope, Query.fromQL( "name contains 'Ferrari*'"));
        assertEquals( 1, results.getEntities().size() );

        entityManager.delete( entity.getId() );
        entityIndex.deindex( scope, entity );

        entityIndex.refresh();

        results = entityIndex.search( scope, Query.fromQL( "name contains 'Ferrari*'"));
        assertEquals( 0, results.getEntities().size() );
    }
   
   
    private void testQuery( EntityIndex entityIndex, CollectionScope scope, String queryString, int num ) {

        StopWatch timer = new StopWatch();
        timer.start();
        Query query = Query.fromQL( queryString );
        query.setLimit( 1000 );
        Results results = entityIndex.search( scope, query );
        timer.stop();

        if ( num == 1 ) {
            assertNotNull( results.getEntities().get(0) != null );
        } else {
            assertEquals( num, results.getEntities().size() );
        }
        log.debug( "Query time {}ms", timer.getTime() );
    }


    private void testQueries( EntityIndex entityIndex, CollectionScope scope ) {

        testQuery( entityIndex, scope, "name = 'Morgan Pierce'", 1);

        testQuery( entityIndex, scope, "name = 'morgan pierce'", 1);

        testQuery( entityIndex, scope, "name = 'Morgan'", 0);

        testQuery( entityIndex, scope, "name contains 'Morgan'", 1);

        testQuery( entityIndex, scope, "company > 'GeoLogix'", 564);

        testQuery( entityIndex, scope, "gender = 'female'", 433);

        testQuery( entityIndex, scope, "name = 'Minerva Harrell' and age > 39", 1);

        testQuery( entityIndex, scope, "name = 'Minerva Harrell' and age > 39 and age < 41", 1);

        testQuery( entityIndex, scope, "name = 'Minerva Harrell' and age > 40", 0);

        testQuery( entityIndex, scope, "name = 'Minerva Harrell' and age >= 40", 1);

        testQuery( entityIndex, scope, "name = 'Minerva Harrell' and age <= 40", 1);
    }

       
    @Test
    public void testEntityToMap() throws IOException {

        InputStream is = this.getClass().getResourceAsStream( "/sample-small.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> contacts = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        for ( Object o : contacts ) {

            Map<String, Object> map1 = (Map<String, Object>)o;

            // convert map to entity
            Entity entity1 = EntityBuilder.fromMap( "testscope", map1 );

            // convert entity back to map
            Map map2 = EsEntityIndexImpl.entityToMap( entity1 );

            // the two maps should be the same except for six new system properties
            Map diff = Maps.difference( map1, map2 ).entriesDiffering();
            assertEquals( 6, diff.size() );
        }
    }
}
