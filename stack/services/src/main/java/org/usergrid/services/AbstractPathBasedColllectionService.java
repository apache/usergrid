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
import static org.usergrid.services.ServiceParameter.mergeQueries;
import static org.usergrid.utils.InflectionUtils.pluralize;
import static org.usergrid.utils.ListUtils.dequeueCopy;
import static org.usergrid.utils.ListUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Schema;
import org.usergrid.services.ServiceParameter.IdParameter;
import org.usergrid.services.ServiceParameter.NameParameter;
import org.usergrid.services.ServiceParameter.QueryParameter;
import org.usergrid.services.exceptions.ServiceInvocationException;

public class AbstractPathBasedColllectionService extends
		AbstractCollectionService {

	private static final Logger logger = LoggerFactory
			.getLogger(AbstractPathBasedColllectionService.class);

	public AbstractPathBasedColllectionService() {
		super();
	}

	@Override
	public ServiceContext getContext(ServiceAction action,
			ServiceRequest request, ServiceResults previousResults,
			ServicePayload payload) throws Exception {

		EntityRef owner = request.getOwner();
		String collectionName = "application".equals(owner.getType()) ? pluralize(getServiceInfo()
				.getItemType()) : getServiceInfo().getCollectionName();

		EntityRef pathEntity = null;
		List<ServiceParameter> parameters = filter(request.getParameters(),
				replaceParameters);
		ServiceParameter first_parameter = null;
		if (!isEmpty(parameters)) {
			first_parameter = parameters.get(0);

			if (first_parameter instanceof NameParameter) {

				if (hasServiceMetadata(first_parameter.getName())) {
					return new ServiceContext(this, action, request,
							previousResults, owner, collectionName, parameters,
							payload).withServiceMetadata(first_parameter
							.getName());
				} else if (hasServiceCommand(first_parameter.getName())) {
					return new ServiceContext(this, action, request,
							previousResults, owner, collectionName, parameters,
							payload).withServiceCommand(first_parameter
							.getName());
				}

				List<String> aliases = new ArrayList<String>();
				String alias = "";
				String slash = "";
				for (ServiceParameter parameter : parameters) {
					if (parameter instanceof NameParameter) {
						String name = parameter.getName();
						if ((entityDictionaries != null)
								&& (entityDictionaries.contains(name))) {
							break;
						}
						if (Schema.getDefaultSchema().hasCollection(
								getServiceInfo().getItemType(), name)) {
							break;
						}
						alias += slash + name;
						aliases.add(alias);
						slash = "/";
					} else {
						break;
					}
				}
				if (!isEmpty(aliases)) {
					logger.info("Found {} potential paths", aliases.size());
					Map<String, EntityRef> aliasedEntities = em.getAlias(
							getEntityType(), aliases);
					for (int i = aliases.size() - 1; i >= 0; i--) {
						alias = aliases.get(i);
						pathEntity = aliasedEntities.get(alias);
						if (pathEntity != null) {
							logger.info("Found entity {} of type {} for alias {}",
                      new Object[]{pathEntity.getUuid(), pathEntity.getType(), alias});
							parameters = parameters.subList(i + 1,
									parameters.size());
							first_parameter = new IdParameter(
									pathEntity.getUuid());
							// if (!isEmpty(parameters)) {
							// first_parameter = parameters.get(0);
							// }
							break;
						}
					}
				}
			}

			if (pathEntity == null) {
				parameters = dequeueCopy(parameters);
			}
		}

		Query query = null;
		if (first_parameter instanceof QueryParameter) {
			query = first_parameter.getQuery();
		}
		parameters = mergeQueries(query, parameters);

		if (first_parameter instanceof IdParameter) {
			UUID id = first_parameter.getId();
			return new ServiceContext(this, action, request, previousResults,
					owner, collectionName, Query.fromUUID(id), parameters,
					payload);

		} else if (first_parameter instanceof NameParameter) {
			String name = first_parameter.getName();
			return new ServiceContext(this, action, request, previousResults,
					owner, collectionName, Query.fromIdentifier(name),
					parameters, payload);

		} else if (query != null) {
			return new ServiceContext(this, action, request, previousResults,
					owner, collectionName, query, parameters, payload);

		} else if (first_parameter == null) {
			return new ServiceContext(this, action, request, previousResults,
					owner, collectionName, null, null, payload);
		}

		throw new ServiceInvocationException(request, "No parameter found");
	}

}
