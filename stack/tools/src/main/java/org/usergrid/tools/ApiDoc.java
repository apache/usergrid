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

import static org.usergrid.utils.StringUtils.readClasspathFileAsString;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.schema.CollectionInfo;
import org.usergrid.tools.apidoc.swagger.ApiListing;
import org.usergrid.utils.JsonUtils;
import org.w3c.dom.Document;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ApiDoc extends ToolBase {

	private static final Logger logger = LoggerFactory.getLogger(ApiDoc.class);

	@Override
	public Options createOptions() {

		Option generateWadl = OptionBuilder.create("wadl");

		Options options = new Options();
		options.addOption(generateWadl);

		return options;
	}

	@Override
	public void runTool(CommandLine line) throws Exception {
		logger.info("Generating applications docs...");

		ApiListing listing = loadListing("applications");
		output(listing, "applications");

		logger.info("Generating management docs...");

		listing = loadListing("management");
		output(listing, "management");

		logger.info("Done!");
	}

	public ApiListing loadListing(String section) {
		Yaml yaml = new Yaml(new Constructor(ApiListing.class));
		String yamlString = readClasspathFileAsString("/apidoc/" + section
				+ ".yaml");
		ApiListing listing = (ApiListing) yaml.load(yamlString);
		return listing;
	}

	public void output(ApiListing listing, String section) throws IOException,
			TransformerException {
		Document doc = listing.createWADLApplication();

		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		transformerFactory.setAttribute("indent-number", 4);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", "4");

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(section + ".wadl"));
		transformer.transform(source, result);

		File file = new File(section + ".json");
		listing.setBasePath("${basePath}");
		FileUtils.write(file, JsonUtils.mapToFormattedJsonString(listing));

	}

	public void addCollections(ApiListing listing) {
		Map<String, CollectionInfo> collections = Schema.getDefaultSchema()
				.getCollections(Application.ENTITY_TYPE);
		collections.clear();

	}
}
