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

public class CollectionSettingsScopeImpl implements CollectionSettingsScope {

    private final Id owner;
    private final String collectionName;

    public CollectionSettingsScopeImpl( final Id owner, final String collectionName ) {
        this.owner = owner;
        this.collectionName = collectionName;
    }

    @Override
    public String getCollectionName() {
        return collectionName;
    }

    @Override
    public Id getApplication() {
        return owner;
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof CollectionSettingsScopeImpl ) ) {
            return false;
        }

        final CollectionSettingsScopeImpl collectionSettingsScope = ( CollectionSettingsScopeImpl ) o;

        if ( !collectionName.equals( collectionSettingsScope.collectionName ) ) {
            return false;
        }
        if ( !owner.equals( collectionSettingsScope.owner ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(collectionName)
            .append(owner)
            .toHashCode();
    }

}
