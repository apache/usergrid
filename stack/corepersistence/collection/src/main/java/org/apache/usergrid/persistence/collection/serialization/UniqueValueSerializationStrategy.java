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
package org.apache.usergrid.persistence.collection.serialization;


import java.util.Collection;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;


/**
 * Reads and writes to UniqueValues column family.
 */
public interface UniqueValueSerializationStrategy extends Migration {

    /**
     * Write the specified UniqueValue to Cassandra with optional timeToLive in milliseconds.
     * 
     * @param uniqueValue Object to be written
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    public MutationBatch write( CollectionScope scope,  UniqueValue uniqueValue );

    /**
     * Write the specified UniqueValue to Cassandra with optional timeToLive in milliseconds.
     * 
     * @param uniqueValue Object to be written
     * @param timeToLive How long object should live in seconds 
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    public MutationBatch write( CollectionScope scope,  UniqueValue uniqueValue, Integer timeToLive );

    /**
     * Load UniqueValue that matches field from collection or null if that value does not exist.
     *
     * @param colScope Collection scope in which to look for field name/value
     * @param fields Field name/value to search for
     *
     * @return UniqueValueSet containing fields from the collection that exist in cassandra
     *
     * @throws ConnectionException on error connecting to Cassandra
     */
    public UniqueValueSet load( CollectionScope colScope, Collection<Field> fields ) throws ConnectionException;


    /**
     * Load UniqueValue that matches field from collection or null if that value does not exist.
     *
     * This set represents all unique values that have been stored for this system and not removed. For instance, if an
     * entity is deleted and the unique field is not, an erroneous non-unique exception could be thrown when the entity
     * is missing, and the unique value is present.  When deleting unique values we should really get an ack on the
     * unique delete before deleting entries from here
     *
     * @param colScope Collection scope in which to look for field name/value
     * @param fields Field name/value to search for
     *
     * @return UniqueValueSet containing fields from the collection that exist in cassandra
     *
     * @throws ConnectionException on error connecting to Cassandra
     */
    public UniqueValueSet loadAllSavedValues( CollectionScope colScope, Id entityId)
            throws ConnectionException;


    /**
     * Delete the specified Unique Value from Cassandra.
     * 
     * @param uniqueValue Object to be deleted.
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    public MutationBatch delete( CollectionScope scope,  UniqueValue uniqueValue );


}
