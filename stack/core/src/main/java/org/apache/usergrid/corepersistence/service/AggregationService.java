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
package org.apache.usergrid.corepersistence.service;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.SearchEdge;

import java.util.Map;

/**
 * Service to retrieve aggregations by application scope.
 */
public interface AggregationService {


    /**
     * get entity size for app
     *
     * @param applicationScope
     * @return
     */
    long getApplicationSize(ApplicationScope applicationScope);
    /**
     * get entity size for app
     *
     * @param applicationScope
     * @return
     */
    Map<String,Long> getEachCollectionSize(ApplicationScope applicationScope);

    /**
     * get total entity size for an edge
     *
     * @param applicationScope
     * @param edge
     * @return
     */
    long getSize(final ApplicationScope applicationScope, final SearchEdge edge);

    /**
     * get getSize by collection name
     * @param applicationScope
     * @param collectionName
     * @return
     */
    long getCollectionSize(final ApplicationScope applicationScope, final String collectionName);
}
