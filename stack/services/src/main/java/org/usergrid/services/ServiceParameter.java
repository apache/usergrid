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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.utils.ListUtils.dequeue;
import static org.usergrid.utils.ListUtils.dequeueCopy;
import static org.usergrid.utils.ListUtils.isEmpty;
import static org.usergrid.utils.ListUtils.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query;

public abstract class ServiceParameter {

	private static final Logger logger = LoggerFactory
			.getLogger(ServiceParameter.class);

	ServiceParameter() {

	}

	public UUID getId() {
		return null;
	}

	public String getName() {
		return null;
	}

	public Query getQuery() {
		return null;
	}

	@Override
	public String toString() {
		return "";
	}

	public boolean isQuery() {
		return false;
	}

	public boolean isId() {
		return false;
	}

	public boolean isName() {
		return false;
	}

	public static List<ServiceParameter> addParameter(
			List<ServiceParameter> parameters, UUID entityId) {

		if (parameters == null) {
			parameters = new ArrayList<ServiceParameter>();
		}

		if (entityId == null) {
			return parameters;
		}

		ServiceParameter p = new IdParameter(entityId);
		parameters.add(p);
		return parameters;
	}

	public static List<ServiceParameter> addParameter(
			List<ServiceParameter> parameters, String name) {

		if (parameters == null) {
			parameters = new ArrayList<ServiceParameter>();
		}

		if (name == null) {
			return parameters;
		}

		if ("all".equals(name)) {
			Query query = new Query();
			ServiceParameter p = new QueryParameter(query);
			parameters.add(p);
			return parameters;
		}

		ServiceParameter p = new NameParameter(name);
		parameters.add(p);
		return parameters;
	}

	public static List<ServiceParameter> addParameter(
			List<ServiceParameter> parameters, Query query) {

		if (parameters == null) {
			parameters = new ArrayList<ServiceParameter>();
		}

		if (query == null) {
			return parameters;
		}

		if (lastParameterIsQuery(parameters)) {
			logger.error("Adding two queries in a row");
		}

		ServiceParameter p = new QueryParameter(query);
		parameters.add(p);
		return parameters;
	}

	public static List<ServiceParameter> addParameters(
			List<ServiceParameter> parameters, Object... params)
			throws Exception {

		if (parameters == null) {
			parameters = new ArrayList<ServiceParameter>();
		}

		if (params == null) {
			return parameters;
		}

		for (Object param : params) {
			if (param instanceof UUID) {
				addParameter(parameters, (UUID) param);
			} else if (param instanceof String) {
				addParameter(parameters, (String) param);
			} else if (param instanceof Query) {
				addParameter(parameters, (Query) param);
			}
		}

		return parameters;
	}

	public static List<ServiceParameter> parameters(Object... params)
			throws Exception {
		return addParameters(null, params);
	}

	public static boolean firstParameterIsName(List<ServiceParameter> parameters) {
		if (!isEmpty(parameters)) {
			return parameters.get(0).isName();
		}
		return false;
	}

	public static boolean lastParameterIsName(List<ServiceParameter> parameters) {
		if (!isEmpty(parameters)) {
			return parameters.get(parameters.size() - 1).isName();
		}
		return false;
	}

	public static boolean firstParameterIsQuery(
			List<ServiceParameter> parameters) {
		if (!isEmpty(parameters)) {
			return parameters.get(0).isQuery();
		}
		return false;
	}

	public static boolean lastParameterIsQuery(List<ServiceParameter> parameters) {
		if (!isEmpty(parameters)) {
			return parameters.get(parameters.size() - 1).isQuery();
		}
		return false;
	}

	public static boolean firstParameterIsId(List<ServiceParameter> parameters) {
		if (!isEmpty(parameters)) {
			return parameters.get(0).isId();
		}
		return false;
	}

	public static boolean lastParameterIsId(List<ServiceParameter> parameters) {
		if (!isEmpty(parameters)) {
			return parameters.get(parameters.size() - 1).isId();
		}
		return false;
	}

	public static ServiceParameter firstParameter(
			List<ServiceParameter> parameters) {
		if (!isEmpty(parameters)) {
			return parameters.get(0);
		}

		return null;
	}

	public static boolean moreParameters(List<ServiceParameter> parameters) {
		return moreParameters(parameters, true);
	}

	public static boolean moreParameters(List<ServiceParameter> parameters,
			boolean ignoreTrailingQueries) {
		if (isEmpty(parameters)) {
			return false;
		}
		if (ignoreTrailingQueries) {
			for (ServiceParameter parameter : parameters) {
				if (!(parameter instanceof QueryParameter)) {
					return true;
				}
			}
			return false;
		}
		return true;

	}

	public static int parameterCount(List<ServiceParameter> parameters) {
		return parameterCount(parameters, false);
	}

	public static int parameterCount(List<ServiceParameter> parameters,
			boolean ignoreTrailingQueries) {
		if (isEmpty(parameters)) {
			return 0;
		}
		int count = parameters.size();
		if (ignoreTrailingQueries) {
			count = 0;
			for (ServiceParameter parameter : parameters) {
				if (!(parameter instanceof QueryParameter)) {
					count++;
				} else {
					return count;
				}
			}
		}
		return count;
	}

	public static ServiceParameter dequeueParameter(
			List<ServiceParameter> parameters) {
		return dequeue(parameters);
	}

	public static void queueParameter(List<ServiceParameter> parameters,
			ServiceParameter parameter) {
		parameters = queue(parameters, parameter);
	}

	public static List<ServiceParameter> mergeQueries(Query query,
			List<ServiceParameter> parameters) {
		while (firstParameterIsQuery(parameters)) {
			parameters = dequeueCopy(parameters);
		}
		return parameters;
	}

	public static List<ServiceParameter> filter(
			List<ServiceParameter> parameters,
			Map<List<String>, List<String>> replaceParameters) {
		if (replaceParameters == null) {
			return parameters;
		}
		if ((parameters == null) || (parameters.size() == 0)) {
			return parameters;
		}
		for (Entry<List<String>, List<String>> replaceSet : replaceParameters
				.entrySet()) {
			if (parameters.size() < replaceSet.getKey().size()) {
				continue;
			}
			boolean found = true;
			for (int i = 0; i < replaceSet.getKey().size(); i++) {
				String matchStr = replaceSet.getKey().get(i);
				ServiceParameter param = parameters.get(i);
				if (matchStr.equals("$id")
						&& ((param instanceof IdParameter) || (param instanceof NameParameter))) {
					continue;
				} else if (matchStr.equals("$query")
						&& (param instanceof QueryParameter)) {
					continue;
				} else if (matchStr.equalsIgnoreCase(param.getName())) {
					continue;
				}
				found = false;
				break;
			}
			if (!found) {
				continue;
			}
			ArrayList<ServiceParameter> p = new ArrayList<ServiceParameter>();
			for (String name : replaceSet.getValue()) {
				if (name.startsWith("\\")) {
					int i = Integer.parseInt(name.substring(1));
					p.add(parameters.get(i));
				} else {
					p.add(new NameParameter(name));
				}
			}
			p.addAll(parameters.subList(replaceSet.getKey().size(),
					parameters.size()));
			return p;
		}
		return parameters;
	}

	public static class IdParameter extends ServiceParameter {
		UUID entityId;

		public IdParameter(UUID entityId) {
			this.entityId = entityId;
		}

		@Override
		public UUID getId() {
			return entityId;
		}

		@Override
		public boolean isId() {
			return true;
		}

		@Override
		public String toString() {
			return entityId.toString();
		}

	}

	public static class NameParameter extends ServiceParameter {
		String name;

		public NameParameter(String name) {
			name = name.trim().toLowerCase();
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isName() {
			return true;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	public static class QueryParameter extends ServiceParameter {
		Query query;

		public QueryParameter(Query query) {
			this.query = query;
		}

		@Override
		public Query getQuery() {
			return query;
		}

		@Override
		public boolean isQuery() {
			return true;
		}

		@Override
		public String toString() {
			String queryStr = query.toString();
			if (isNotBlank(queryStr)) {
				return queryStr;
			}
			return "";
		}
	}
}
