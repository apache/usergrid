/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;


/** @author tnine */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public abstract class MvccLogEntrySerializationStrategyImplTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    private MvccLogEntrySerializationStrategy logEntryStrategy;


    @Before
    public void wireLogEntryStrategy() {
        logEntryStrategy = getLogEntryStrategy();
    }


    /**
     * Get the log entry strategy from
     */
    protected abstract MvccLogEntrySerializationStrategy getLogEntryStrategy();


    @Test
    public void createAndDelete() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final Id id = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();

        for ( Stage stage : Stage.values() ) {
            MvccLogEntry saved = new MvccLogEntryImpl( id, version, stage, MvccLogEntry.State.COMPLETE );
            logEntryStrategy.write( context, saved ).execute();

            //Read it back

            MvccLogEntry returned =
                logEntryStrategy.load( context, Collections.singleton( id ), version ).getMaxVersion( id );

            assertNotNull( "Returned value should not be null", returned );

            assertEquals( "Returned should equal the saved", saved, returned );
        }
    }


    @Test
    public void loadNoData() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final Id id = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();


        MvccLogEntry returned =
            logEntryStrategy.load( context, Collections.singleton( id ), version ).getMaxVersion( id );

        assertNull( "Returned value should not exist", returned );
    }


    @Test
    public void getMultipleEntries() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final Id id = new SimpleId( "test" );

        int count = 10;

        final UUID[] versions = new UUID[count];
        final Stage COMPLETE = Stage.COMPLETE;
        final MvccLogEntry[] entries = new MvccLogEntry[count];


        for ( int i = 0; i < count; i++ ) {
            versions[i] = UUIDGenerator.newTimeUUID();

            entries[i] = new MvccLogEntryImpl( id, versions[i], COMPLETE, MvccLogEntry.State.COMPLETE );
            logEntryStrategy.write( context, entries[i] ).execute();

            //Read it back

            MvccLogEntry returned =
                logEntryStrategy.load( context, Collections.singleton( id ), versions[i] ).getMaxVersion( id );

            assertNotNull( "Returned value should not be null", returned );

            assertEquals( "Returned should equal the saved", entries[i], returned );
        }

        //now do a range scan from the end

        List<MvccLogEntry> results = logEntryStrategy.load( context, id, versions[count - 1], count );

        assertEquals( count, results.size() );

        for ( int i = 0; i < count; i++ ) {
            final MvccLogEntry saved = entries[count - i - 1];
            final MvccLogEntry returned = results.get( i );

            assertEquals( "Entry was not equal to the saved value", saved, returned );
        }

        //now delete them all and ensure we get no results back
        for ( int i = 0; i < count; i++ ) {
            logEntryStrategy.delete( context, id, versions[i] ).execute();
        }

        results = logEntryStrategy.load( context, id, versions[versions.length - 1], versions.length );

        assertEquals( "All log entries were deleted", 0, results.size() );
    }


    @Test
    public void getReversedEntries() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final Id id = new SimpleId( "test" );

        int count = 10;

        final UUID[] versions = new UUID[count];
        final Stage COMPLETE = Stage.COMPLETE;
        final MvccLogEntry[] entries = new MvccLogEntry[count];


        for ( int i = 0; i < count; i++ ) {
            versions[i] = UUIDGenerator.newTimeUUID();

            entries[i] = new MvccLogEntryImpl( id, versions[i], COMPLETE, MvccLogEntry.State.COMPLETE );
            logEntryStrategy.write( context, entries[i] ).execute();

            //Read it back

            MvccLogEntry returned =
                logEntryStrategy.load( context, Collections.singleton( id ), versions[i] ).getMaxVersion( id );

            assertNotNull( "Returned value should not be null", returned );

            assertEquals( "Returned should equal the saved", entries[i], returned );
        }


        final UUID[] assertVersions = Arrays.copyOf( versions, versions.length );

        Arrays.sort( assertVersions, ( v1, v2 ) -> UUIDComparator.staticCompare( v1, v2 ) * -1 );

        //now do a range scan from the end

        final int half = count / 2;

        final List<MvccLogEntry> results = logEntryStrategy.loadReversed( context, id, versions[0], half );

        assertEquals( half, results.size() );

        for ( int i = 0; i < count / 2; i++ ) {
            final MvccLogEntry saved = entries[i];
            final MvccLogEntry returned = results.get( i );

            assertEquals( "Entry was not equal to the saved value", saved, returned );
        }


        //now get the next batch
        final List<MvccLogEntry> results2 = logEntryStrategy.loadReversed( context, id, versions[half], count );

        assertEquals( half, results2.size() );

        for ( int i = 0; i < half; i++ ) {
            final MvccLogEntry saved = entries[half + i];
            final MvccLogEntry returned = results2.get( i );

            assertEquals( "Entry was not equal to the saved value", saved, returned );
        }


        //now delete them all and ensure we get no results back
        for ( int i = 0; i < count; i++ ) {
            logEntryStrategy.delete( context, id, versions[i] ).execute();
        }

        final List<MvccLogEntry> results3 = logEntryStrategy.loadReversed( context, id, null, versions.length );

        assertEquals( "All log entries were deleted", 0, results3.size() );
    }


    @Test
    public void createAndDeleteEntries() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final Id id = new SimpleId( "test" );


        final int size = 10;

        final List<MvccLogEntry> savedEntries = new ArrayList<>( size );

        for ( int i = 0; i < size; i++ ) {
            final UUID version = UUIDGenerator.newTimeUUID();
            MvccLogEntry saved = new MvccLogEntryImpl( id, version, Stage.COMMITTED, MvccLogEntry.State.COMPLETE );
            logEntryStrategy.write( context, saved ).execute();

            savedEntries.add( saved );
        }

        //now test we get them all back

        final List<MvccLogEntry> results = logEntryStrategy.loadReversed( context, id, null, size );

        assertEquals( size, results.size() );

        //assert they're the same
        for ( int i = 0; i < size; i++ ) {
            assertEquals( savedEntries.get( i ), results.get( i ) );
        }

        //now delete them all

        for ( final MvccLogEntry mvccLogEntry : savedEntries ) {
            logEntryStrategy.delete( context, id, mvccLogEntry.getVersion() ).execute();
        }

        //now get them back, should be empty
        final List<MvccLogEntry> emptyResults = logEntryStrategy.loadReversed( context, id, null, size );

        assertEquals( 0, emptyResults.size() );
    }


    @Test( expected = NullPointerException.class )
    public void writeParamsNoContext() throws ConnectionException {
        logEntryStrategy.write( null, mock( MvccLogEntry.class ) );
    }


    @Test( expected = NullPointerException.class )
    public void writeParams() throws ConnectionException {
        logEntryStrategy.write( mock( ApplicationScope.class ), null );
    }


    @Test( expected = NullPointerException.class )
    public void deleteParamContext() throws ConnectionException {
        logEntryStrategy.delete( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID() );
    }


    @Test( expected = NullPointerException.class )
    public void deleteParamEntityId() throws ConnectionException {

        logEntryStrategy
            .delete( new ApplicationScopeImpl( new SimpleId( "organization" ) ), null, UUIDGenerator.newTimeUUID() );
    }


    @Test( expected = NullPointerException.class )
    public void deleteParamVersion() throws ConnectionException {

        logEntryStrategy
            .delete( new ApplicationScopeImpl( new SimpleId( "organization" ) ), new SimpleId( "test" ), null );
    }


    @Test( expected = NullPointerException.class )
    public void loadParamContext() throws ConnectionException {
        logEntryStrategy.load( null, Collections.<Id>emptyList(), UUIDGenerator.newTimeUUID() );
    }


    @Test( expected = NullPointerException.class )
    public void loadParamEntityId() throws ConnectionException {

        logEntryStrategy
            .load( new ApplicationScopeImpl( new SimpleId( "organization" ) ), null, UUIDGenerator.newTimeUUID() );
    }


    @Test( expected = NullPointerException.class )
    public void loadParamVersion() throws ConnectionException {

        logEntryStrategy.load( new ApplicationScopeImpl( new SimpleId( "organization" ) ),
            Collections.<Id>singleton( new SimpleId( "test" ) ), null );
    }


    @Test( expected = NullPointerException.class )
    public void loadListParamContext() throws ConnectionException {
        logEntryStrategy.load( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), 1 );
    }


    @Test( expected = NullPointerException.class )
    public void loadListParamEntityId() throws ConnectionException {

        logEntryStrategy
            .load( new ApplicationScopeImpl( new SimpleId( "organization" ) ), null, UUIDGenerator.newTimeUUID(), 1 );
    }


    @Test( expected = NullPointerException.class )
    public void loadListParamVersion() throws ConnectionException {

        logEntryStrategy
            .load( new ApplicationScopeImpl( new SimpleId( "organization" ) ), new SimpleId( "test" ), null, 1 );
    }


    @Test( expected = IllegalArgumentException.class )
    public void loadListParamSize() throws ConnectionException {

        logEntryStrategy.load( new ApplicationScopeImpl( new SimpleId( "organization" ) ), new SimpleId( "test" ),
            UUIDGenerator.newTimeUUID(), 0 );
    }
}

