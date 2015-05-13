/*
 *
*
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
*
 *
 */

package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.model.field.Field;


/**
 * Class that encapsulates a unique field with the entity and version that own it
 */
public class UniqueFieldEntry {

    private final UUID version;
    private final Field field;


    /**
     * Create a unique field with the specified version and field
     * @param version
     * @param field
     */
    public UniqueFieldEntry( final UUID version, final Field field ) {
        this.version = version;
        this.field = field;
    }


    public UUID getVersion() {
        return version;
    }


    public Field getField() {
        return field;
    }
}
