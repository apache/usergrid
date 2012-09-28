/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.services.groups.rolenamess;


import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.services.ServicePayload;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.groups.roles.RolesService;

/**
 * Placeholder endpoint to delegate all "**\\/rolenames" to the roles service so data is inserted into the roles collection and the rolenames map 
 * @author tnine
 *
 */
public class RolenamesService extends RolesService {

    /**
     * 
     */
    public RolenamesService() {
        super();
    }

    public Entity updateEntity(ServiceRequest request, EntityRef ref,
            ServicePayload payload) throws Exception {
        return super.updateEntity(request, ref, payload);
    }

}
