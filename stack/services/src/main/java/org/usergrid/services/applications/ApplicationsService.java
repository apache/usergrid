/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
package org.usergrid.services.applications;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.usergrid.services.ServiceResults.genericServiceResults;
import static org.usergrid.services.ServiceResults.simpleServiceResults;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.services.AbstractService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceParameter.QueryParameter;
import org.usergrid.services.ServicePayload;
import org.usergrid.services.ServiceResults;
import org.usergrid.services.ServiceResults.Type;

public class ApplicationsService extends AbstractService {

	private static final Logger logger = LoggerFactory
			.getLogger(ApplicationsService.class);

	public ApplicationsService() {
		super();
		logger.info("/applications");
		addEntityDictionary("rolenames");
		addEntityDictionary("counters");
		addEntityCommand("hello");
		addEntityCommand("resetroles");
	}

	@Override
	public ServiceResults invoke(ServiceContext context) throws Exception {

		ServiceResults results = null;

		String metadataType = checkForServiceMetadata(context);
		if (metadataType != null) {
			return handleServiceMetadata(context, metadataType);
		}

		String entityDictionary = checkForEntityDictionaries(context);
		String entityCommand = checkForEntityCommands(context);

		results = invokeItemWithId(context, sm.getApplicationId());
		context.dequeueParameter();

		results = handleEntityDictionary(context, results, entityDictionary);
		results = handleEntityCommand(context, results, entityCommand);

		return results;
	}

	@Override
	public ServiceResults getItemById(ServiceContext context, UUID id)
			throws Exception {
		return getApplicationEntity(context);
	}

	@Override
	public ServiceResults putItemById(ServiceContext context, UUID id)
			throws Exception {
		return updateApplicationEntity(context, context.getPayload());
	}

	@Override
	public ServiceResults getEntityDictionary(ServiceContext context,
			List<EntityRef> refs, String dictionary) throws Exception {

		if ("rolenames".equalsIgnoreCase(dictionary)) {
			checkPermissionsForPath(context, "/rolenames");

			if (context.parameterCount() == 0) {

				return getApplicationRoles();

			} else if (context.parameterCount() == 1) {

				String roleName = context.getParameters().get(0).getName();
				if (isBlank(roleName)) {
					return null;
				}

				return getApplicationRolePermissions(roleName);
			}

		} else if ("counters".equals(dictionary)) {
			checkPermissionsForPath(context, "/counters");

			if (context.parameterCount() == 0) {
				return getApplicationCounterNames();
			} else if (context.parameterCount() > 0) {
				if (context.getParameters().get(0) instanceof QueryParameter) {
					return getApplicationCounters(((QueryParameter) context
							.getParameters().get(0)).getQuery());
				}
			}
		}

		return super.getEntityDictionary(context, refs, dictionary);
	}

	@Override
	public ServiceResults postEntityDictionary(ServiceContext context,
			List<EntityRef> refs, String dictionary, ServicePayload payload)
			throws Exception {

		if ("rolenames".equalsIgnoreCase(dictionary)) {
			checkPermissionsForPath(context, "/rolenames");

			if (context.parameterCount() == 0) {

				String name = payload.getStringProperty("name");
				if (isBlank(name)) {
					return null;
				}

				String title = payload.getStringProperty("title");
				if (isBlank(title)) {
					title = name;
				}

				return newApplicationRole(name, title);

			} else if (context.parameterCount() == 1) {

				String roleName = context.getParameters().get(0).getName();
				if (isBlank(roleName)) {
					return null;
				}

				String permission = payload.getStringProperty("permission");
				if (isBlank(permission)) {
					return null;
				}

				return grantApplicationRolePermission(roleName, permission);
			}

		}

		return super.postEntityDictionary(context, refs, dictionary, payload);
	}

	@Override
	public ServiceResults deleteEntityDictionary(ServiceContext context,
			List<EntityRef> refs, String dictionary) throws Exception {

		if ("rolenames".equalsIgnoreCase(dictionary)) {
			checkPermissionsForPath(context, "/rolenames");

			if (context.parameterCount() == 1) {

				String roleName = context.getParameters().get(0).getName();
				if (isBlank(roleName)) {
					return null;
				}

				return deleteApplicationRole(roleName);

			} else if (context.parameterCount() > 1) {

				String roleName = context.getParameters().get(0).getName();
				if (isBlank(roleName)) {
					return null;
				}

				Query q = context.getParameters().get(1).getQuery();
				if (q == null) {
					return null;
				}

				List<String> permissions = q.getPermissions();
				if (permissions == null) {
					return null;
				}

				for (String permission : permissions) {
					revokeApplicationRolePermission(roleName, permission);
				}

				return getApplicationRolePermissions(roleName);

			}
		}

		return super.deleteEntityDictionary(context, refs, dictionary);
	}

	public ServiceResults getApplicationEntity(ServiceContext context)
			throws Exception {

		checkPermissionsForPath(context, "/");

		Entity entity = em.get(em.getApplicationRef());
		Results r = Results.fromEntity(entity);

		Map<String, Object> collections = em.getApplicationCollectionMetadata();
		// Set<String> collections = em.getApplicationCollections();
		if (collections.size() > 0) {
			r.setMetadata(em.getApplicationRef().getUuid(), "collections",
					collections);
		}

		return genericServiceResults(r);
	}

	public ServiceResults updateApplicationEntity(ServiceContext context,
			ServicePayload payload) throws Exception {

		checkPermissionsForPath(context, "/");

		Map<String, Object> properties = payload.getProperties();
		Object m = properties.get("metadata");
		if (m instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> metadata = (Map<String, Object>) m;
			Object c = metadata.get("collections");
			if (c instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> collections = (Map<String, Object>) c;
				for (String collection : collections.keySet()) {
					em.createApplicationCollection(collection);
					logger.info("Created collection " + collection
							+ " for application " + sm.getApplicationId());
				}
			}
		}

		Entity entity = em.get(em.getApplicationRef());
		em.updateProperties(entity, properties);
		entity.addProperties(properties);
		Results r = Results.fromEntity(entity);

		Set<String> collections = em.getApplicationCollections();
		if (collections.size() > 0) {
			r.setMetadata(em.getApplicationRef().getUuid(), "collections",
					collections);
		}

		return genericServiceResults(r);
	}

	public ServiceResults getApplicationRoles() throws Exception {
		Map<String, String> roles = em.getRoles();
		ServiceResults results = genericServiceResults().withData(roles);
		return results;
	}

	public ServiceResults newApplicationRole(String roleName, String roleTitle)
			throws Exception {
		em.createRole(roleName, roleTitle);
		return getApplicationRoles();
	}

	public ServiceResults deleteApplicationRole(String roleName)
			throws Exception {
		em.deleteRole(roleName);
		return getApplicationRoles();
	}

	public ServiceResults getApplicationRolePermissions(String roleName)
			throws Exception {
		Set<String> permissions = em.getRolePermissions(roleName);
		ServiceResults results = genericServiceResults().withData(permissions);
		return results;
	}

	public ServiceResults grantApplicationRolePermission(String roleName,
			String permission) throws Exception {
		em.grantRolePermission(roleName, permission);
		return getApplicationRolePermissions(roleName);
	}

	public ServiceResults revokeApplicationRolePermission(String roleName,
			String permission) throws Exception {
		em.revokeRolePermission(roleName, permission);
		return getApplicationRolePermissions(roleName);
	}

	public ServiceResults getApplicationCounterNames() throws Exception {
		Set<String> counters = em.getCounterNames();
		ServiceResults results = genericServiceResults().withData(counters);
		return results;
	}

	public ServiceResults getApplicationCounters(Query query) throws Exception {
		Results counters = em.getAggregateCounters(query);
		ServiceResults results = simpleServiceResults(Type.COUNTERS);
		if (counters != null) {
			results.withCounters(counters.getCounters());
		}
		return results;
	}

	@Override
	public ServiceResults getEntityCommand(ServiceContext context,
			List<EntityRef> refs, String command) throws Exception {
		if ("hello".equalsIgnoreCase(command)) {
			ServiceResults results = genericServiceResults().withData(
					hashMap("say", "Hello!"));
			return results;
		}
		return super.getEntityCommand(context, refs, command);
	}

	@Override
	public ServiceResults postEntityCommand(ServiceContext context,
			List<EntityRef> refs, String command, ServicePayload payload)
			throws Exception {
		if ("resetroles".equalsIgnoreCase(command)) {
			em.resetRoles();
			return getApplicationRoles();
		}
		return super.postEntityCommand(context, refs, command, payload);
	}

}
