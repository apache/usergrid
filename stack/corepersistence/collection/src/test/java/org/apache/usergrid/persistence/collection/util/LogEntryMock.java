package org.apache.usergrid.persistence.collection.util;/*
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;


/**
 * Utility for constructing representative log entries for mock serialziation from high to low
 */
public class LogEntryMock {


    private final TreeMap<UUID, MvccLogEntry> entries = new TreeMap<>(ReversedUUIDComparator.INSTANCE);

    private final Id entityId;


    /**
     * Create a mock list of versions of the specified size
     *
     * @param entityId The entity Id to use
     * @param versions The versions to use
     */
    private LogEntryMock(final Id entityId, final List<UUID> versions ) {

        this.entityId = entityId;

        for ( UUID version: versions) {
            entries.put( version, new MvccLogEntryImpl( entityId, version, Stage.ACTIVE, MvccLogEntry.State.COMPLETE ) );
        }
    }


    /**
     * Init the mock with the given data structure
     * @param logEntrySerializationStrategy The strategy to moc
     * @param scope
     * @throws ConnectionException
     */
    private void initMock(  final MvccLogEntrySerializationStrategy logEntrySerializationStrategy, final ApplicationScope scope )

            throws ConnectionException {

        //wire up the mocks
        when(logEntrySerializationStrategy.load( same( scope ), same( entityId ), any(UUID.class), any(Integer.class)  )).thenAnswer( new Answer<List<MvccLogEntry>>() {


            @Override
            public List<MvccLogEntry> answer( final InvocationOnMock invocation ) throws Throwable {
                final UUID startVersion = ( UUID ) invocation.getArguments()[2];
                final int count = (Integer)invocation.getArguments()[3];

                final List<MvccLogEntry> results = new ArrayList<>( count );

                final Iterator<MvccLogEntry> itr = entries.tailMap( startVersion, true ).values().iterator();

                for(int i = 0; i < count && itr.hasNext(); i ++){
                    results.add( itr.next() );
                }


                return results;
            }
        } );
    }


    /**
     * Get the entry at the specified index from high to low
     * @param index
     * @return
     */
    public MvccLogEntry getEntryAtIndex(final int index){

        final Iterator<MvccLogEntry> itr = entries.values().iterator();

        for(int i = 0; i < index; i ++){
           itr.next();
        }

        return itr.next();
    }


    /**
     *
     * @param logEntrySerializationStrategy The mock to use
     * @param scope The scope to use
     * @param entityId The entityId to use
     * @param versions The versions to mock
     * @throws ConnectionException
     */
    public static LogEntryMock createLogEntryMock(final MvccLogEntrySerializationStrategy logEntrySerializationStrategy, final  ApplicationScope scope,final Id entityId, final List<UUID> versions )

            throws ConnectionException {

        LogEntryMock mock = new LogEntryMock( entityId, versions );
        mock.initMock( logEntrySerializationStrategy, scope );

        return mock;
    }


    public Collection<MvccLogEntry> getEntries() {
        return entries.values();
    }


    private static final class ReversedUUIDComparator implements Comparator<UUID> {

        public static final ReversedUUIDComparator INSTANCE = new ReversedUUIDComparator();


        @Override
        public int compare( final UUID o1, final UUID o2 ) {
            return UUIDComparator.staticCompare( o1, o2 ) * -1;
        }
    }
}
