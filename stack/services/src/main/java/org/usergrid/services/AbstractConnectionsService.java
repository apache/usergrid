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
import org.usergrid.persistence.ConnectionRef;
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
		declareMetadataType("indexes");
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

		if (!query.hasQueryPredicates() && (query.getEntityType() != null)
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
			count = Query.MAX_LIMIT;
			level = Level.ALL_PROPERTIES;
		}

		if (context.getRequest().isReturnsTree()) {
			level = Results.Level.ALL_PROPERTIES;
		}

		query.setLimit(count);
		query.setResultsLevel(level);

		Results r = null;

		if (connecting()) {
			if (query.hasQueryPredicates()) {
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
			throw new ServiceResourceNotFoundException(context);
		}
		entity = importEntity(context, entity);

		createConnection(context.getOwner(), context.getCollectionName(),
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
    if (!query.hasQueryPredicates() && (query.getEntityType() != null)) {

      Entity entity;
      if (query.containsSingleNameOrEmailIdentifier()) {
        String name = query.getSingleNameOrEmailIdentifier();

        EntityRef ref = em.getAlias(query.getEntityType(), name);
        if (ref == null) {
          throw new ServiceResourceNotFoundException(context);
        }
        entity = em.get(ref);
        if (entity == null) {
          throw new ServiceResourceNotFoundException(context);
        }
      } else {
        entity = em.create(query.getEntityType(), context.getProperties());
      }
			entity = importEntity(context, entity);

			createConnection(context.getOwner(), query.getConnectionType(),
					entity);

			return new ServiceResults(null, context, Type.CONNECTION,
					Results.fromEntity(entity), null, null);

		}

		return getItemsByQuery(context, query);
	}

	@Override
	public ServiceResults putItemById(ServiceContext context, UUID id)
			throws Exception {

		if (context.moreParameters()) {
			return getItemById(context, id);
		}

		checkPermissionsForEntity(context, id);

		Entity item = em.get(id);
		if (item != null) {
			updateEntity(context, item, context.getPayload());
			item = importEntity(context, item);
		} else {
			String entityType = getEntityType();
			item = em.create(id, entityType, context.getPayload()
					.getProperties());
		}
		return new ServiceResults(this, context, Type.CONNECTION,
				Results.fromEntity(item), null, null);
	}

	@Override
	public ServiceResults putItemByName(ServiceContext context, String name)
			throws Exception {

		return putItemsByQuery(context, context.getQuery());
	}

	@Override
	public ServiceResults putItemsByQuery(ServiceContext context, Query query)
			throws Exception {

		checkPermissionsForCollection(context);

		if (context.moreParameters()) {
			return getItemsByQuery(context, query);
		}

		
		Results r = em.searchConnectedEntities(context.getOwner(), query);
		if (r.isEmpty()) {
			throw new ServiceResourceNotFoundException(context);
		}

		updateEntities(context, r);

		return new ServiceResults(this, context, Type.CONNECTION, r, null, null);
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
			throw new ServiceResourceNotFoundException(context);
		}
		entity = importEntity(context, entity);

		deleteConnection(em.connectionRef(context.getOwner(),
				context.getCollectionName(), entity));

		return new ServiceResults(null, context, Type.CONNECTION,
				Results.fromEntity(entity), null, null);
	}

	@Override
	public ServiceResults deleteItemByName(ServiceContext context, String name)
			throws Exception {
		return deleteItemsByQuery(context, context.getQuery());
	}

	@Override
	public ServiceResults deleteItemsByQuery(ServiceContext context, Query query)
			throws Exception {

		checkPermissionsForCollection(context);

		if (!query.hasQueryPredicates() && (query.getEntityType() != null)
				&& query.containsNameOrEmailIdentifiersOnly()) {

			String name = query.getSingleNameOrEmailIdentifier();

			String nameProperty = Schema.getDefaultSchema().aliasProperty(
					query.getEntityType());
			if (nameProperty == null) {
				nameProperty = "name";
			}

			EntityRef ref = em.getAlias(query.getEntityType(), name);
			if (ref == null) {
				throw new ServiceResourceNotFoundException(context);
			}
			Entity entity = em.get(ref);
			if (entity == null) {
				throw new ServiceResourceNotFoundException(context);
			}
			entity = importEntity(context, entity);

			deleteConnection(em.connectionRef(context.getOwner(),
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

	public ConnectionRef createConnection(EntityRef connectingEntity,
			String connectionType, EntityRef connectedEntityRef)
			throws Exception {
		return em.createConnection(connectingEntity, connectionType,
				connectedEntityRef);
	}

	public void deleteConnection(ConnectionRef connectionRef) throws Exception {
		em.deleteConnection(connectionRef);
	}

}
