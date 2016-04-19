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


import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.utils.InflectionUtils;


public class PathQuery<E> {

    private PathQuery source;
    private Query query;
    private UUID uuid;
    private String type;


    public PathQuery() {
    }


    /**
     * top level
     *
     * @param head the top-level entity
     */
    public PathQuery( EntityRef head ) {
        this.uuid = head.getUuid();
        this.type = head.getType();
        this.query = null;
    }


    /**
     * top level
     *  @param head the top-level entity
     * @param query the query - must have a collection or connectType value set
     */
    public PathQuery(EntityRef head, Query query) {
        if ( query.getCollection() == null && query.getConnectionType() == null ) {
            throw new IllegalArgumentException( "Query must have a collection or connectionType value" );
        }
        this.uuid = head.getUuid();
        this.type = head.getType();
        this.query = query;
    }


    /**
     * chained
     *
     * @param source the source query we're chaining from
     * @param query the query - must have a collection or connectType value set
     */
    public PathQuery( PathQuery source, Query query ) {
        if ( query.getCollection() == null && query.getConnectionType() == null ) {
            throw new IllegalArgumentException( "Query must have a collection or connectionType value" );
        }
        this.source = source;
        this.query = query;
    }


    public PathQuery chain( Query query ) {
        return new PathQuery( this, query );
    }


    public Iterator<E> iterator( EntityManager em ) {
        try {

            if ( uuid != null && type != null ) {
                return new PagingResultsIterator( getHeadResults( em ), query.getResultsLevel(), null);
            }
            else {
                return new MultiQueryIterator( em, source.refIterator( em, false), query );
            }
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public Iterator<E> graphIterator( EntityManager em ) {
        try {

            if ( uuid != null && type != null ) {
                return new PagingResultsIterator( getHeadResults( em ), query.getResultsLevel(), null);
            }else {

                return new NotificationGraphIterator(em, source.refIterator(em, true), query);
            }

        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }




    protected Results getHeadResults( EntityManager em ) throws Exception {

        EntityRef ref = new SimpleEntityRef(type,uuid);

        // if it's a single name identifier, just directly fetch that
        if ( !query.getQl().isPresent() && query.getSingleNameOrEmailIdentifier() != null){

            String name = query.getSingleNameOrEmailIdentifier();
            String entityType = InflectionUtils.singularize(query.getCollection());

            UUID entityId = em.getUniqueIdFromAlias( entityType, name );

            if( entityId == null){
                throw new
                    IllegalArgumentException("Entity with name "+name+" not found. Unable to send push notification");
            }


            return em.getEntities(Collections.singletonList(entityId), entityType);
        }

        return ( query.getCollection() != null ) ?
               em.searchCollection( ref, query.getCollection(), query ) :
               em.searchTargetEntities(ref, query);
    }


    protected Iterator refIterator(EntityManager em, boolean useGraph) throws Exception {

        if ( query.getQl() == null && query.getSingleNameOrEmailIdentifier() != null){

            return new PagingResultsIterator( getHeadResults( em ), Level.REFS, null);

        }

        if ( type != null  && uuid != null) {
            return new PagingResultsIterator( getHeadResults( em ), Level.REFS, null);
        }
        else {
            Query q = query;
            if ( query.getResultsLevel() != Level.REFS ) { // ensure REFS level
                q = new Query( q );
                q.setResultsLevel( Level.REFS );
            }
            if( useGraph){
                return new NotificationGraphIterator( em, source.refIterator( em, true), q );
            }else{
                return new MultiQueryIterator( em, source.refIterator( em, false ), q );

            }
        }
    }


    public PathQuery getSource() {
        return source;
    }


    public String getType(){return type;}

    public UUID getUuid(){return uuid;}

    public Query getQuery() {
        return query;
    }
}
