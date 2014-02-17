/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence;


import java.util.Iterator;
import java.util.UUID;


/**
 * For each in a set of source UUIDs, executes a sub-query and provides a unified iterator over the union of all
 * results. Honors page sizes for the Query to ensure memory isn't blown out.
 */
public class MultiQueryIterator implements Iterator {

    private EntityManager entityManager;
    private Iterator<UUID> source;
    private Query query;
    private MutableEntityRef entityRef = new MutableEntityRef();
    private Iterator currentIterator;


    public MultiQueryIterator( Results results, Query query ) {
        this( results.getQueryProcessor().getEntityManager(), new PagingResultsIterator( results, Results.Level.IDS ),
                query );
    }


    public MultiQueryIterator( EntityManager entityManager, Iterator<UUID> source, Query query ) {
        if ( query.getCollection() == null && query.getConnectionType() == null ) {
            throw new IllegalArgumentException( "Query must have a collection or connectionType value" );
        }
        this.entityManager = entityManager;
        this.source = source;
        this.query = query;
    }


    @Override
    public boolean hasNext() {
        if ( source == null ) {
            return false;
        }
        if ( currentIterator != null && currentIterator.hasNext() ) {
            return true;
        }
        while ( source.hasNext() ) {
            UUID uuid = source.next();
            Results r = getResultsFor( uuid );
            if ( r.size() > 0 ) {
                currentIterator = new PagingResultsIterator( r, query.getResultsLevel() );
                return currentIterator.hasNext();
            }
        }
        currentIterator = null;
        source = null;
        return false;
    }


    @Override
    public Object next() {
        return ( currentIterator != null ) ? currentIterator.next() : null;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }


    private Results getResultsFor( UUID uuid ) {
        entityRef.setUUID( uuid );
        try {
            return ( query.getCollection() != null ) ?
                   entityManager.searchCollection( entityRef, query.getCollection(), query ) :
                   entityManager.searchConnectedEntities( entityRef, query );
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }


    // just avoid some garbage collection
    private static class MutableEntityRef implements EntityRef {

        private UUID uuid;


        public void setUUID( UUID uuid ) {
            this.uuid = uuid;
        }


        @Override
        public UUID getUuid() {
            return uuid;
        }


        @Override
        public String getType() {
            return null;
        }
    }
}
