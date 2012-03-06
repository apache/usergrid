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
