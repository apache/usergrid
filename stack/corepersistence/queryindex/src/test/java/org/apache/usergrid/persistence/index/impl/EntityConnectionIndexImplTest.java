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


import java.io.IOException;
import java.util.HashMap;

import org.jukito.UseModules;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith( ITRunner.class )
@UseModules( { TestIndexModule.class } )
public class EntityConnectionIndexImplTest extends BaseIT {

    private static final Logger log = LoggerFactory.getLogger( EntityConnectionIndexImplTest.class );

    @ClassRule
    public static ElasticSearchRule es = new ElasticSearchRule();

    @ClassRule
    public static CassandraRule cass = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    public EntityIndexFactory ecif;

    @Test
    public void testBasicOperation() throws IOException {

        Id appId = new SimpleId( "application" );

        // create a muffin
        CollectionScope muffinScope = new CollectionScopeImpl( appId, appId, "muffins" );
        Entity muffin = new Entity( new SimpleId( UUIDGenerator.newTimeUUID(), muffinScope.getName() ) );

        muffin = EntityIndexMapUtils.fromMap( muffin, new HashMap<String, Object>() {{
            put( "size", "Large" );
            put( "flavor", "Blueberry" );
        }} );
        EntityUtils.setVersion( muffin, UUIDGenerator.newTimeUUID() );


        // create a person who likes muffins
        CollectionScope peopleScope = new CollectionScopeImpl( appId, appId, "people" );
        Entity person = new Entity( new SimpleId( UUIDGenerator.newTimeUUID(), peopleScope.getName() ) );
        person = EntityIndexMapUtils.fromMap( person, new HashMap<String, Object>() {{
            put( "name", "Dave" );
            put( "hometown", "Chapel Hill" );
        }} );
        EntityUtils.setVersion( person, UUIDGenerator.newTimeUUID() );


        assertNotNull( person.getId() );
        assertNotNull( person.getId().getUuid() );

        // index connection of "person Dave likes Large Blueberry muffin"

        IndexScope scope = new IndexScopeImpl( appId, person.getId(), "likes" );

        EntityIndex personLikesIndex = ecif.createEntityIndex( scope );
        personLikesIndex.index( muffin );

        personLikesIndex.refresh();

        // now, let's search for things that Dave likes
        CandidateResults likes = personLikesIndex.search( Query.fromQL( "select *" ) );
        assertEquals( 1, likes.size() );
        assertEquals(muffin.getId(), likes.get(0).getId());

    }
}
