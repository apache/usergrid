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
package org.apache.usergrid.persistence.collection.mvcc.changelog;


import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test basic operation of change log
 */
public class ChangeLogGeneratorImplTest {
    private static final Logger LOG = LoggerFactory.getLogger( ChangeLogGeneratorImplTest.class );


    /**
     * Test rolling up 3 versions, properties are added then deleted
     */
    @Test
    public void testBasicOperation() throws ConnectionException {

        LOG.info( "ChangeLogGeneratorImpl test" );


        final Id entityId = new SimpleId( "test" );

        Entity e1 = new Entity( entityId );
        e1.setField( new StringField( "name", "name1" ) );
        e1.setField( new IntegerField( "count", 1 ) );
        e1.setField( new BooleanField( "single", true ) );

        final MvccEntity mvccEntity1 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.COMPLETE, e1 );

        Entity e2 = new Entity( entityId );
        e2.setField( new StringField( "name", "name2" ) );
        e2.setField( new IntegerField( "count", 2 ) );
        e2.setField( new StringField( "nickname", "buddy" ) );
        e2.setField( new BooleanField( "cool", false ) );

        final MvccEntity mvccEntity2 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.PARTIAL, e2 );


        Entity e3 = new Entity( entityId );
        e3.setField( new StringField( "name", "name3" ) );
        e3.setField( new IntegerField( "count", 2 ) );
        //appears in e1, since it's been added again, we want to make sure it doesn't appear in the delete list
        e3.setField( new BooleanField( "single", true ) );

        final MvccEntity mvccEntity3 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.PARTIAL, e3 );


        ChangeLogGeneratorImpl instance = new ChangeLogGeneratorImpl();
        ChangeLog result =
                instance.getChangeLog( Arrays.asList( mvccEntity1, mvccEntity2, mvccEntity3 ) ); // minVersion = e3


        assertEquals( "All changes not present", 2, result.getSize() );


        Collection<Field> changes = result.getWrites();

        assertEquals( 0, changes.size() );

        Set<String> deletes = result.getDeletes();

        assertEquals( 2, deletes.size() );

        assertTrue( deletes.contains( "nickname" ) );
        assertTrue( deletes.contains( "cool" ) );
    }


    /**
     * Test rolling up 3 versions, properties are added then deleted
     */
    @Test
    public void testDeletedVersionFirst() throws ConnectionException {

        LOG.info( "ChangeLogGeneratorImpl test" );


        final Id entityId = new SimpleId( "test" );

        final MvccEntity mvccEntity1 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.DELETED,
                        Optional.<Entity>absent() );

        Entity e2 = new Entity( entityId );
        e2.setField( new StringField( "name", "name2" ) );
        e2.setField( new IntegerField( "count", 2 ) );
        e2.setField( new StringField( "nickname", "buddy" ) );
        e2.setField( new BooleanField( "cool", false ) );

        final MvccEntity mvccEntity2 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.PARTIAL, e2 );


        Entity e3 = new Entity( entityId );
        e3.setField( new StringField( "name", "name3" ) );
        e3.setField( new IntegerField( "count", 2 ) );
        //appears in e1, since it's been added again, we want to make sure it doesn't appear in the delete list
        e3.setField( new BooleanField( "single", true ) );

        final MvccEntity mvccEntity3 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.PARTIAL, e3 );


        ChangeLogGeneratorImpl instance = new ChangeLogGeneratorImpl();
        ChangeLog result =
                instance.getChangeLog( Arrays.asList( mvccEntity1, mvccEntity2, mvccEntity3 ) ); // minVersion = e3


        assertEquals( "All changes not present", 2, result.getSize() );


        Collection<Field> changes = result.getWrites();

        assertEquals( 0, changes.size() );



        Set<String> deletes = result.getDeletes();

        assertEquals( 2, deletes.size() );

        assertTrue( deletes.contains( "nickname" ) );
        assertTrue( deletes.contains( "cool" ) );
    }


    /**
     * Test rolling up 3 versions, properties are added then deleted
     */
    @Test
    public void testDeletedMiddle() throws ConnectionException {

        LOG.info( "ChangeLogGeneratorImpl test" );


        final Id entityId = new SimpleId( "test" );

        Entity e1 = new Entity( entityId );
        e1.setField( new StringField( "name", "name1" ) );
        e1.setField( new IntegerField( "count", 1 ) );
        e1.setField( new BooleanField( "single", true ) );

        final MvccEntity mvccEntity1 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.COMPLETE, e1 );

        Entity e2 = new Entity( entityId );
        e2.setField( new StringField( "name", "name2" ) );
        e2.setField( new IntegerField( "count", 2 ) );
        e2.setField( new StringField( "nickname", "buddy" ) );
        e2.setField( new BooleanField( "cool", false ) );

        final MvccEntity mvccEntity2 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.DELETED, e2 );


        Entity e3 = new Entity( entityId );
        e3.setField( new StringField( "name", "name3" ) );
        e3.setField( new IntegerField( "count", 2 ) );
        //appears in e1, since it's been added again, we want to make sure it doesn't appear in the delete list
        e3.setField( new BooleanField( "single", true ) );

        final MvccEntity mvccEntity3 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.PARTIAL, e3 );


        ChangeLogGeneratorImpl instance = new ChangeLogGeneratorImpl();
        ChangeLog result =
                instance.getChangeLog( Arrays.asList( mvccEntity1, mvccEntity2, mvccEntity3 ) ); // minVersion = e3


        assertEquals( "All changes present", 0, result.getSize() );


        Collection<Field> changes = result.getWrites();

        assertEquals( 0, changes.size() );

        Set<String> deletes = result.getDeletes();

        assertEquals( 0, deletes.size() );
    }


    /**
     * Test rolling up 3 versions, properties are added then deleted
     */
    @Test
    public void testDeletedLast() throws ConnectionException {

        final Id entityId = new SimpleId( "test" );

        Entity e1 = new Entity( entityId );
        e1.setField( new StringField( "name", "name1" ) );
        e1.setField( new IntegerField( "count", 1 ) );
        e1.setField( new BooleanField( "single", true ) );

        final MvccEntity mvccEntity1 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.COMPLETE, e1 );

        Entity e2 = new Entity( entityId );
        e2.setField( new StringField( "name", "name2" ) );
        e2.setField( new IntegerField( "count", 2 ) );
        e2.setField( new StringField( "nickname", "buddy" ) );
        e2.setField( new BooleanField( "cool", false ) );

        final MvccEntity mvccEntity2 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.PARTIAL, e2 );


        final MvccEntity mvccEntity3 =
                new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.DELETED,
                        Optional.<Entity>absent() );


        ChangeLogGeneratorImpl instance = new ChangeLogGeneratorImpl();
        ChangeLog result =
                instance.getChangeLog( Arrays.asList( mvccEntity1, mvccEntity2, mvccEntity3 ) ); // minVersion = e3


        assertEquals( "All changes not present", 0, result.getSize() );


        Collection<Field> changes = result.getWrites();

        assertEquals( 0, changes.size() );

        Set<String> deletes = result.getDeletes();

        assertEquals( 0, deletes.size() );
    }
}

