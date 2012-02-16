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
package org.usergrid.rest.applications;

import static org.usergrid.services.ServiceParameter.addParameter;
import static org.usergrid.services.ServicePayload.batchPayload;
import static org.usergrid.services.ServicePayload.idListPayload;
import static org.usergrid.services.ServicePayload.payload;
import static org.usergrid.utils.JsonUtils.mapToJsonString;
import static org.usergrid.utils.JsonUtils.normalizeJsonTree;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.AggregateCounter;
import org.usergrid.persistence.AggregateCounterSet;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Query;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.usergrid.services.ServiceAction;
import org.usergrid.services.ServiceManager;
import org.usergrid.services.ServiceParameter;
import org.usergrid.services.ServicePayload;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.core.provider.EntityHolder;

@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class ServiceResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(ServiceResource.class);

	ServiceManager services;
	List<ServiceParameter> serviceParameters = null;

	public ServiceResource(AbstractContextResource parent) throws Exception {
		super(parent);

	}

	public ServiceResource(ServiceResource parent) throws Exception {
		super(parent);
		services = parent.services;
	}

	public ServiceResource getServiceResourceParent() {
		if (parent instanceof ServiceResource) {
			return (ServiceResource) parent;
		}
		return null;
	}

	public ServiceManager getServices() {
		return services;
	}

	public UUID getApplicationId() {
		return services.getApplicationId();
	}

	public List<ServiceParameter> getServiceParameters() {
		if (serviceParameters != null) {
			return serviceParameters;
		}
		if (getServiceResourceParent() != null) {
			return getServiceResourceParent().getServiceParameters();
		}
		serviceParameters = new ArrayList<ServiceParameter>();
		return serviceParameters;
	}

	public static List<ServiceParameter> addMatrixParams(
			List<ServiceParameter> parameters, UriInfo ui, PathSegment ps)
			throws Exception {

		MultivaluedMap<String, String> params = ps.getMatrixParameters();
		if (params != null) {
			Query query = Query.fromQueryParams(params);
			if (query != null) {
				parameters = addParameter(parameters, query);
			}
		}

		return parameters;

	}

	public static List<ServiceParameter> addQueryParams(
			List<ServiceParameter> parameters, UriInfo ui) throws Exception {

		MultivaluedMap<String, String> params = ui.getQueryParameters();
		if (params != null) {
			Query query = Query.fromQueryParams(params);
			if (query != null) {
				parameters = addParameter(parameters, query);
			}
		}

		return parameters;

	}

	@Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public AbstractContextResource addIdParameter(@Context UriInfo ui,
			@PathParam("entityId") PathSegment entityId) throws Exception {

		logger.info("ServiceResource.addIdParameter");

		UUID itemId = UUID.fromString(entityId.getPath());

		addParameter(getServiceParameters(), itemId);

		addMatrixParams(getServiceParameters(), ui, entityId);

		return new ServiceResource(this);
	}

	@Path("{itemName}")
	public AbstractContextResource addNameParameter(@Context UriInfo ui,
			@PathParam("itemName") PathSegment itemName) throws Exception {

		logger.info("ServiceResource.addNameParameter");

		logger.info("Current segment is " + itemName.getPath());

		if (itemName.getPath().startsWith("{")) {
			Query query = Query.fromJsonString(itemName.getPath());
			if (query != null) {
				addParameter(getServiceParameters(), query);
			}
		} else {
			addParameter(getServiceParameters(), itemName.getPath());
		}

		addMatrixParams(getServiceParameters(), ui, itemName);

		return new ServiceResource(this);
	}

	public ServiceResults executeServiceRequest(UriInfo ui,
			ApiResponse response, ServiceAction action, ServicePayload payload)
			throws Exception {

		logger.info("ServiceResource.executeServiceRequest");

		boolean tree = "true".equalsIgnoreCase(ui.getQueryParameters()
				.getFirst("tree"));

		addQueryParams(getServiceParameters(), ui);
		ServiceRequest r = services.newRequest(action, tree,
				getServiceParameters(), payload);
		response.setServiceRequest(r);
		ServiceResults results = r.execute();
		if (results != null) {
			if (results.hasData()) {
				response.setData(results.getData());
			}
			if (results.getServiceMetadata() != null) {
				response.setMetadata(results.getServiceMetadata());
			}
			Query query = r.getLastQuery();
			if (query != null) {
				query = new Query(query);
				query.setIdsOnly(false);
				if (query.hasSelectSubjects()) {
					response.setList(query.getSelectionResults(results));
					response.setNext(results.getNextResult());
					response.setPath(results.getPath());
					return results;
				}
			}
			response.setResults(results);

		}

		httpServletRequest.setAttribute("applicationId",
				services.getApplicationId());

		return results;
	}

	@GET
	@RequireApplicationAccess
	public JSONWithPadding executeGet(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("ServiceResource.executeGet");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get");
		response.setApplication(services.getApplicationId());
		response.setParams(ui.getQueryParameters());

		executeServiceRequest(ui, response, ServiceAction.GET, null);

		return new JSONWithPadding(response, callback);
	}

	@SuppressWarnings({ "unchecked" })
	public ServicePayload getPayload(Object json) {
		ServicePayload payload = null;
		json = normalizeJsonTree(json);
		if (json instanceof Map) {
			Map<String, Object> jsonMap = (Map<String, Object>) json;
			payload = payload(jsonMap);
		} else if (json instanceof List) {
			List<?> jsonList = (List<?>) json;
			if (jsonList.size() > 0) {
				if (jsonList.get(0) instanceof UUID) {
					payload = idListPayload((List<UUID>) json);
				} else if (jsonList.get(0) instanceof Map) {
					payload = batchPayload((List<Map<String, Object>>) jsonList);
				}
			}
		}
		if (payload == null) {
			payload = new ServicePayload();
		}
		return payload;
	}

	@POST
	@RequireApplicationAccess
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONWithPadding executePost(@Context UriInfo ui,
			EntityHolder<Object> body,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("ServiceResource.executePost");

		Object json = body.getEntity();

		ApiResponse response = new ApiResponse(ui);
		response.setAction("post");
		response.setApplication(services.getApplicationId());
		response.setParams(ui.getQueryParameters());

		ServicePayload payload = getPayload(json);

		executeServiceRequest(ui, response, ServiceAction.POST, payload);

		return new JSONWithPadding(response, callback);
	}

	@PUT
	@RequireApplicationAccess
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONWithPadding executePut(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("ServiceResource.executePut");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("put");
		response.setApplication(services.getApplicationId());
		response.setParams(ui.getQueryParameters());

		ServicePayload payload = getPayload(json);

		executeServiceRequest(ui, response, ServiceAction.PUT, payload);

		return new JSONWithPadding(response, callback);
	}

	@DELETE
	@RequireApplicationAccess
	public JSONWithPadding executeDelete(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("ServiceResource.executeDelete");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("delete");
		response.setApplication(services.getApplicationId());
		response.setParams(ui.getQueryParameters());

		response.setError("Delete collection not implemented");

		executeServiceRequest(ui, response, ServiceAction.DELETE, null);

		return new JSONWithPadding(response, callback);
	}

	@Produces("text/csv")
	@GET
	@RequireApplicationAccess
	public String executeGetCsv(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {
		ui.getQueryParameters().putSingle("pad", "true");
		JSONWithPadding jsonp = executeGet(ui, callback);

		StringBuilder builder = new StringBuilder();
		if ((jsonp != null) && (jsonp.getJsonSource() instanceof ApiResponse)) {
			ApiResponse apiResponse = (ApiResponse) jsonp.getJsonSource();
			if ((apiResponse.getCounters() != null)
					&& (apiResponse.getCounters().size() > 0)) {
				List<AggregateCounterSet> counters = apiResponse.getCounters();
				int size = counters.get(0).getValues().size();
				List<AggregateCounter> firstCounterList = counters.get(0)
						.getValues();
				if (size > 0) {
					builder.append("timestamp");
					for (AggregateCounterSet counterSet : counters) {
						builder.append(",");
						builder.append(counterSet.getName());
					}
					builder.append("\n");
					SimpleDateFormat formatter = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss.SSS");
					for (int i = 0; i < size; i++) {
						// yyyy-mm-dd hh:mm:ss.000
						builder.append(formatter.format(new Date(
								firstCounterList.get(i).getTimestamp())));
						for (AggregateCounterSet counterSet : counters) {
							List<AggregateCounter> counterList = counterSet
									.getValues();
							builder.append(",");
							builder.append(counterList.get(i).getValue());
						}
						builder.append("\n");
					}
				}
			} else if ((apiResponse.getEntities() != null)
					&& (apiResponse.getEntities().size() > 0)) {
				for (Entity entity : apiResponse.getEntities()) {
					builder.append(entity.getUuid());
					builder.append(",");
					builder.append(entity.getType());
					builder.append(",");
					builder.append(mapToJsonString(entity));
				}

			}
		}
		return builder.toString();
	}

}
