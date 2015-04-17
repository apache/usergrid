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
package org.apache.usergrid.persistence.model.field;

import java.util.ArrayList;
import java.util.List;

/**
 * An object field that represents a list of objects. This can also be used to represent arrays
 * @param <T> Type of entity in list, must be primitive or Entity.
 */
public abstract class ListField<T> extends AbstractField<List<T>> {

    /**
     * Constructor that initializes with an empty set for adding to later
     */
    public ListField( String name ) {
        super( name, new ArrayList<T>() );
    }

    public ListField( String name, List list ) {
        super( name, list );
    }

    public ListField() {
        super();
    }

    /**
     * Add the value to the list
     */
    public void add( T listItem ) {
        value.add( listItem );
    }


    @Override
    public FieldTypeName getTypeName() {
        return FieldTypeName.LIST;

    }
}
