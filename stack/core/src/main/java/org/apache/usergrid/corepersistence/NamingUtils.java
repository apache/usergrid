/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence;


import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;


/**
 * Static class to encapsulate naming conventions used through the CP entity system
 */
public class NamingUtils {


    /** The System Application where we store app and org metadata */
    public static final UUID SYSTEM_APP_ID =
            UUID.fromString("b6768a08-b5d5-11e3-a495-10ddb1de66c3");
    /** App where we store management info */
    public static final  UUID MANAGEMENT_APPLICATION_ID =
            UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c8");
    /** TODO Do we need this in two-dot-o? */
    public static final  UUID DEFAULT_APPLICATION_ID =
            UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c9");


    /**
     * Get the application scope from the given uuid
     * @param applicationId The applicationId
     */
    public static ApplicationScope getApplicationScope( UUID applicationId ) {

        // We can always generate a scope, it doesn't matter if  the application exists yet or not.
        final ApplicationScopeImpl scope = new ApplicationScopeImpl( generateApplicationId( applicationId ) );

        return scope;
    }


    /**
     * Generate an applicationId from the given UUID
     * @param applicationId  the applicationId
     *
     */
    public static Id generateApplicationId( UUID applicationId ) {
        return new SimpleId( applicationId, Application.ENTITY_TYPE );
    }
}
