/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.index;


import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Low level queue service for indexing entities
 */
public interface AsyncIndexService extends ReIndexAction {


    /**
     * Queue an entity to be indexed.  This will start processing immediately. For implementations that are realtime (akka, in memory)
     * We will return a distributed future.  For SQS impls, this will return immediately, and the result will not be available.
     * After SQS is removed, the tests should be enhanced to ensure that we're processing our queues correctly.
     * @param applicationScope
     * @param entity The entity to index
     */
    void queueEntityIndexUpdate( final ApplicationScope applicationScope, final Entity entity);

}
