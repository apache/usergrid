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


import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 *
 */
public class OrganizationScopeImpl implements OrganizationScope {

    protected final Id organization;


    public OrganizationScopeImpl( final Id organization ) {
        this.organization = organization;}


    @Override
    public Id getOrganization() {
        return this.organization;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof OrganizationScopeImpl ) ) {
            return false;
        }

        final OrganizationScopeImpl that = ( OrganizationScopeImpl ) o;

        if ( !organization.equals( that.organization ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        return organization.hashCode();
    }


    @Override
    public String toString() {
        return "OrganizationScopeImpl{" +
                "organization=" + organization +
                '}';
    }
}
