/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.map.impl;


import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * The scope impl
 */
public class MapScopeImpl implements MapScope {

    private final Id owner;
    private final String name;


    public MapScopeImpl( final Id owner, final String name ) {
        this.owner = owner;
        this.name = name;
    }


    @Override
    public Id getApplication() {
        return owner;
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
        if ( !( o instanceof MapScopeImpl ) ) {
            return false;
        }

        final MapScopeImpl mapScope = ( MapScopeImpl ) o;

        if ( !name.equals( mapScope.name ) ) {
            return false;
        }
        if ( !owner.equals( mapScope.owner ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = owner.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "MapScopeImpl{" +
                "owner=" + owner +
                ", name='" + name + '\'' +
                '}';
    }

}
