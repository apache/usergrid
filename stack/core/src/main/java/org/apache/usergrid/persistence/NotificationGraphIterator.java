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
            EntityRef ref = source.next();
            Results r = getResultsFor(ref);
            if (r.size() > 0) {
                currentIterator = new PagingResultsIterator(r, query.getResultsLevel());
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

            if (query.getCollection() != null) {

                if(logger.isTraceEnabled()) {
                    logger.trace("Fetching with refType: {}, collection: {} with no query",
                        ref.getType(), query.getCollection());
                }
                return entityManager.searchCollection(ref, query.getCollection(), null);

            } else {

                if(logger.isTraceEnabled()) {
                    logger.trace("Searching target entities with refType: {} for collection: {}  with no query",
                        ref.getType(), query.getCollection());
                }

                query.setQl("select *");
                return entityManager.searchTargetEntities(ref, query);

            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
