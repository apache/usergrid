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


import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Wrapper object to create collections in prefixes
 */
public class CollectionPrefixedKey<K> {

    private final String collectionName;
    private final Id owner;
    private final K subKey;


    public CollectionPrefixedKey( final String collectionName, final Id owner, final K subKey ) {
        this.collectionName = collectionName;
        this.owner = owner;
        this.subKey = subKey;
    }


    /**
     * Get the name of the
     * @return
     */
    public String getCollectionName() {
        return collectionName;
    }


    /**
     * Get the owner of the collection
     * @return
     */
    public Id getOwner() {
        return owner;
    }


    /**
     * Get the object to be used in the key
     * @return
     */
    public K getSubKey() {
        return subKey;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof CollectionPrefixedKey ) ) {
            return false;
        }

        final CollectionPrefixedKey that = ( CollectionPrefixedKey ) o;

        if ( !collectionName.equals( that.collectionName ) ) {
            return false;
        }
        if ( !subKey.equals( that.subKey ) ) {
            return false;
        }
        if ( !owner.equals( that.owner ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = collectionName.hashCode();
        result = 31 * result + owner.hashCode();
        result = 31 * result + subKey.hashCode();
        return result;
    }
}
