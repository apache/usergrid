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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.utils.JsonUtils;

public class ImportFB extends ToolBase {

	private static final Logger logger = LoggerFactory.getLogger(ImportFB.class);

	@Override
	@SuppressWarnings("static-access")
	public Options createOptions() {

		Option input = OptionBuilder.withArgName("file").hasArg().isRequired()
				.withDescription("JSON file of array of Facebook users")
				.create("i");

		Option output = OptionBuilder.withArgName("file").hasArg().isRequired()
				.withDescription("JSON file with array of Usergrid users")
				.create("o");

		Options options = new Options();
		options.addOption(input);
		options.addOption(output);

		return options;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void runTool(CommandLine line) throws Exception {
		String input = line.getOptionValue("i");
		String output = line.getOptionValue("o");

		logger.info("Importing fbusers.json");

		Object json = JsonUtils.loadFromFilesystem(input);
		Map<String, Object> map = (Map<String, Object>) json;
		List<Map<String, Object>> fbusers = (List<Map<String, Object>>) map
				.get("data");
		List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> fbuser : fbusers) {
			// logger.info(fbuser.get("name"));

			Map<String, Object> user = new LinkedHashMap<String, Object>();
			String name = (String) fbuser.get("name");
			String username = (String) fbuser.get("username");
			String picture = (String) fbuser.get("picture");
			user.put("name", name);
			if (username == null) {
				username = name.replace(' ', '.');
			}
			username = username.toLowerCase();
			user.put("username", username);
			user.put("picture", picture);
			user.put("facebook", fbuser);
			users.add(user);
		}
		logger.info("Imported " + users.size() + " users");
		// System.out.println(JsonUtils.mapToFormattedJsonString(users));

		try {
			PrintWriter out = new PrintWriter(new File(output), "UTF-8");
			out.print(JsonUtils.mapToFormattedJsonString(users));
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
