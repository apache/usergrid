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
package org.usergrid.rest;

import static org.usergrid.persistence.SimpleEntityRef.ref;
import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION_ID;
import static org.usergrid.services.ServiceParameter.parameters;
import static org.usergrid.services.ServicePayload.batchPayload;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.rest.security.annotations.RequireSystemAccess;
import org.usergrid.services.ServiceAction;
import org.usergrid.services.ServiceManager;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;
import org.usergrid.utils.JsonUtils;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Path("/test")
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class TestResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(TestResource.class);

	public TestResource() {

	}

	private String getSampleDataUrl(String file) {
		String url = properties.getProperty("usergrid.test.sample_data_url")
				+ file;
		return url;
	}

	@RequireSystemAccess
	@GET
	@Path("load_test_users")
	public JSONWithPadding loadTestUsers(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("loading users");

		Map<String, String> properties = emf.getServiceProperties();
		if (properties == null) {
			response.setError("Unable to retrieve system properties, database is probably down.");
			return new JSONWithPadding(response, callback);
		}

		if ("true".equalsIgnoreCase(properties.get("test.users.loaded"))) {
			response.setError("Test users were already loaded.");
			return new JSONWithPadding(response, callback);
		}

		emf.setServiceProperty("test.users.loaded", "true");

		logger.info("Loading users");

		UUID nsId = emf.lookupApplication("test-app");
		if (nsId == null) {
			response.setError("No test-app application created.");
			return new JSONWithPadding(response, callback);
		}

		Object json = JsonUtils.loadFromUrl(getSampleDataUrl("userlist.json"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> set = (List<Map<String, Object>>) json;

		ServiceManager sm = smf.getServiceManager(nsId);
		ServiceRequest request = sm.newRequest(ServiceAction.POST,
				parameters("users"), batchPayload(set));
		request.execute();

		response.setSuccess();

		logger.info("Users loaded");

		return new JSONWithPadding(response, callback);
	}

	@RequireSystemAccess
	@SuppressWarnings("unchecked")
	@GET
	@Path("load_sxsw_data")
	public JSONWithPadding loadSXSWData(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("loading sxsw data");

		Map<String, String> properties = emf.getServiceProperties();
		if (properties == null) {
			response.setError("Unable to retrieve system properties, database is probably down.");
			return new JSONWithPadding(response);
		}

		if ("true".equalsIgnoreCase(properties.get("test.sxsw_data.loaded"))) {
			response.setError("SXSW test data already loaded.");
			return new JSONWithPadding(response);
		}

		emf.setServiceProperty("test.sxsw_data.loaded", "true");

		UUID nsId = emf.lookupApplication("sxsw");
		if (nsId == null) {
			response.setError("No SXSW application created.");
			return new JSONWithPadding(response);
		}

		ServiceManager sm = smf.getServiceManager(nsId);

		logger.info("Loading SXSW data");

		logger.info("Fetching SXSW conference events");

		Object json = JsonUtils
				.loadFromUrl(getSampleDataUrl("sxsw_events.json"));
		List<Map<String, Object>> set = (List<Map<String, Object>>) json;

		logger.info("SXSW conference events fetched");

		String pattern = "EEEE MMMM d hh:mma zzz yyyy";
		SimpleDateFormat format = new SimpleDateFormat(pattern);

		logger.info("Preparing SXSW conference events");

		for (Map<String, Object> event : set) {
			String timestr = (String) event.get("time");
			String datestr = (String) event.get("date");
			if (StringUtils.isNotBlank(datestr)
					&& StringUtils.isNotBlank(timestr)) {
				datestr = datestr + " " + timestr + " CST 2011";
				Date d = format.parse(datestr);
				event.put("time", d.getTime());
				event.put("date",
						DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(d));
			} else {
				event.remove("date");
				event.remove("time");
			}
		}

		logger.info("SXSW conference events prepared");

		logger.info("Storing SXSW conference events");

		ServiceRequest request = sm.newRequest(ServiceAction.POST,
				parameters("conference_events"), batchPayload(set));
		ServiceResults conference_events = request.execute();

		logger.info("SXSW conference events stored");

		logger.info("Fetching SXSW presenters");

		json = JsonUtils.loadFromUrl(getSampleDataUrl("sxsw_presenters.json"));
		set = (List<Map<String, Object>>) json;

		logger.info("SXSW presenters fetched");

		logger.info("Storing SXSW presenters");

		request = sm.newRequest(ServiceAction.POST, parameters("presenters"),
				batchPayload(set));
		ServiceResults sxsw_presenters = request.execute();

		logger.info("SXSW presenters stored");

		logger.info("Fetching SXSW events to presenters list");

		json = JsonUtils
				.loadFromUrl(getSampleDataUrl("sxsw_event_presenters.json"));

		logger.info("SXSW events to presenters list fetched");

		logger.info("Connecting SXSW events to presenters");

		Map<String, List<String>> map = (Map<String, List<String>>) json;
		EntityManager em = sm.getEntityManager();
		for (Entry<String, List<String>> list : map.entrySet()) {
			if (StringUtils.isNotBlank(list.getKey())
					&& CollectionUtils.isNotEmpty(list.getValue())) {
				Entity event = conference_events.findForProperty("eventid",
						list.getKey()).getEntity();
				/*
				 * Entity event = em.searchCollection(sm.getApplicationRef(),
				 * "conference_events", Query.findForProperty("eventid",
				 * list.getKey())) .getEntity();
				 */
				if (event != null) {
					for (String name : list.getValue()) {
						Entity presenter = sxsw_presenters.findForProperty(
								"name", name).getEntity();
						/*
						 * Entity presenter = em.searchCollection(
						 * sm.getApplicationRef(), "presenters",
						 * Query.findForProperty("name", name)) .getEntity();
						 */
						if (presenter != null) {
							logger.info("Connecting "
									+ event.getProperty("title")
									+ " to presenter "
									+ presenter.getProperty("name"));
							em.createConnection(event, "presenter", presenter);
						}
					}
				}
			}
		}

		logger.info("SXSW events connected to presenters");

		response.setSuccess();

		logger.info("SXSW data loaded");

		return new JSONWithPadding(response, callback);
	}

	@GET
	@Path("hello")
	public JSONWithPadding helloWorld(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback) {

		logger.info("Saying hello");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("hello world!");
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("testpost")
	public JSONWithPadding testPost(Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback) {

		return new JSONWithPadding(json, callback);
	}

	@GET
	@Path("testjson")
	public JSONWithPadding testJson(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("test json");
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireSystemAccess
	@GET
	@Path("connect")
	public JSONWithPadding testConnect(@Context UriInfo ui,
			@QueryParam("from") UUID from, @QueryParam("to") UUID to,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("test connection");

		UUID applicationId = DEFAULT_APPLICATION_ID;
		EntityManager em = emf.getEntityManager(applicationId);
		em.createConnection(ref(from), "likes", ref(to));

		response.setSuccess();
		// response.setResult(c);

		return new JSONWithPadding(response, callback);
	}

	@GET
	@Path("page")
	public Viewable page(@Context UriInfo ui) throws Exception {

		return new Viewable("test", this);
	}

	public String getFoo() {
		return "FOO";
	}
}
