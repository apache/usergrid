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
package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Simple integration test of uniqueness verification.
 */
@RunWith( JukitoRunner.class )
@UseModules( TestCollectionModule.class )
public class WriteUniqueVerifyIT {

    @ClassRule
    public static CassandraRule rule = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    public EntityCollectionManagerFactory cmf;

    @Test
    public void testConflict() {

        final Id orgId = new SimpleId("WriteUniqueVerifyIT");
        final Id appId = new SimpleId("testConflict");

        final CollectionScope scope = new CollectionScopeImpl( appId, orgId, "fastcars" );
        final EntityCollectionManager entityManager = cmf.createCollectionManager( scope );

        final Entity entity = TestEntityGenerator.generateEntity();
        entity.setField(new StringField("name", "Aston Martin Vanquish", true));
        entity.setField(new StringField("identifier", "v12", true));
        entity.setField(new IntegerField("top_speed_mph", 200));
        entityManager.write( entity ).toBlockingObservable().last();

        // another enity that tries to use two unique values already taken by first
        final Entity entity2 = TestEntityGenerator.generateEntity();
        entity2.setField(new StringField("name", "Aston Martin Vanquish", true));
        entity2.setField(new StringField("identifier", "v12", true));
        entity2.setField(new IntegerField("top_speed_mph", 120));

        try {
            entityManager.write( entity2 ).toBlockingObservable().last();
            fail("Write should have thrown an exception");

        } catch ( WriteUniqueVerifyException e ) {
            // verify two unique value violations
            assertEquals( 2, e.getVioliations().size() );
        }

    }

}


