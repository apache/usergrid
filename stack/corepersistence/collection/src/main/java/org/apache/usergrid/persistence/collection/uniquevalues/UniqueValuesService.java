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
package org.apache.usergrid.persistence.collection.uniquevalues;


import org.apache.usergrid.persistence.actorsystem.RouterProducer;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import java.util.UUID;


/**
 * Service that reserves and confirms unique values.
 */
public interface UniqueValuesService extends RouterProducer {

    /**
     * Check that unique values are unique and reserve them for a limited time.
     * If the reservations are not confirmed, they will expire.
     *
     * @param scope Application scope of entity.
     * @param entity Entity with unique values to be confirmed.
     * @param version Version of entity claiming unique values.
     * @param region Authoritative Region to be used for this entity or null to use current region.
     * @throws UniqueValueException if unique values cannot be confirmed.
     */
    void reserveUniqueValues( ApplicationScope scope, Entity entity, UUID version, String region )
        throws UniqueValueException;

    /**
     * Confirm unique values that were reserved earlier.
     *
     * @param scope Application scope of entity.
     * @param entity Entity with unique values to be reserved.
     * @param version Version of entity claiming unique values.
     * @param region Authoritative Region to be used for this entity or null to use current region.
     * @throws UniqueValueException if unique values cannot be reserved.
     */
    void confirmUniqueValues( ApplicationScope scope, Entity entity, UUID version , String region )
        throws UniqueValueException;

    // TODO: is this really necessary? MarkCommit and UniqueCleanup should do the trick
    /**
     * Release unique values held by an entity.
     * @param scope Application scope of entity.
     * @param entityId Id of Entity with unique values to be released
     * @param version Version of entity.
     * @param region Authoritative Region to be used for this entity or null to use current region.
     * @throws UniqueValueException if unique values cannot be reserved.
     */
//    void releaseUniqueValues( ApplicationScope scope, Id entityId, UUID version, String region )
//        throws UniqueValueException;

}
