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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test extends ToolBase {

	private static final Logger logger = LoggerFactory.getLogger(Test.class);

	@Override
	public Options createOptions() {

		Option useSpring = OptionBuilder.create("spring");

		Options options = new Options();
		options.addOption(useSpring);

		return options;
	}

	@Override
	public void runTool(CommandLine line) throws Exception {
		logger.info("Starting test...");
		boolean startSpring = line.hasOption("spring");
		if (startSpring) {
			startSpring();
		}

	}

}
