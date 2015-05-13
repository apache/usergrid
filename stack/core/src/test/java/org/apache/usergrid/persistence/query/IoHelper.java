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

package org.apache.usergrid.persistence.query;


import java.util.Map;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Query;


/**
 * Interface to abstract actually doing I/O targets. The same test logic can be applied to both collections and
 * connections
 *
 * @author tnine
 */
public interface IoHelper {
    /** Perform any setup required */
    public void doSetup() throws Exception;

    /**
     * Write the entity to the data store
     *
     * @param entity the entity
     */
    public Entity writeEntity( Map<String, Object> entity ) throws Exception;

    /**
     * Get the results for the query
     *
     * @param query the query to get results for
     *
     * @return the results of the query
     */
    public Results getResults( Query query ) throws Exception;
}


