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

package org.apache.usergrid.persistence.core.scope;


import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;


/**
 *
 *
 */
public class ApplicationScopeImpl implements ApplicationScope {

    protected final Id application;


    public ApplicationScopeImpl( final Id application ) {
        Preconditions.checkNotNull(application, "application id is required");
        this.application = application;
    }


    @Override
    public Id getApplication() {
        return this.application;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof ApplicationScope ) ) {
            return false;
        }

        final ApplicationScope that = ( ApplicationScope ) o;

        if ( !application.equals( that.getApplication() ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        return application.hashCode();
    }


    @Override
    public String toString() {
        return "ApplicationScopeImpl{" +
                "application=" + application +
                '}';
    }
}
