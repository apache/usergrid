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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;


public class UniqueValueSetImpl implements UniqueValueSet {

    private final Map<String, UniqueValue> values;

    public UniqueValueSetImpl(final int expectedMaxSize) {
        values = new HashMap<>(expectedMaxSize);
    }


    public void addValue(UniqueValue value){
        values.put( value.getField().getName(), value );
    }

    @Override
    public UniqueValue getValue( final String fieldName ) {
        return values.get( fieldName );
    }


    @Override
    public Iterator<UniqueValue> iterator() {
        return new UniqueValueIterator(values.entrySet());
    }


    /**
     * Inner class of unique value iterator
     */
    private static final class
            UniqueValueIterator implements Iterator<UniqueValue>{

        private final Iterator<Map.Entry<String, UniqueValue>> sourceIterator;

        public UniqueValueIterator( final Set<Map.Entry<String, UniqueValue>> entries ) {
            this.sourceIterator = entries.iterator();
        }


        @Override
        public boolean hasNext() {
            return sourceIterator.hasNext();
        }


        @Override
        public UniqueValue next() {
            return sourceIterator.next().getValue();
        }


        @Override
        public void remove() {
            throw new UnsupportedOperationException( "Remove is unsupported" );
        }
    }
}
