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
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
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
public class EntityConnectionIndexImplTest {

    private static final Logger log = LoggerFactory.getLogger( EntityConnectionIndexImplTest.class );
    
    @ClassRule
    public static CassandraRule cass = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;
        
    @Inject
    public EntityIndexFactory ecif;    

    @Inject
    public EntityCollectionManagerFactory ecmf;

    
    @Test
    public void testBasicOperation() throws IOException { 

        Id orgId = new SimpleId("organization");
        OrganizationScope orgScope = new OrganizationScopeImpl( orgId );

        Id appId = new SimpleId("application");
        CollectionScope appScope = new CollectionScopeImpl( orgId, appId, "test-app" );

        // create a muffin
        CollectionScope muffinScope = new CollectionScopeImpl( appId, orgId, "muffins" );
        Entity muffin = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), muffinScope.getName()));
        muffin = EntityBuilder.fromMap( muffinScope.getName(), muffin, new HashMap<String, Object>() {{
            put("size", "Large");
            put("flavor", "Blueberry");
        }} );
        EntityUtils.setVersion( muffin, UUIDGenerator.newTimeUUID() );
        EntityCollectionManager muffinMgr = ecmf.createCollectionManager( muffinScope );
        muffin = muffinMgr.write( muffin ).toBlockingObservable().last();

        // create a person who likes muffins
        CollectionScope peopleScope = new CollectionScopeImpl( appId, orgId, "people" );
        Entity person = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), peopleScope.getName()));
        person = EntityBuilder.fromMap( peopleScope.getName(), person, new HashMap<String, Object>() {{
            put("name", "Dave");
            put("hometown", "Chapel Hill");
        }} );
        EntityUtils.setVersion( person, UUIDGenerator.newTimeUUID() );
        EntityCollectionManager peopleMgr = ecmf.createCollectionManager( peopleScope );
        person = peopleMgr.write( person ).toBlockingObservable().last();

        assertNotNull( person.getId() );
        assertNotNull( person.getId().getUuid() );

        // index connection of "person Dave likes Large Blueberry muffin"
        EntityIndex personLikesIndex = ecif.createEntityIndex( orgScope, appScope );
        personLikesIndex.indexConnection( person, "likes", muffin, muffinScope );

        personLikesIndex.refresh();

        // now, let's search for things that Dave likes
        Results likes = personLikesIndex.searchConnections( person, "likes", Query.fromQL( "select *"));
        assertEquals( 1, likes.size() );
        assertEquals( "Blueberry", likes.getEntities().get(0).getField("flavor").getValue() );
    }
}
