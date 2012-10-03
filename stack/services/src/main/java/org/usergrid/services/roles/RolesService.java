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
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.SimpleRoleRef;
import org.usergrid.persistence.entities.Group;
import org.usergrid.persistence.entities.Role;
import org.usergrid.services.AbstractCollectionService;
import org.usergrid.services.ServiceContext;
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
     * org.usergrid.services.AbstractCollectionService#putItemById(org.usergrid
     * .services.ServiceContext, java.util.UUID)
     */
    @Override
    public ServiceResults putItemById(ServiceContext context, UUID id) throws Exception {
        return super.putItemById(context, id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#putItemByName(org.usergrid
     * .services.ServiceContext, java.lang.String)
     */
    @Override
    public ServiceResults putItemByName(ServiceContext context, String name) throws Exception {
        return postItemByName(context, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#putItemsByQuery(org.usergrid
     * .services.ServiceContext, org.usergrid.persistence.Query)
     */
    @Override
    public ServiceResults putItemsByQuery(ServiceContext context, Query query) throws Exception {
        return postItemsByQuery(context, query);
    }

    //
    // private ServiceResults postRole(ServiceContext context) throws Exception
    // {
    // // else if (context.parameterCount() == 1) {
    //
    // String roleName = context.getParameters().get(0).getName();
    // if (isBlank(roleName)) {
    // return null;
    // }
    //
    // String permission = context.getStringProperty("permission");
    // if (isBlank(permission)) {
    // return null;
    // }
    //
    // return grantApplicationRolePermission(roleName, permission);
    // }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#postCollection(org.usergrid
     * .services.ServiceContext)
     */
    @Override
    public ServiceResults postCollection(ServiceContext context) throws Exception {
        // invoke the default to add it to the roles collection

        String name = context.getStringProperty("name");

        // now add it to the maps of the application

        String title = context.getStringProperty("title");

        if (isBlank(title)) {
            title = name;
        }

        Long inactivity = context.getLongProperty("inactivity");

        if (inactivity == null) {
            inactivity = 0l;
        }

        context.getProperties().put("title", title);
        context.getProperties().put("inactivity", inactivity);

        ServiceResults results = super.postCollection(context);

        return newApplicationRole(name, title, inactivity, results);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#putCollection(org.usergrid
     * .services.ServiceContext)
     */
    @Override
    public ServiceResults putCollection(ServiceContext context) throws Exception {
        return postCollection(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#postItemsByQuery(org.
     * usergrid.services.ServiceContext, org.usergrid.persistence.Query)
     */
    @Override
    public ServiceResults postItemsByQuery(ServiceContext context, Query query) throws Exception {
        throw new UnsupportedOperationException("You cannot update roles by query");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#postItemById(org.usergrid
     * .services.ServiceContext, java.util.UUID)
     */
    @Override
    public ServiceResults postItemById(ServiceContext context, UUID id) throws Exception {
        ServiceResults results = super.postItemById(context, id);

        String permission = context.getStringProperty("permission");

        if (!isBlank(permission)) {
            Role role = em.get(id, Role.class);

            if (role != null) {
                grantApplicationRolePermission(role.getName(), permission, results);
            }

        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#postItemByName(org.usergrid
     * .services.ServiceContext, java.lang.String)
     */
    @Override
    public ServiceResults postItemByName(ServiceContext context, String name) throws Exception {
        ServiceResults results = super.postItemByName(context, name);

        String permission = context.getStringProperty("permission");

        if (!isBlank(permission)) {
            grantApplicationRolePermission(name, permission, results);
        }

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#deleteItemById(org.usergrid
     * .services.ServiceContext, java.util.UUID)
     */
    @Override
    public ServiceResults deleteItemById(ServiceContext context, UUID id) throws Exception {

        ServiceResults results = super.deleteItemById(context, id);

        Entity entity = results.getEntities().get(0);

        if (entity != null) {
            deleteApplicationRole(entity.getName(), results);
        }

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#deleteItemByName(org.
     * usergrid.services.ServiceContext, java.lang.String)
     */
    @Override
    public ServiceResults deleteItemByName(ServiceContext context, String name) throws Exception {
        ServiceResults results = super.deleteItemByName(context, name);
        return deleteApplicationRole(name, results);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.services.AbstractCollectionService#deleteItemsByQuery(org
     * .usergrid.services.ServiceContext, org.usergrid.persistence.Query)
     */
    @Override
    public ServiceResults deleteItemsByQuery(ServiceContext context, Query query) throws Exception {
        throw new UnsupportedOperationException("You cannot delete roles by query");
    }

    public ServiceResults newApplicationRole(String roleName, String roleTitle, long inactivity, ServiceResults results)
            throws Exception {
        em.createRole(roleName, roleTitle, inactivity);
        return getApplicationRoles(results);
    }

    public ServiceResults deleteApplicationRole(String roleName, ServiceResults results) throws Exception {
        em.deleteRole(roleName);
        return getApplicationRoles(results);
    }

    public ServiceResults grantApplicationRolePermission(String roleName, String permission, ServiceResults results)
            throws Exception {
        em.grantRolePermission(roleName, permission);
        return getApplicationRolePermissions(roleName, results);
    }

    public ServiceResults revokeApplicationRolePermission(String roleName, String permission, ServiceResults results)
            throws Exception {
        em.revokeRolePermission(roleName, permission);
        return getApplicationRolePermissions(roleName, results);
    }

    public ServiceResults getApplicationRolePermissions(String roleName, ServiceResults results) throws Exception {
        Set<String> permissions = em.getRolePermissions(roleName);
        results.withData(permissions);
        return results;
    }

    public ServiceResults getApplicationRoles(ServiceResults results) throws Exception {
        Map<String, String> roles = em.getRoles();
        results.withData(roles);
        return results;
    }

}
