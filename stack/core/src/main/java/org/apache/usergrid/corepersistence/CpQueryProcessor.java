/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence;

import java.nio.ByteBuffer;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.cassandra.QueryProcessor;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import org.apache.usergrid.persistence.schema.CollectionInfo;


public class CpQueryProcessor implements QueryProcessor {

    Query query;
    EntityManager em;
    EntityRef entityRef;
    String collectionName;

    public CpQueryProcessor( EntityManager em, EntityRef entityRef, String collectionName ) {
        this.em = em;
        this.entityRef = entityRef;
        this.collectionName = collectionName;
    }

    @Override
    public void applyCursorAndSort(QuerySlice slice) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public CollectionInfo getCollectionInfo() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ByteBuffer getCursorCache(int nodeId) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityManager getEntityManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public QueryNode getFirstNode() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public int getPageSizeHint(QueryNode node) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public Results getResults(SearchVisitor visitor) throws Exception {
        return em.searchCollection( entityRef, collectionName, query);
    }

    @Override
    public void setQuery(Query query) {
        this.query = query;
    }
    
}
