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
package org.usergrid.services.users;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.persistence.Schema.PROPERTY_EMAIL;
import static org.usergrid.persistence.Schema.PROPERTY_PICTURE;
import static org.usergrid.services.ServiceResults.genericServiceResults;
import static org.usergrid.utils.ConversionUtils.string;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.entities.Role;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.services.AbstractCollectionService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServicePayload;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;
import org.usergrid.services.exceptions.ServiceResourceNotFoundException;

public class UsersService extends AbstractCollectionService {

    private static final Logger logger = LoggerFactory
            .getLogger(UsersService.class);

    public UsersService() {
        super();
        logger.info("/users");

        makeConnectionPrivate("following");

        declareVirtualCollections(Arrays.asList("following", "followers"));

        addReplaceParameters(Arrays.asList("$id", "followers"),
                Arrays.asList("\\0", "connecting", "following"));

        declareEntityDictionaries(Arrays.asList("rolenames", "permissions"));

    }

  @Override
  public ServiceResults getItemByName(ServiceContext context, String name) throws Exception {
    String nameProperty = Schema.getDefaultSchema().aliasProperty(
  				getEntityType());
  		if (nameProperty == null) {
  			nameProperty = "name";
  		}
      EntityRef entity = null;
      Identifier id = Identifier.from(name);
      if ( id != null ) {
        entity = em.getUserByIdentifier(id);
      }

  		if (entity == null) {
        logger.info("miss on entityType: {} with name: {}", getEntityType(), name);
  			throw new ServiceResourceNotFoundException(context);
  		}

  		if (!context.moreParameters()) {
  			entity = em.get(entity);
  			entity = importEntity(context, (Entity) entity);
  		}

  		checkPermissionsForEntity(context, entity);

  		List<ServiceRequest> nextRequests = context
  				.getNextServiceRequests(entity);

  		return new ServiceResults(this, context, ServiceResults.Type.COLLECTION,
  				Results.fromRef(entity), null, nextRequests);
  }

  @Override
    public ServiceResults invokeItemWithName(ServiceContext context, String name)
            throws Exception {
        if ("me".equals(name)) {
            UserInfo user = SubjectUtils.getUser();
            if ((user != null) && (user.getUuid() != null)) {
                return super.invokeItemWithId(context, user.getUuid());
            }
        }
        return super.invokeItemWithName(context, name);
    }

    @Override
    public ServiceResults postCollection(ServiceContext context)
            throws Exception {
        Iterator<Map<String, Object>> i = context.getPayload()
                .payloadIterator();
        while (i.hasNext()) {
            Map<String, Object> p = i.next();
            setGravatar(p);
        }
        return super.postCollection(context);
    }

    public void setGravatar(Map<String, Object> p) {
        if (isBlank(string(p.get(PROPERTY_PICTURE)))
                && isNotBlank(string(p.get("email")))) {
            p.put(PROPERTY_PICTURE,
                    "http://www.gravatar.com/avatar/"
                            + md5Hex(string(p.get(PROPERTY_EMAIL)).trim()
                                    .toLowerCase()));
        }
    }

    public ServiceResults getUserRoles(UUID userId) throws Exception {
        Map<String, Role> roles = em.getUserRolesWithTitles(userId);
        // roles.put("default", "Default");
        ServiceResults results = genericServiceResults().withData(roles);
        return results;
    }

    public ServiceResults getApplicationRolePermissions(String roleName)
            throws Exception {
        Set<String> permissions = em.getRolePermissions(roleName);
        ServiceResults results = genericServiceResults().withData(permissions);
        return results;
    }

    public ServiceResults addUserRole(UUID userId, String roleName)
            throws Exception {
        em.addUserToRole(userId, roleName);
        return getUserRoles(userId);
    }

    public ServiceResults deleteUserRole(UUID userId, String roleName)
            throws Exception {
        em.removeUserFromRole(userId, roleName);
        return getUserRoles(userId);
    }

    @Override
    public ServiceResults getEntityDictionary(ServiceContext context,
            List<EntityRef> refs, String dictionary) throws Exception {

        if ("rolenames".equalsIgnoreCase(dictionary)) {
            EntityRef entityRef = refs.get(0);
            checkPermissionsForEntitySubPath(context, entityRef, "rolenames");

            if (context.parameterCount() == 0) {

                return getUserRoles(entityRef.getUuid());

            } else if (context.parameterCount() == 1) {

                String roleName = context.getParameters().get(1).getName();
                if (isBlank(roleName)) {
                    return null;
                }

                return getApplicationRolePermissions(roleName);
            }

        } else if ("permissions".equalsIgnoreCase(dictionary)) {
            EntityRef entityRef = refs.get(0);
            checkPermissionsForEntitySubPath(context, entityRef, "permissions");

            return genericServiceResults().withData(
                    em.getUserPermissions(entityRef.getUuid()));

        }

        return super.getEntityDictionary(context, refs, dictionary);
    }

    @Override
    public ServiceResults postEntityDictionary(ServiceContext context,
            List<EntityRef> refs, String dictionary, ServicePayload payload)
            throws Exception {

        if ("permissions".equalsIgnoreCase(dictionary)) {
            EntityRef entityRef = refs.get(0);
            checkPermissionsForEntitySubPath(context, entityRef, "permissions");

            String permission = payload.getStringProperty("permission");
            if (isBlank(permission)) {
                return null;
            }

            em.grantUserPermission(entityRef.getUuid(), permission);

            return genericServiceResults().withData(
                    em.getUserPermissions(entityRef.getUuid()));

        }

        return super.postEntityDictionary(context, refs, dictionary, payload);
    }

    @Override
    public ServiceResults putEntityDictionary(ServiceContext context,
            List<EntityRef> refs, String dictionary, ServicePayload payload)
            throws Exception {

        if ("rolenames".equalsIgnoreCase(dictionary)) {
            EntityRef entityRef = refs.get(0);
            checkPermissionsForEntitySubPath(context, entityRef, "rolenames");

            if (context.parameterCount() == 0) {

                String name = payload.getStringProperty("name");
                if (isBlank(name)) {
                    return null;
                }

                return addUserRole(entityRef.getUuid(), name);

            }

        }

        return super.postEntityDictionary(context, refs, dictionary, payload);
    }

    @Override
    public ServiceResults deleteEntityDictionary(ServiceContext context,
            List<EntityRef> refs, String dictionary) throws Exception {

        if ("rolenames".equalsIgnoreCase(dictionary)) {
            EntityRef entityRef = refs.get(0);
            checkPermissionsForEntitySubPath(context, entityRef, "rolenames");

            if (context.parameterCount() == 1) {

                String roleName = context.getParameters().get(1).getName();
                if (isBlank(roleName)) {
                    return null;
                }

                return deleteUserRole(entityRef.getUuid(), roleName);

            }
        } else if ("permissions".equalsIgnoreCase(dictionary)) {
            EntityRef entityRef = refs.get(0);
            checkPermissionsForEntitySubPath(context, entityRef, "permissions");

            Query q = context.getParameters().get(0).getQuery();
            if (q == null) {
                return null;
            }

            List<String> permissions = q.getPermissions();
            if (permissions == null) {
                return null;
            }

            for (String permission : permissions) {
                em.revokeUserPermission(entityRef.getUuid(), permission);
            }

            return genericServiceResults().withData(
                    em.getUserPermissions(entityRef.getUuid()));

        }

        return super.deleteEntityDictionary(context, refs, dictionary);
    }

}
