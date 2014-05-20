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
package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Simple impl of hte collection context
 *
 * @author tnine
 */
public class CollectionScopeImpl extends ApplicationScopeImpl implements CollectionScope {
    private final Id ownerId;
    private final String name;


    public CollectionScopeImpl( final Id applicationId, final Id ownerId, final String name ) {
        super( applicationId );
        this.ownerId = ownerId;
        this.name = name;

        MvccValidationUtils.validateCollectionScope( this );
    }


    @Override
    public Id getOwner() {
        return ownerId;
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof CollectionScopeImpl ) ) {
            return false;
        }
        if ( !super.equals( o ) ) {
            return false;
        }

        final CollectionScopeImpl that = ( CollectionScopeImpl ) o;

        if ( !name.equals( that.name ) ) {
            return false;
        }
        if ( !ownerId.equals( that.ownerId ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + ownerId.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "CollectionScopeImpl{" +
                "ownerId=" + ownerId +
                ", name='" + name + '\'' +
                '}';
    }
}
