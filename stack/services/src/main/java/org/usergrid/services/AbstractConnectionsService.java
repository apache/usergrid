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
package org.usergrid.services;

import static org.usergrid.services.ServiceParameter.filter;
import static org.usergrid.services.ServiceParameter.firstParameterIsName;
import static org.usergrid.utils.ClassUtils.cast;
import static org.usergrid.utils.InflectionUtils.pluralize;
import static org.usergrid.utils.ListUtils.dequeue;
import static org.usergrid.utils.ListUtils.initCopy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Results.Level;
import org.usergrid.persistence.Schema;
import org.usergrid.services.ServiceParameter.IdParameter;
import org.usergrid.services.ServiceParameter.NameParameter;
import org.usergrid.services.ServiceParameter.QueryParameter;
import org.usergrid.services.ServiceResults.Type;
import org.usergrid.services.exceptions.ServiceResourceNotFoundException;

public class AbstractConnectionsService extends AbstractService {

	private static final Logger logger = LoggerFactory
			.getLogger(AbstractConnectionsService.class);

	public AbstractConnectionsService() {
		// addSets(Arrays.asList("indexes"));
		addMetadataType("indexes");
	}

	public boolean connecting() {
		return "connecting".equals(getServiceInfo().getCollectionName());
	}

	/**
	 * Create context from parameter queue. Returns context containing a query
	 * object that represents the parameters in the queue.
	 * <p>
	 * Valid parameter patterns:
	 * <p>
	 * <cType>/ <br>
	 * <cType>/<query> <br>
	 * <cType>/indexes <br>
	 * <cType>/any <br>
	 * <cType>/<eType> <br>
	 * <cType>/<eType>/indexes <br>
	 * <cType>/<eType>/<id> <br>
	 * <cType>/<eType>/<name> <br>
	 * <cType>/<eType>/<query> <br>
	 * 
	 * 
	 */
	@Override
	public ServiceContext getContext(ServiceAction action,
			ServiceRequest request, ServiceResults previousResults,
			ServicePayload payload) throws Exception {

		EntityRef owner = request.getOwner();
		String collectionName = "application".equals(owner.getType()) ? pluralize(getServiceInfo()
				.getItemType()) : getServiceInfo().getCollectionName();
		// ServiceResults previousResults = request.getPreviousResults();

		List<ServiceParameter> parameters = initCopy(request.getParameters());

		parameters = filter(parameters, replaceParameters);

		String cType = collectionName;
		if ("connecting".equals(collectionName)
				|| "connections".equals(collectionName)
				|| "connected".equals(collectionName)) {
			cType = null;
		}
		if ((cType == null) && firstParameterIsName(parameters)) {
			cType = dequeue(parameters).getName();
		}
		if (cType != null) {
			collectionName = cType;
		}

		String eType = null;
		UUID id = null;
		String name = null;
		Query query = null;

		ServiceParameter first_parameter = dequeue(parameters);

		if (first_parameter instanceof QueryParameter) {
			query = first_parameter.getQuery();
		} else if (first_parameter instanceof IdParameter) {
			id = first_parameter.getId();
		} else if (first_parameter instanceof NameParameter) {
			String s = first_parameter.getName();
			if (hasServiceMetadata(s)) {
				return new ServiceContext(this, action, request,
						previousResults, owner, collectionName, parameters,
						payload).withServiceMetadata(s);
			} else if (hasServiceCommand(s)) {
				return new ServiceContext(this, action, request,
						previousResults, owner, collectionName, parameters,
						payload).withServiceCommand(s);
			} else if ("any".equals(s)) {
				// do nothing, placeholder
			} else {
				eType = Schema.normalizeEntityType(s);
				first_parameter = dequeue(parameters);
				if (first_parameter instanceof QueryParameter) {
					query = first_parameter.getQuery();
				} else if (first_parameter instanceof IdParameter) {
					id = first_parameter.getId();
				} else if (first_parameter instanceof NameParameter) {
					s = first_parameter.getName();
					if (hasServiceMetadata(s)) {
						return new ServiceContext(this, action, request,
								previousResults, owner, collectionName,
								parameters, payload).withServiceMetadata(s);
					} else if (hasServiceCommand(s)) {
						return new ServiceContext(this, action, request,
								previousResults, owner, collectionName,
								parameters, payload).withServiceCommand(s);
					} else {
						name = s;
					}
				}
			}
		}

		if (query == null) {
			query = new Query();
		}
		query.setConnectionType(cType);
		query.setEntityType(eType);
		if (id != null) {
			query.addIdentifier(Identifier.fromUUID(id));
		}
		if (name != null) {
			query.addIdentifier(Identifier.from(name));
		}

		return new ServiceContext(this, action, request, previousResults,
				owner, collectionName, query, parameters, payload);
	}

	@Override
	public ServiceResults getCollection(ServiceContext context)
			throws Exception {

		checkPermissionsForCollection(context);

		Results r = null;

		if (connecting()) {
			r = em.getConnectingEntities(context.getOwner().getUuid(),
					context.getCollectionName(), null,
					Results.Level.ALL_PROPERTIES);
		} else {
			r = em.getConnectedEntities(context.getOwner().getUuid(),
					context.getCollectionName(), null,
					Results.Level.ALL_PROPERTIES);
		}

		importEntities(context, r);

		return new ServiceResults(this, context, Type.CONNECTION, r, null, null);
	}

	@Override
	public ServiceResults getItemById(ServiceContext context, UUID id)
			throws Exception {

		checkPermissionsForEntity(context, id);

		EntityRef entity = null;

		if (!context.moreParameters()) {
			entity = em.get(id);

			entity = importEntity(context, (Entity) entity);
		} else {
			entity = em.getRef(id);
		}

		if (entity == null) {
			throw new ServiceResourceNotFoundException(context);
		}

		// TODO check that entity is in fact connected

		List<ServiceRequest> nextRequests = context
				.getNextServiceRequests(entity);

		return new ServiceResults(this, context, Type.CONNECTION,
				Results.fromRef(entity), null, nextRequests);
	}

	@Override
	public ServiceResults getItemsByQuery(ServiceContext context, Query query)
			throws Exception {

		checkPermissionsForCollection(context);

		if (!query.hasFilterPredicates() && (query.getEntityType() != null)
				&& query.containsNameOrEmailIdentifiersOnly()) {

			String name = query.getSingleNameOrEmailIdentifier();

			String nameProperty = Schema.getDefaultSchema().aliasProperty(
					query.getEntityType());
			if (nameProperty == null) {
				nameProperty = "name";
			}

			EntityRef ref = em.getAlias(query.getEntityType(), name);
			if (ref == null) {
				return null;
			}
			Entity entity = em.get(ref);
			if (entity == null) {
				return null;
			}
			entity = importEntity(context, entity);

			return new ServiceResults(null, context, Type.CONNECTION,
					Results.fromEntity(entity), null, null);
		}

		int count = query.getLimit();
		Results.Level level = Results.Level.REFS;
		if (!context.moreParameters()) {
			count = 1000;
			level = Level.ALL_PROPERTIES;
		}

		if (context.getRequest().isReturnsTree()) {
			level = Results.Level.ALL_PROPERTIES;
		}

		query.setLimit(count);
		query.setResultsLevel(level);

		Results r = null;

		if (connecting()) {
			if (query.hasFilterPredicates()) {
				logger.info("Attempted query of backwards connections");
				return null;
			} else {
				r = em.getConnectingEntities(context.getOwner().getUuid(),
						query.getEntityType(), query.getConnectionType(), level);
			}
		} else {
			r = em.searchConnectedEntities(context.getOwner(), query);
		}

		importEntities(context, r);

		List<ServiceRequest> nextRequests = context.getNextServiceRequests(r
				.getRefs());

		return new ServiceResults(this, context, Type.CONNECTION, r, null,
				nextRequests);
	}

	@Override
	public ServiceResults postItemById(ServiceContext context, UUID id)
			throws Exception {

		checkPermissionsForEntity(context, id);

		if (context.moreParameters()) {
			return getItemById(context, id);
		}

		Entity entity = em.get(id);
		if (entity == null) {
			return null;
		}
		entity = importEntity(context, entity);

		em.createConnection(context.getOwner(), context.getCollectionName(),
				entity);

		return new ServiceResults(null, context, Type.CONNECTION,
				Results.fromEntity(entity), null, null);
	}

	@Override
	public ServiceResults postItemByName(ServiceContext context, String name)
			throws Exception {
		return postItemsByQuery(context, context.getQuery());
	}

	@Override
	public ServiceResults postItemsByQuery(ServiceContext context, Query query)
			throws Exception {

		checkPermissionsForCollection(context);

		if (!query.hasFilterPredicates() && (query.getEntityType() != null)
				&& query.containsSingleNameOrEmailIdentifier()) {

			String name = query.getSingleNameOrEmailIdentifier();

			String nameProperty = Schema.getDefaultSchema().aliasProperty(
					query.getEntityType());
			if (nameProperty == null) {
				nameProperty = "name";
			}

			EntityRef ref = em.getAlias(query.getEntityType(), name);
			if (ref == null) {
				return null;
			}
			Entity entity = em.get(ref);
			if (entity == null) {
				return null;
			}
			entity = importEntity(context, entity);

			em.createConnection(context.getOwner(), query.getConnectionType(),
					entity);

			return new ServiceResults(null, context, Type.CONNECTION,
					Results.fromEntity(entity), null, null);

		}

		return getItemsByQuery(context, query);
	}

	@Override
	public ServiceResults deleteItemById(ServiceContext context, UUID id)
			throws Exception {

		checkPermissionsForEntity(context, id);

		if (context.moreParameters()) {
			return getItemById(context, id);
		}

		Entity entity = em.get(id);
		if (entity == null) {
			return null;
		}
		entity = importEntity(context, entity);

		em.deleteConnection(em.connectionRef(context.getOwner(),
				context.getCollectionName(), entity));

		return new ServiceResults(null, context, Type.CONNECTION,
				Results.fromEntity(entity), null, null);
	}

	@Override
	public ServiceResults deleteItemsByQuery(ServiceContext context, Query query)
			throws Exception {

		checkPermissionsForCollection(context);

		if (!query.hasFilterPredicates() && (query.getEntityType() != null)
				&& query.containsNameOrEmailIdentifiersOnly()) {

			String name = query.getSingleNameOrEmailIdentifier();

			String nameProperty = Schema.getDefaultSchema().aliasProperty(
					query.getEntityType());
			if (nameProperty == null) {
				nameProperty = "name";
			}

			EntityRef ref = em.getAlias(query.getEntityType(), name);
			if (ref == null) {
				return null;
			}
			Entity entity = em.get(ref);
			if (entity == null) {
				return null;
			}
			entity = importEntity(context, entity);

			em.deleteConnection(em.connectionRef(context.getOwner(),
					query.getConnectionType(), entity));

			return new ServiceResults(null, context, Type.CONNECTION,
					Results.fromEntity(entity), null, null);

		}

		return getItemsByQuery(context, query);
	}

	@Override
	public ServiceResults getServiceMetadata(ServiceContext context,
			String metadataType) throws Exception {
		if ("indexes".equals(metadataType)) {
			String cType = context.getQuery().getConnectionType();
			if (cType != null) {
				Set<String> indexes = cast(em.getConnectionIndexes(
						context.getOwner(), cType));

				return new ServiceResults(this, context.getRequest().withPath(
						context.getRequest().getPath() + "/indexes"),
						context.getPreviousResults(), context.getChildPath(),
						Type.GENERIC, Results.fromData(indexes), null, null);
			}
		}
		return null;
	}

}
