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
package org.usergrid.services.roles;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.usergrid.services.ServiceResults.genericServiceResults;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.SimpleRoleRef;
import org.usergrid.persistence.entities.Group;
import org.usergrid.persistence.entities.Role;
import org.usergrid.services.AbstractCollectionService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceParameter;
import org.usergrid.services.ServicePayload;
import org.usergrid.services.ServiceResults;

public class RolesService extends AbstractCollectionService {

    private static final Logger logger = LoggerFactory.getLogger(RolesService.class);

    public RolesService() {
        super();
        logger.info("/roles");

        declareEntityDictionary("permissions");

    }

    @Override
    public ServiceResults getItemByName(ServiceContext context, String name) throws Exception {
        if ((context.getOwner() != null) && Group.ENTITY_TYPE.equals(context.getOwner().getType())) {
            return getItemById(context, SimpleRoleRef.getIdForGroupIdAndRoleName(context.getOwner().getUuid(), name));
        }
        return super.getItemByName(context, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractService#getEntityDictionary(org.usergrid
     * .services.ServiceContext, java.util.List, java.lang.String)
     */
    @Override
    public ServiceResults getEntityDictionary(ServiceContext context, List<EntityRef> refs, String dictionary)
            throws Exception {

        if ("permissions".equalsIgnoreCase(dictionary)) {
            checkPermissionsForPath(context, "/permissions");

            EntityRef ref = refs.get(0);

            String roleName = (String) em.getProperty(ref, "name");
            
            if (isBlank(roleName)) {
                return null;
            }

            return getApplicationRolePermissions(roleName);

        }

        return super.getEntityDictionary(context, refs, dictionary);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractService#putEntityDictionary(org.usergrid
     * .services.ServiceContext, java.util.List, java.lang.String,
     * org.usergrid.services.ServicePayload)
     */
    @Override
    public ServiceResults putEntityDictionary(ServiceContext context, List<EntityRef> refs, String dictionary,
            ServicePayload payload) throws Exception {
        return postEntityDictionary(context, refs, dictionary, payload);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractService#postEntityDictionary(org.usergrid
     * .services.ServiceContext, java.util.List, java.lang.String,
     * org.usergrid.services.ServicePayload)
     */
    @Override
    public ServiceResults postEntityDictionary(ServiceContext context, List<EntityRef> refs, String dictionary,
            ServicePayload payload) throws Exception {

        if ("permissions".equalsIgnoreCase(dictionary)) {
            checkPermissionsForPath(context, "/permissions");

            EntityRef ref = refs.get(0);

            String roleName = (String) em.getProperty(ref, "name");

            if (isBlank(roleName)) {
                throw new IllegalArgumentException(String.format("Could not load role with id '%s'", ref.getUuid()));
            }

            String permission = payload.getStringProperty("permission");

            if (isBlank(permission)) {
                throw new IllegalArgumentException("You must supply a 'permission' property");
            }

            return grantApplicationRolePermission(roleName, permission);

        }

        return super.postEntityDictionary(context, refs, dictionary, payload);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractService#deleteEntityDictionary(org.usergrid
     * .services.ServiceContext, java.util.List, java.lang.String)
     */
    @Override
    public ServiceResults deleteEntityDictionary(ServiceContext context, List<EntityRef> refs, String dictionary)
            throws Exception {

        if ("permissions".equalsIgnoreCase(dictionary)) {
            checkPermissionsForPath(context, "/permissions");

            EntityRef ref = refs.get(0);

            String roleName = (String) em.getProperty(ref, "name");

            if (isBlank(roleName)) {
                throw new IllegalArgumentException(String.format("Could not load role with id '%s'", ref.getUuid()));
            }

            Query q = context.getParameters().get(0).getQuery();

            if (q == null) {
                throw new IllegalArgumentException("You must supply a 'permission' query parameter");
            }

            List<String> permissions = q.getPermissions();
            if (permissions == null) {
                throw new IllegalArgumentException("You must supply a 'permission' query parameter");
            }

            ServiceResults results = null;

            for (String permission : permissions) {

                results = revokeApplicationRolePermission(roleName, permission);
            }

            return results;

        }

        return super.deleteEntityDictionary(context, refs, dictionary);
    }

    public ServiceResults newApplicationRole(String roleName, String roleTitle, long inactivity) throws Exception {
        em.createRole(roleName, roleTitle, inactivity);
        return getApplicationRoles();
    }

    public ServiceResults deleteApplicationRole(String roleName) throws Exception {
        em.deleteRole(roleName);
        return getApplicationRolePermissions(roleName);
    }

    public ServiceResults getApplicationRolePermissions(String roleName) throws Exception {
        Set<String> permissions = em.getRolePermissions(roleName);
        ServiceResults results = genericServiceResults().withData(permissions);
        return results;
    }

    public ServiceResults grantApplicationRolePermission(String roleName, String permission) throws Exception {
        em.grantRolePermission(roleName, permission);
        return getApplicationRolePermissions(roleName);
    }

    public ServiceResults revokeApplicationRolePermission(String roleName, String permission) throws Exception {
        em.revokeRolePermission(roleName, permission);
        return getApplicationRolePermissions(roleName);
    }

    public ServiceResults getApplicationRoles() throws Exception {
        Map<String, String> roles = em.getRoles();
        ServiceResults results = genericServiceResults().withData(roles);
        return results;
    }

}
