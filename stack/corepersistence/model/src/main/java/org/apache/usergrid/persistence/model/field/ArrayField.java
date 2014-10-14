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

import java.util.List;

/**
 * A marker to signal array handling. Just delegates to list field for easier handling internally
 */
public class ArrayField<T> extends ListField<T> {

    /**
     * Contructor that intializes with an empty set for adding to later
     */
    public ArrayField( String name ) {
        super( name );
    }

    public ArrayField( String name, List<T> list ) {
        super( name, list );
    }

    public ArrayField() {
        super();
    }

    /**
     * Add the value to the list
     */
    public void add( T listItem ) {
        value.add( listItem );
    }



}
