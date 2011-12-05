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
package org.usergrid.tools;

import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION_ID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.codehaus.jackson.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.services.ServiceAction;
import org.usergrid.services.ServiceManager;
import org.usergrid.services.ServiceParameter;
import org.usergrid.services.ServicePayload;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;
import org.usergrid.utils.HttpUtils;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;

public class Cli extends ToolBase {

	public static final int MAX_ENTITY_FETCH = 100;

	private static final Logger logger = LoggerFactory.getLogger(Cli.class);

	JsonFactory jsonFactory = new JsonFactory();

	@Override
	@SuppressWarnings("static-access")
	public Options createOptions() {

		Option hostOption = OptionBuilder.withArgName("host").hasArg()
				.withDescription("Cassandra host").create("host");

		Option remoteOption = OptionBuilder.withDescription(
				"Use remote Cassandra instance").create("remote");

		Options options = new Options();
		options.addOption(hostOption);
		options.addOption(remoteOption);

		return options;
	}

	@Override
	public void runTool(CommandLine line) throws Exception {
		startSpring();
		handleInput();
	}

	public void handleInput() {
		BufferedReader d = new BufferedReader(new InputStreamReader(System.in));

		UUID applicationId = DEFAULT_APPLICATION_ID;

		while (true) {
			System.out.println();
			System.out.print(">");
			String s = null;
			try {
				s = d.readLine();
			} catch (IOException e) {
			}
			if (s == null) {
				System.exit(0);
			}

			s = s.trim().toLowerCase();
			if (s.startsWith("use ")) {
				s = s.substring(4);
				applicationId = UUIDUtils.tryExtractUUID(s);
				if (applicationId == null) {
					try {
						applicationId = emf.lookupApplication(s.trim());
					} catch (Exception e) {
					}
				}
				if (applicationId == null) {
					applicationId = DEFAULT_APPLICATION_ID;
				}
				System.out.println("Using application " + applicationId);
				continue;
			}

			ServiceAction action = ServiceAction.GET;
			if (s.startsWith("get ")) {
				s = s.substring("get ".length()).trim();
			} else if (s.startsWith("post ")) {
				s = s.substring("post ".length()).trim();
				action = ServiceAction.POST;
			} else if (s.startsWith("put ")) {
				s = s.substring("put".length()).trim();
				action = ServiceAction.PUT;
			} else if (s.startsWith("delete ")) {
				s = s.substring("delete ".length()).trim();
				action = ServiceAction.DELETE;
			} else if (s.startsWith("quit")) {
				return;
			}

			List<ServiceParameter> parameters = new ArrayList<ServiceParameter>();
			int i = 0;
			boolean next_is_query = false;
			boolean next_is_payload = false;
			boolean next_is_json = false;
			ServicePayload payload = null;
			while (i < s.length()) {
				boolean is_query = next_is_query;
				if (next_is_payload) {
					String str = s.substring(i);
					payload = ServicePayload.stringPayload(str);
					break;
				} else if (next_is_json) {
					next_is_json = false;
					int start = i - 1;
					int bracket_count = 1;
					while (i < s.length()) {
						char c = s.charAt(i);
						if (c == '{') {
							bracket_count++;
						} else if (c == '}') {
							bracket_count--;
						}
						if (bracket_count == 0) {
							i++;
							String json = s.substring(start, i);
							Query query = Query.fromJsonString(json);
							ServiceParameter.addParameter(parameters, query);
							if ((i < s.length()) && (s.charAt(i) == '/')) {
								i++;
							}
							break;
						}
						i++;
					}
					continue;
				}

				next_is_query = false;
				next_is_payload = false;
				next_is_json = false;
				int slash = s.indexOf('/', i);
				int semicolon = s.indexOf(';', i);
				int question = s.indexOf('?', i);
				int space = s.indexOf(' ', i);
				int bracket = s.indexOf('{', i);
				int j = s.length();

				if ((slash >= 0) && (slash < j)) {
					j = slash;
				}

				if ((space >= 0) && (space < j)) {
					j = space;
					next_is_payload = true;
				}

				if ((semicolon >= 0) && (semicolon < j)) {
					next_is_query = true;
					next_is_payload = false;
					j = semicolon;
				}

				if ((question >= 0) && (question < j)) {
					next_is_query = true;
					next_is_payload = false;
					j = question;
				}

				if ((bracket >= 0) && (bracket < j)) {
					next_is_query = false;
					next_is_payload = false;
					next_is_json = true;
					j = bracket;
				}

				String segment = s.substring(i, j);
				if (segment.length() > 0) {
					if (is_query) {
						Map<String, List<String>> params = HttpUtils
								.parseQueryString(segment);
						Query query = Query.fromQueryParams(params);
						ServiceParameter.addParameter(parameters, query);
					} else {
						UUID uuid = UUIDUtils.tryGetUUID(segment);
						if (uuid != null) {
							ServiceParameter.addParameter(parameters, uuid);
						} else {
							ServiceParameter.addParameter(parameters, segment);
						}
					}
				}

				i = j + 1;
			}

			if (parameters.size() == 0) {
				continue;
			}

			System.out.println(action + " " + parameters + " " + payload);
			ServiceManager services = smf.getServiceManager(applicationId);
			ServiceRequest r = null;
			try {
				r = services.newRequest(action, parameters, payload);
			} catch (Exception e) {
				logger.error("Error", e);
			}
			ServiceResults results = null;
			try {
				results = r.execute();
			} catch (Exception e) {
				logger.error("Error", e);
			}
			if (results != null) {
				if (results.hasData()) {
					System.out.println(JsonUtils
							.mapToFormattedJsonString(results.getData()));
				}
				if (results.getServiceMetadata() != null) {
					System.out.println(JsonUtils
							.mapToFormattedJsonString(results
									.getServiceMetadata()));
				}
				System.out.println(JsonUtils.mapToFormattedJsonString(results
						.getEntities()));

			}
		}

	}
}
