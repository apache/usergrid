/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index;

import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import rx.Observable;

/**
 * Entity Index for an Application.
 */
public interface ApplicationEntityIndex {


    /**
     * Create the index batch.
     */
    public EntityIndexBatch createBatch();

    /**
     * Execute query in Usergrid syntax.
     */
    public CandidateResults search(final IndexScope indexScope, final SearchTypes searchTypes, final Query query);
    public CandidateResults search(final IndexScope indexScope, final SearchTypes searchTypes, final Query query, final int limit);



    /**
     * get next page of results
     * @param cursor
     * @return
     */
    public CandidateResults getNextPage(final String cursor, final int limit);

    /**
     * delete all application records
     * @return
     */
    public Observable deleteApplication();
}
