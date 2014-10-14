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
package org.apache.usergrid.persistence.index.impl;

import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.utils.IndexValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;


public class IndexScopeImpl implements IndexScope {
    private final Id ownerId;
    private final String type;


    public IndexScopeImpl( final Id ownerId, final String type ) {
        this.ownerId = ownerId;
        this.type = type;

        IndexValidationUtils.validateIndexScope( this );
    }


    @Override
    public String getName() {
        return  type;
    }


    @Override
    public Id getOwner() {
        return ownerId;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof IndexScopeImpl ) ) {
            return false;
        }

        final IndexScopeImpl that = ( IndexScopeImpl ) o;

        if ( !ownerId.equals( that.ownerId ) ) {
            return false;
        }
        if ( !type.equals( that.type ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = ownerId.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "IndexScopeImpl{" +
                "ownerId=" + ownerId +
                ", type='" + type + '\'' +
                '}';
    }
}
