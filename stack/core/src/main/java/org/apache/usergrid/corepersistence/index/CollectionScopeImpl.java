/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.index;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import java.util.UUID;

import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;


public class CollectionScopeImpl implements CollectionScope {

    protected Id application;
    protected String collectionName;


    /**
     * Do not delete!  Needed for Jackson
     */
    @SuppressWarnings( "unused" )
    public CollectionScopeImpl(){

    }

    public CollectionScopeImpl(final Id application, final String collectionName ) {
        this.application = application;
        this.collectionName = collectionName;
    }

    public CollectionScopeImpl(final UUID applicationID, final String collectionName) {
        this(new SimpleId(applicationID, TYPE_APPLICATION), collectionName);
    }

    @Override
    public String getCollectionName() {
        return collectionName;
    }

    @Override
    public Id getApplication() {
        return application;
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof CollectionScopeImpl) ) {
            return false;
        }

        final CollectionScopeImpl collectionVersionScope = (CollectionScopeImpl) o;

        if ( !application.equals( collectionVersionScope.application) ) {
            return false;
        }

        if ( !collectionName.equals( collectionVersionScope.collectionName ) ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(application)
            .append(collectionName)
            .toHashCode();
    }

}
