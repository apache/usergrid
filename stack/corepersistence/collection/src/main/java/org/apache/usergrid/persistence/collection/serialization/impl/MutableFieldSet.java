package org.apache.usergrid.persistence.collection.serialization.impl;/*
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


import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.persistence.collection.FieldSet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.field.Field;


public class MutableFieldSet implements FieldSet {


    private final Map<Field<?>, MvccEntity> entities;


    public MutableFieldSet( final int expectedSize ) {
        this.entities = new HashMap<>( expectedSize );
    }


    public void addEntity(final Field<?> field,  final MvccEntity entity ) {
        entities.put( field, entity );
    }


    @Override
    public MvccEntity getEntity( final Field<?> field) {
        return entities.get( field );
    }




    @Override
    public int size() {
        return entities.size();
    }


    @Override
    public boolean isEmpty() {
        return entities.size() == 0;
    }
}
