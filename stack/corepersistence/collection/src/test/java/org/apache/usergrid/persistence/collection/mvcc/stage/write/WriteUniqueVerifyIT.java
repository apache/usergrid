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


import org.apache.usergrid.persistence.collection.FieldSet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;

import com.google.inject.Inject;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Simple integration test of uniqueness verification.
 */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class WriteUniqueVerifyIT {

    @Inject
    SerializationFig serializationFig;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    public UniqueValueSerializationStrategy uniqueValueSerializationStrategy;

    @Inject
    public EntityCollectionManagerFactory cmf;

    @Test
    public void testConflict() {

        final Id appId = new SimpleId("testConflict");

        final ApplicationScope scope = new ApplicationScopeImpl( appId );
        final EntityCollectionManager entityManager = cmf.createCollectionManager( scope );

        final Entity entity = TestEntityGenerator.generateEntity();
        entity.setField(new StringField("name", "Aston Martin Vanquish", true));
        entity.setField(new StringField("identifier", "v12", true));
        entity.setField(new IntegerField("top_speed_mph", 200));
        entityManager.write( entity ).toBlocking().last();

        Entity entityFetched = entityManager.load( entity.getId() ).toBlocking().last();
        entityFetched.setField( new StringField("foo", "bar"));

        // wait for temporary unique value records to time out
        try {
            Thread.sleep(serializationFig.getTimeout() * 1100);
        } catch (InterruptedException ignored) { }

        // another enity that tries to use two unique values already taken by first
        final Entity entity2 = TestEntityGenerator.generateEntity();
        entity2.setField(new StringField("name", "Aston Martin Vanquish", true));
        entity2.setField(new StringField("identifier", "v12", true));
        entity2.setField(new IntegerField("top_speed_mph", 120));

        try {
            entityManager.write( entity2 ).toBlocking().last();
            fail("Write should have thrown an exception");

        } catch ( Exception ex ) {
            WriteUniqueVerifyException e = (WriteUniqueVerifyException)ex;

            // verify two unique value violations
            assertEquals( 2, e.getVioliations().size() );
        }

        // ensure we can update original entity without error
        entity.setField( new IntegerField("top_speed_mph", 190) );
        entityManager.write( entity );
    }

    @Test
    public void testNoConflict1() {

        final Id appId = new SimpleId("testNoConflict");

        final ApplicationScope scope = new ApplicationScopeImpl( appId);
        final EntityCollectionManager entityManager = cmf.createCollectionManager( scope );

        final Entity entity = TestEntityGenerator.generateEntity();
        entity.setField(new StringField("name", "Porsche 911 GT3", true));
        entity.setField(new StringField("identifier", "911gt3", true));
        entity.setField(new IntegerField("top_speed_mph", 194));
        entityManager.write( entity ).toBlocking().last();

        Entity entityFetched = entityManager.load( entity.getId() ).toBlocking().last();
        entityFetched.setField( new StringField("foo", "baz"));
        entityManager.write( entityFetched ).toBlocking().last();
    }

    @Test
    public void testNoConflict2() {

        final Id appId = new SimpleId("testNoConflict");

        final ApplicationScope scope = new ApplicationScopeImpl( appId );
        final EntityCollectionManager entityManager = cmf.createCollectionManager( scope );

        final Entity entity = TestEntityGenerator.generateEntity();
        entity.setField(new StringField("name", "Alfa Romeo 8C Competizione", true));
        entity.setField(new StringField("identifier", "ar8c", true));
        entity.setField(new IntegerField("top_speed_mph", 182));
        entityManager.write( entity ).toBlocking().last();

        entity.setField( new StringField("foo", "bar"));
        entityManager.write( entity ).toBlocking().last();
    }

    @Test
    public void testConflictReadRepair() throws Exception {

        final Id appId = new SimpleId("testNoConflict");



        final ApplicationScope scope = new ApplicationScopeImpl( appId);

        final EntityCollectionManager entityManager = cmf.createCollectionManager( scope );

        final Entity entity = TestEntityGenerator.generateEntity();
        entity.setField(new StringField("name", "Porsche 911 GT3", true));
        entity.setField(new StringField("identifier", "911gt3", true));
        entity.setField(new IntegerField("top_speed_mph", 194));
        entityManager.write( entity ).toBlocking().last();


        FieldSet fieldSet =
            entityManager.getEntitiesFromFields("test", Collections.singletonList(entity.getField("name")), true)
            .toBlocking().last();

        MvccEntity entityFetched = fieldSet.getEntity( entity.getField("name") );


        final Entity entityDuplicate = TestEntityGenerator.generateEntity();
        UniqueValue uniqueValue = new UniqueValueImpl(new StringField("name", "Porsche 911 GT3", true),
            entityDuplicate.getId(), UUIDGenerator.newTimeUUID());

        // manually insert a record to simulate a 'duplicate' trying to be inserted
        uniqueValueSerializationStrategy.
            write(scope, uniqueValue).execute();



        FieldSet fieldSetAgain =
            entityManager.getEntitiesFromFields("test", Collections.singletonList(entity.getField("name")), true)
                .toBlocking().last();

        MvccEntity entityFetchedAgain = fieldSetAgain.getEntity( entity.getField("name") );

        assertEquals(entityFetched, entityFetchedAgain);


        // now test writing the original entity again ( simulates a PUT )
        // this should read repair and work
        entityManager.write( entity ).toBlocking().last();

        FieldSet fieldSetAgainAgain =
            entityManager.getEntitiesFromFields("test", Collections.singletonList(entity.getField("name")), true)
                .toBlocking().last();

        MvccEntity entityFetchedAgainAgain = fieldSetAgainAgain.getEntity( entity.getField("name") );

        assertEquals(entityFetched, entityFetchedAgainAgain);



    }
}
