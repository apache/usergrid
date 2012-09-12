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
package org.usergrid.services.groups;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.entities.Role;
import org.usergrid.services.AbstractPathBasedColllectionService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServicePayload;
import org.usergrid.services.ServiceResults;

import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.usergrid.services.ServiceResults.genericServiceResults;

public class GroupsService extends AbstractPathBasedColllectionService {

	private static final Logger logger = LoggerFactory.getLogger(GroupsService.class);

  static CharMatcher matcher = CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.anyOf("-./"));

	public GroupsService() {
		super();
		logger.info("/groups");

    declareEntityDictionaries(Arrays.asList("rolenames", "permissions"));
  }

	@Override
	public ServiceResults postCollection(ServiceContext context)
			throws Exception {

    String path = (String)context.getProperty("path");

    logger.info("Creating group with path {}", path);

    Preconditions.checkArgument(matcher.matchesAllOf(path),
            "Illegal characters found in group name: " + path);

		return super.postCollection(context);
	}

  public ServiceResults getGroupRoles(UUID groupId) throws Exception {
    Map<String, Role> roles = em.getGroupRolesWithTitles(groupId);
    ServiceResults results = genericServiceResults().withData(roles);
    return results;
  }

  public ServiceResults getApplicationRolePermissions(String roleName) throws Exception {
    Set<String> permissions = em.getRolePermissions(roleName);
    ServiceResults results = genericServiceResults().withData(permissions);
    return results;
  }

  public ServiceResults addGroupRole(UUID groupId, String roleName) throws Exception {
    em.addGroupToRole(groupId, roleName);
    return getGroupRoles(groupId);
  }

  public ServiceResults deleteGroupRole(UUID groupId, String roleName) throws Exception {
    em.removeGroupFromRole(groupId, roleName);
    return getGroupRoles(groupId);
  }

  @Override
  public ServiceResults getEntityDictionary(ServiceContext context,
                                            List<EntityRef> refs,
                                            String dictionary) throws Exception {

    if ("rolenames".equalsIgnoreCase(dictionary)) {
      EntityRef entityRef = refs.get(0);
      checkPermissionsForEntitySubPath(context, entityRef, "rolenames");

      if (context.parameterCount() == 0) {

        return getGroupRoles(entityRef.getUuid());

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

      return genericServiceResults().withData(em.getGroupPermissions(entityRef.getUuid()));
    }

    return super.getEntityDictionary(context, refs, dictionary);
  }

  @Override
  public ServiceResults postEntityDictionary(ServiceContext context,
                                             List<EntityRef> refs,
                                             String dictionary,
                                             ServicePayload payload) throws Exception {

    if ("permissions".equalsIgnoreCase(dictionary)) {
      EntityRef entityRef = refs.get(0);
      checkPermissionsForEntitySubPath(context, entityRef, "permissions");

      String permission = payload.getStringProperty("permission");
      if (isBlank(permission)) {
        return null;
      }

      em.grantGroupPermission(entityRef.getUuid(), permission);

      return genericServiceResults().withData(em.getGroupPermissions(entityRef.getUuid()));

    }

    return super.postEntityDictionary(context, refs, dictionary, payload);
  }

  @Override
  public ServiceResults putEntityDictionary(ServiceContext context,
                                            List<EntityRef> refs,
                                            String dictionary,
                                            ServicePayload payload) throws Exception {

    if ("rolenames".equalsIgnoreCase(dictionary)) {
      EntityRef entityRef = refs.get(0);
      checkPermissionsForEntitySubPath(context, entityRef, "rolenames");

      if (context.parameterCount() == 0) {

        String name = payload.getStringProperty("name");
        if (isBlank(name)) {
          return null;
        }

        return addGroupRole(entityRef.getUuid(), name);

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

        return deleteGroupRole(entityRef.getUuid(), roleName);

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
        em.revokeGroupPermission(entityRef.getUuid(), permission);
      }

      return genericServiceResults().withData(em.getGroupPermissions(entityRef.getUuid()));
    }

    return super.deleteEntityDictionary(context, refs, dictionary);
  }

}
