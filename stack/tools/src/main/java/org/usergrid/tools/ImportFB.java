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
