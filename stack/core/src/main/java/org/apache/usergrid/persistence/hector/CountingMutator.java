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
package org.apache.usergrid.persistence.hector;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.HCounterSuperColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;


/**
 * Mutator proxy that automatically flushes mutations when they reach a size of 1k
 */
public class CountingMutator<K> implements Mutator<K> {

    private static final Logger logger = LoggerFactory.getLogger( CountingMutator.class );

    /**
     * MAX size of pending mutations we'll handle before flushing.  Defaults to 2k, can be changed on purpose
     * for ease of testing
     */
    public static int MAX_SIZE = 2000;

    /**
     * The maximum size a mutation can have
     */
    private final int maxSize;

    private final Mutator<K> target;


    /**
     * Create a mutator that will flush if our max size is reached
     */
    public CountingMutator( final Mutator<K> target, int maxSize ) {
        this.target = target;
        this.maxSize = maxSize;
    }


    @Override
    public <N, V> MutationResult insert( final K key, final String cf, final HColumn<N, V> c ) {
        return target.insert( key, cf, c );
    }


    @Override
    public <SN, N, V> MutationResult insert( final K key, final String cf, final HSuperColumn<SN, N, V> superColumn ) {
        return target.insert( key, cf, superColumn );
    }


    @Override
    public <N> MutationResult delete( final K key, final String cf, final N columnName,
                                      final Serializer<N> nameSerializer ) {
        return target.delete( key, cf, columnName, nameSerializer );
    }


    @Override
    public <N> MutationResult delete( final K key, final String cf, final N columnName,
                                      final Serializer<N> nameSerializer, final long clock ) {
        return target.delete( key, cf, columnName, nameSerializer, clock );
    }


    @Override
    public <SN, N> MutationResult subDelete( final K key, final String cf, final SN supercolumnName, final N columnName,
                                             final Serializer<SN> sNameSerializer,
                                             final Serializer<N> nameSerializer ) {
        return target.subDelete( key, cf, supercolumnName, columnName, sNameSerializer, nameSerializer );
    }


    @Override
    public <SN> MutationResult superDelete( final K key, final String cf, final SN supercolumnName,
                                            final Serializer<SN> sNameSerializer ) {
        return target.superDelete( key, cf, supercolumnName, sNameSerializer );
    }


    @Override
    public <SN> Mutator<K> addSuperDelete( final K key, final String cf, final SN sColumnName,
                                           final Serializer<SN> sNameSerializer ) {
        target.addSuperDelete( key, cf, sColumnName, sNameSerializer );
        checkAndFlush();
        return this;
    }


    @Override
    public <N, V> Mutator<K> addInsertion( final K key, final String cf, final HColumn<N, V> c ) {
        target.addInsertion( key, cf, c );
        checkAndFlush();
        return this;
    }


    @Override
    public <SN, N, V> Mutator<K> addInsertion( final K key, final String cf, final HSuperColumn<SN, N, V> sc ) {
        target.addInsertion( key, cf, sc );
        checkAndFlush();
        return this;
    }


    @Override
    public <N> Mutator<K> addDeletion( final K key, final String cf, final N columnName,
                                       final Serializer<N> nameSerializer ) {
        target.addDeletion( key, cf, columnName, nameSerializer );
        checkAndFlush();
        return this;
    }


    @Override
    public <N> Mutator<K> addDeletion( final K key, final String cf ) {
        target.addDeletion( key, cf );
        checkAndFlush();
        return this;
    }


    @Override
    public <N> Mutator<K> addDeletion( final Iterable<K> keys, final String cf ) {
        target.addDeletion( keys, cf );
        checkAndFlush();
        return this;
    }


    @Override
    public <N> Mutator<K> addDeletion( final Iterable<K> keys, final String cf, final long clock ) {
        target.addDeletion( keys, cf, clock );
        checkAndFlush();
        return this;
    }


    @Override
    public <N> Mutator<K> addDeletion( final K key, final String cf, final long clock ) {
        target.addDeletion( key, cf, clock );
        checkAndFlush();
        return this;
    }


    @Override
    public <N> Mutator<K> addDeletion( final K key, final String cf, final N columnName,
                                       final Serializer<N> nameSerializer, final long clock ) {
        target.addDeletion( key, cf, columnName, nameSerializer, clock );
        checkAndFlush();
        return this;
    }


    @Override
    public <SN, N, V> Mutator<K> addSubDelete( final K key, final String cf, final HSuperColumn<SN, N, V> sc ) {
        target.addSubDelete( key, cf, sc );
        checkAndFlush();
        return this;
    }


    @Override
    public <SN, N, V> Mutator<K> addSubDelete( final K key, final String cf, final HSuperColumn<SN, N, V> sc,
                                               final long clock ) {
        target.addSubDelete( key, cf, sc, clock );
        checkAndFlush();
        return this;
    }


    @Override
    public <SN, N> Mutator<K> addSubDelete( final K key, final String cf, final SN sColumnName, final N columnName,
                                            final Serializer<SN> sNameSerializer, final Serializer<N> nameSerialer ) {
        target.addSubDelete( key, cf, sColumnName, columnName, sNameSerializer, nameSerialer );
        checkAndFlush();
        return this;
    }


    @Override
    public <SN, N> Mutator<K> addSubDelete( final K key, final String cf, final SN sColumnName, final N columnName,
                                            final Serializer<SN> sNameSerializer, final Serializer<N> nameSerialer,
                                            final long clock ) {
        target.addSubDelete( key, cf, sColumnName, columnName, sNameSerializer, nameSerialer, clock );
        checkAndFlush();
        return this;
    }


    @Override
    public MutationResult execute() {
        return target.execute();
    }


    @Override
    public Mutator<K> discardPendingMutations() {
        return target.discardPendingMutations();
    }


    @Override
    public <N> MutationResult insertCounter( final K key, final String cf, final HCounterColumn<N> c ) {
        return target.insertCounter( key, cf, c );
    }


    @Override
    public <SN, N> MutationResult insertCounter( final K key, final String cf,
                                                 final HCounterSuperColumn<SN, N> superColumn ) {
        return target.insertCounter( key, cf, superColumn );
    }


    @Override
    public <N> MutationResult incrementCounter( final K key, final String cf, final N columnName,
                                                final long increment ) {
        return target.incrementCounter( key, cf, columnName, increment );
    }


    @Override
    public <N> MutationResult decrementCounter( final K key, final String cf, final N columnName,
                                                final long increment ) {
        return target.decrementCounter( key, cf, columnName, increment );
    }


    @Override
    public <N> MutationResult deleteCounter( final K key, final String cf, final N columnName,
                                             final Serializer<N> nameSerializer ) {
        return target.deleteCounter( key, cf, columnName, nameSerializer );
    }


    @Override
    public <SN, N> MutationResult subDeleteCounter( final K key, final String cf, final SN supercolumnName,
                                                    final N columnName, final Serializer<SN> sNameSerializer,
                                                    final Serializer<N> nameSerializer ) {
        return target.subDeleteCounter( key, cf, supercolumnName, columnName, sNameSerializer, nameSerializer );
    }


    @Override
    public <N> Mutator<K> addCounter( final K key, final String cf, final HCounterColumn<N> c ) {
        target.addCounter( key, cf, c );
        checkAndFlush();
        return this;
    }


    @Override
    public <SN, N> Mutator<K> addCounter( final K key, final String cf, final HCounterSuperColumn<SN, N> sc ) {
        target.addCounter( key, cf, sc );
        checkAndFlush();
        return this;
    }


    @Override
    public <N> Mutator<K> addCounterDeletion( final K key, final String cf, final N counterColumnName,
                                              final Serializer<N> nameSerializer ) {
        target.addCounterDeletion( key, cf, counterColumnName, nameSerializer );
        checkAndFlush();
        return this;
    }


    @Override
    public <N> Mutator<K> addCounterDeletion( final K key, final String cf ) {
        target.addCounterDeletion( key, cf );
        checkAndFlush();
        return this;
    }


    @Override
    public <SN, N> Mutator<K> addCounterSubDeletion( final K key, final String cf,
                                                     final HCounterSuperColumn<SN, N> sc ) {
        target.addCounterSubDeletion( key, cf, sc );
        checkAndFlush();
        return this;
    }


    @Override
    public int getPendingMutationCount() {
        return target.getPendingMutationCount();
    }


    /**
     * If our size is > than our max, we'll flush
     */
    public void checkAndFlush() {

        if ( target.getPendingMutationCount() >= maxSize ) {
            logger.info( "Max mutation size of {} reached.  Flushing", maxSize);
            target.execute();
        }
    }


    /**
     * Create a mutator that will flush when the maximum size is reached
     * @param keyspace
     * @param keySerializer
     * @param <K>
     * @return
     */
    public static <K> CountingMutator<K> createFlushingMutator( Keyspace keyspace, Serializer<K> keySerializer ) {
        Mutator<K> target = HFactory.createMutator( keyspace, keySerializer );

        return new CountingMutator<K>( target, MAX_SIZE );
    }
}
