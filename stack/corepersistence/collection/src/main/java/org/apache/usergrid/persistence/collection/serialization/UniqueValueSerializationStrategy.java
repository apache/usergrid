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
import java.util.Iterator;

import org.apache.usergrid.persistence.core.migration.data.VersionedData;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ConsistencyLevel;


/**
 * Reads and writes to UniqueValues column family.
 */
public interface UniqueValueSerializationStrategy extends Migration, VersionedData {


    /**
     * Write the specified UniqueValue to Cassandra with optional timeToLive in milliseconds.
     *
     * @param applicationScope scope
     * @param uniqueValue Object to be written
     *
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    MutationBatch write( ApplicationScope applicationScope, UniqueValue uniqueValue );

    /**
     * Write the specified UniqueValue to Cassandra with optional timeToLive in milliseconds.
     *
     * @param applicationScope scope
     * @param uniqueValue Object to be written
     * @param timeToLive How long object should live in seconds.  -1 implies store forever
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    MutationBatch write( ApplicationScope applicationScope, UniqueValue uniqueValue, int timeToLive );

    /**
     * Load UniqueValue that matches field from collection or null if that value does not exist.
     *
     * @param applicationScope scope in which to look for field name/value
     * @param type The type the unique value exists within
     * @param fields Field name/value to search for
     *
     * @return UniqueValueSet containing fields from the collection that exist in cassandra
     *
     * @throws ConnectionException on error connecting to Cassandra
     */
    UniqueValueSet load( ApplicationScope applicationScope, String type, Collection<Field> fields ) throws ConnectionException;

    /**
    * Load UniqueValue that matches field from collection or null if that value does not exist.
    *
    * @param applicationScope Collection scope in which to look for field name/value
    * @param consistencyLevel Consistency level of query
    * @param type The type the unique value exists within
    * @param fields Field name/value to search for
    * @return UniqueValueSet containing fields from the collection that exist in cassandra
    * @throws ConnectionException on error connecting to Cassandra
    */
    UniqueValueSet load( ApplicationScope applicationScope, ConsistencyLevel consistencyLevel, String type,
                         Collection<Field> fields ) throws ConnectionException;


    /**
     * Loads the currently persisted history of every unique value the entity has held.  This will
     * start from the max version and return values in descending version order.  Note that for entities
     * with more than one unique field, sequential fields can be returned with the same version.
     * @param applicationScope The scope the entity is stored in
     * @param entityId
     * @return
     */
    Iterator<UniqueValue> getAllUniqueFields( ApplicationScope applicationScope, Id entityId );


    /**
     * Delete the specified Unique Value from Cassandra.
     *
     * @param applicationScope The scope of the application
     * @param uniqueValue Object to be deleted.
     * @return MutatationBatch that encapsulates operation, caller may or may not execute.
     */
    MutationBatch delete( ApplicationScope applicationScope, UniqueValue uniqueValue );


}
