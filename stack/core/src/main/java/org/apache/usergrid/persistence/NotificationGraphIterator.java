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


import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class NotificationGraphIterator implements ResultsIterator, Iterable {

    private static final Logger logger = LoggerFactory.getLogger(NotificationGraphIterator.class);


    EntityManager entityManager;

    private Iterator<EntityRef> source;
    private Query query;
    private Iterator currentIterator;


    public NotificationGraphIterator(EntityManager entityManager,
                                     Iterator<EntityRef> source,
                                     Query query) {

        this.entityManager = entityManager;
        this.source = source;
        this.query = query;

    }

    @Override
    public Iterator iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        if (source == null) {
            return false;
        }
        if (currentIterator != null && currentIterator.hasNext()) {
            return true;
        }
        while (source.hasNext()) {
            Object next = source.next();
            Results r;

            EntityRef ref = (EntityRef) next;
            r = getResultsFor(ref);

            if (r.size() > 0) {


                if(ref.getType().equals(Group.ENTITY_TYPE)) {

                    currentIterator = new PagingResultsIterator(r, query.getResultsLevel(), Query.Level.REFS);
                }else{
                    currentIterator = new PagingResultsIterator(r, query.getResultsLevel(), null);

                }

                return currentIterator.hasNext();
            }
        }
        currentIterator = null;
        source = null;
        return false;
    }


    @Override
    public Object next() {




        return (currentIterator != null) ? currentIterator.next() : null;
    }

    @Override
    public boolean hasPages() {
        return currentIterator != null && currentIterator instanceof ResultsIterator && ((ResultsIterator) currentIterator).hasPages();
    }


    private Results getResultsFor(EntityRef ref) {

        try {


            query.setLimit(Query.MAX_LIMIT); // always fetch our MAX limit to reduce # of IO hops
            if (query.getCollection() != null) {

                // make sure this results in graph traversal
                query.setQl("select *");

                if(logger.isTraceEnabled()) {
                    logger.trace("Fetching with refType: {}, collection: {} with no query",
                        ref.getType(), query.getCollection());
                }

                // if we're fetching devices through groups->users->devices, get only the IDs and don't load the entities
                if( ref.getType().equals(Group.ENTITY_TYPE)){

                    // groups->users is a passthrough to devices, load our max limit
                    query.setLimit(Query.MAX_LIMIT);

                    // set the query level for the when fetching users to IDS, we don't need the full entity
                    query.setResultsLevel(Query.Level.IDS);

                 return entityManager.searchCollection(ref, "users", query);

                }

                if( ref.getType().equals(User.ENTITY_TYPE)){

                    Query devicesQuery = new Query();
                    devicesQuery.setCollection("devices");
                    devicesQuery.setResultsLevel(Query.Level.CORE_PROPERTIES);

                    return entityManager.searchCollection(ref, devicesQuery.getCollection(), devicesQuery);
                }

                return entityManager.searchCollection(ref, query.getCollection(), query);


            } else {

                if(logger.isTraceEnabled()) {
                    logger.trace("Searching target entities with refType: {} for collection: {}  with no query",
                        ref.getType(), query.getCollection());
                }

                query.setQl("select *"); // make sure this results in graph traversal
                return entityManager.searchTargetEntities(ref, query);

            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
