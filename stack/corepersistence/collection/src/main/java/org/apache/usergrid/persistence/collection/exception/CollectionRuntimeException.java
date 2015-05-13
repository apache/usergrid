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
package org.apache.usergrid.persistence.collection.exception;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;


public class CollectionRuntimeException extends RuntimeException {

    private MvccEntity entity;
    private ApplicationScope applicationScope;


    public CollectionRuntimeException( MvccEntity entity, ApplicationScope scope, final String message ) {
        super( message );
        this.entity = entity;
        this.applicationScope = scope;
    }


    public CollectionRuntimeException( MvccEntity entity, ApplicationScope scope, final String message, final Throwable cause ) {
        super( message, cause );
        this.entity = entity;
        this.applicationScope = scope;
    }


    public CollectionRuntimeException( MvccEntity entity, ApplicationScope scope, final Throwable cause ) {
        super( cause );
        this.entity = entity;
        this.applicationScope = scope;
    }


    public CollectionRuntimeException( MvccEntity entity, ApplicationScope scope,
            final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
        this.entity = entity;
        this.applicationScope = scope;
    }


    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }

    /**
     * Entity involved in operation.
     * @return Entity or null if entity not instantiated yet in operation.
     */
    public MvccEntity getEntity() {
        return entity;
    }
}
