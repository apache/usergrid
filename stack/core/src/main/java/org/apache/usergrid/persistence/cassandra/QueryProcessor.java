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

package org.apache.usergrid.persistence.cassandra;

import java.nio.ByteBuffer;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import org.apache.usergrid.persistence.schema.CollectionInfo;


public interface QueryProcessor {
    int PAGE_SIZE = 1000;

    /**
     * Apply cursor position and sort order to this slice. This should only be invoke at evaluation time to ensure that
     * the IR tree has already been fully constructed
     */
    void applyCursorAndSort(QuerySlice slice);

    CollectionInfo getCollectionInfo();

    /**
     * Return the node id from the cursor cache
     * @param nodeId
     * @return
     */
    ByteBuffer getCursorCache(int nodeId);

    EntityManager getEntityManager();

    QueryNode getFirstNode();

    /** @return the pageSizeHint */
    int getPageSizeHint(QueryNode node);

    Query getQuery();

    /** Return the iterator results, ordered if required */
    Results getResults(SearchVisitor visitor) throws Exception;

    void setQuery(Query query);
    
}
