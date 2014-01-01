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


import java.util.Collection;
import java.util.List;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;


/**
 * Test basic operation of change log
 */
@RunWith( JukitoRunner.class )
public class ChangeLogGeneratorImplTest {
    private static final Logger LOG = LoggerFactory.getLogger( ChangeLogGeneratorImplTest.class );

    @ClassRule
    public static CassandraRule rule = new CassandraRule();

    @Inject
    private EntityCollectionManagerFactory factory;

    @Inject
    MvccEntitySerializationStrategy mvccEntitySerializationStrategy;

    public ChangeLogGeneratorImplTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }


    /**
     * Test that change log creation follows Todd's example. 
     */
    @Test
    public void testBasicOperation() throws ConnectionException {

        LOG.info("ChangeLogGeneratorImpl test");

        // create an entity and make a series of changes to it so that versions get created
        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ), new SimpleId( "test" ), "test" );

        // Todd's example:
        //
        // V1 : { "name" : "name1" , "count": 1}
        // V2:  { "name" : "name2" , "count": 2, "nickname" : "buddy"}
        // V3:  { "name" : "name3" , "count": 2}
        
        EntityCollectionManager manager = factory.createCollectionManager( context );
        Entity e1 = new Entity( new SimpleId( "test" ) );
        e1.setField( new StringField( "name", "name1" ) );
        e1.setField( new IntegerField( "count", 1 ) );
        Observable<Entity> o1 = manager.write( e1 );
        e1 = o1.toBlockingObservable().lastOrDefault( null );

        Entity e2 = manager.load( e1.getId() ).toBlockingObservable().lastOrDefault( null );
        e2.setField( new StringField( "name", "name2" ) );
        e2.setField( new IntegerField( "count", 2 ) );
        e2.setField( new StringField( "nickname", "buddy" ) );
        Observable<Entity> o2 = manager.write( e2 );
        e2 = o2.toBlockingObservable().lastOrDefault( null );

        Entity e3 = manager.load( e1.getId() ).toBlockingObservable().lastOrDefault( null );
        e3.setField( new StringField( "name", "name3" ) );
        e3.setField( new IntegerField( "count", 2 ) );
        e3.getFields().remove(new StringField( "nickname", "buddy"));
        Observable<Entity> o3 = manager.write( e3 );
        e3 = o3.toBlockingObservable().lastOrDefault( null );

        {
            // test minVersion of e3
            // 
            // based on that data we expect something like this:
            //
            // Type = PROPERTY_WRITE, Property = count, Value = 2, Versions = [cd54818c-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_WRITE, Property = name, Value = name3, Versions = [cd54818c-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_DELETE, Property = nickname, Value = buddy, Versions = [cd53be3a-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_DELETE, Property = name, Value = name2, Versions = [cd53be3a-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_DELETE, Property = count, Value = 1, Versions = [cd47b048-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_DELETE, Property = name, Value = name1, Versions = [cd47b048-67f6-11e3-945d-cae0eb411d00]

            List<MvccEntity> versions = mvccEntitySerializationStrategy
               .load( context, e1.getId(), e3.getVersion(), 10);

            ChangeLogGeneratorImpl instance = new ChangeLogGeneratorImpl();
            List<ChangeLogEntry> result = instance.getChangeLog( versions, e3.getVersion() ); // minVersion = e3

            for (ChangeLogEntry cle : result) {
                LOG.info( cle.toString() );
                Assert.assertFalse( cle.getVersions().isEmpty() );
            }
            Assert.assertEquals( 6, result.size() );
            Assert.assertTrue( isAscendingOrder( result ) );

            Assert.assertEquals( ChangeLogEntry.ChangeType.PROPERTY_WRITE, result.get( 1 ).getChangeType() );
            Assert.assertEquals( "name3", result.get( 1 ).getField().getValue() );

            Assert.assertEquals( ChangeLogEntry.ChangeType.PROPERTY_DELETE, result.get( 5 ).getChangeType() );
            Assert.assertEquals( "name1", result.get( 5 ).getField().getValue() );
        }
       
        {

            // test minVersion of e2
            // 
            // based on that data we expect something like this:
            //
            // Type = PROPERTY_WRITE, Property = count, Value = 2, Versions = [cd53be3a-67f6-11e3-945d-cae0eb411d00, cd54818c-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_WRITE, Property = name, Value = name3, Versions = [cd54818c-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_WRITE, Property = nickname, Value = buddy, Versions = [cd53be3a-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_WRITE, Property = name, Value = name2, Versions = [cd53be3a-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_DELETE, Property = count, Value = 1, Versions = [cd47b048-67f6-11e3-945d-cae0eb411d00]
            // Type = PROPERTY_DELETE, Property = name, Value = name1, Versions = [cd47b048-67f6-11e3-945d-cae0eb411d00]

            List<MvccEntity> versions = mvccEntitySerializationStrategy
               .load( context, e1.getId(), e3.getVersion(), 10);

            ChangeLogGeneratorImpl instance = new ChangeLogGeneratorImpl();
            List<ChangeLogEntry> result = instance.getChangeLog( versions, e2.getVersion() ); // minVersion = e2

            for (ChangeLogEntry cle : result) {
                LOG.info( cle.toString() );
                Assert.assertFalse( cle.getVersions().isEmpty() );
            }
            Assert.assertEquals(6, result.size() );
            Assert.assertTrue( isAscendingOrder( result ) );

            Assert.assertEquals( ChangeLogEntry.ChangeType.PROPERTY_WRITE, result.get( 2 ).getChangeType() );
            Assert.assertEquals( "buddy", result.get( 2 ).getField().getValue() );

            Assert.assertEquals( ChangeLogEntry.ChangeType.PROPERTY_DELETE, result.get( 4 ).getChangeType() );
            Assert.assertEquals( "count", result.get( 4 ).getField().getName() );
        }
    }

    public static boolean isAscendingOrder( Collection<ChangeLogEntry> col ) {
        Comparable previous = null;
        for ( Comparable item : col ) {
            if ( previous == null ) {
                previous = item;
                continue;
            } 
            int comparedToPrevious = item.compareTo( previous );
            if ( comparedToPrevious < 0 ) {
                return false;
            }
        }
        return true;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public static class TestModule extends JukitoModule {

        @Override
        protected void configureTest() {
            install( new CollectionModule() );
        }
    }
}

