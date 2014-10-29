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
package org.apache.usergrid.persistence.index;

import org.apache.usergrid.persistence.model.entity.Id;


public interface IndexScope {

    /**
     * @return The name of the index. If you use pluralization for you names vs types,
     * you must keep the consistent or you will be unable to load data
     */
    public String getName();

    /**
     * @return A uuid that is unique to this context.  It can be any uuid (time uuid preferred). 
     * Can be an application id if this is indexed in a collection, or the collection owner.  
     * In a graph structure, this will be the source node in the graph
     */
    public Id getOwner();
}
