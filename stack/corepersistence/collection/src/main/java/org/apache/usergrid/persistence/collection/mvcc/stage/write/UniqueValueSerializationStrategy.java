/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.model.field.Field;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

/**
 * Reads and writes to UniqueValues column family.
 */
public interface UniqueValueSerializationStrategy {

    /**
     * Write the specified UniqueValue to Cassandra with optional timeToLive in milliseconds.
     * 
     * @param uniqueValue Object to be written
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    public MutationBatch write( UniqueValue uniqueValue );

    /**
     * Write the specified UniqueValue to Cassandra with optional timeToLive in milliseconds.
     * 
     * @param uniqueValue Object to be written
     * @param timeToLive How long object should live in seconds 
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    public MutationBatch write( UniqueValue uniqueValue, Integer timeToLive );

    /**
     * Load UniqueValue that matches field from collection or null if that value does not exist.
     * 
     * @param colScope Collection scope in which to look for field name/value
     * @param field Field name/value to search for
     * @return UniqueValue or null if not found
     * @throws ConnectionException on error connecting to Cassandra
     */
    public UniqueValue load( CollectionScope colScope, Field field ) throws ConnectionException;

    /**
     * Delete the specified Unique Value from Cassandra.
     * 
     * @param uniqueValue Object to be deleted.
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    public MutationBatch delete( UniqueValue uniqueValue );
}
