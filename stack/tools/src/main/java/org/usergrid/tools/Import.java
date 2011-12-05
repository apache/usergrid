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

import static org.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.usergrid.persistence.Schema.PROPERTY_UUID;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.entities.Application;

public class Import extends ToolBase {

	private static final Logger logger = LoggerFactory.getLogger(Import.class);

	/** Input directory where the .json export files are */
	static final String INPUT_DIR = "inputDir";

	static File importDir;

	static final String DEFAULT_INPUT_DIR = "export";

	JsonFactory jsonFactory = new JsonFactory();

	@Override
	@SuppressWarnings("static-access")
	public Options createOptions() {

		Option hostOption = OptionBuilder.withArgName("host").hasArg()
				.withDescription("Cassandra host").create("host");

		Option remoteOption = OptionBuilder.withDescription(
				"Use remote Cassandra instance").create("remote");

		Option inputDir = OptionBuilder.hasArg()
				.withDescription("input directory -inputDir").create(INPUT_DIR);

		Option verbose = OptionBuilder
				.withDescription(
						"Print on the console an echo of the content written to the file")
				.create(VERBOSE);

		Options options = new Options();
		options.addOption(hostOption);
		options.addOption(remoteOption);
		options.addOption(inputDir);
		options.addOption(verbose);

		return options;
	}

	@Override
	public void runTool(CommandLine line) throws Exception {
		startSpring();

		setVerbose(line);

		openImportDirectory(line);

		importOrganizations();

		importApplications();

		importCollections();
	}

	/**
	 * Import applications
	 */
	private void importApplications() throws Exception {
		String[] nanemspaceFileNames = importDir.list(new PrefixFileFilter(
				"application."));
		logger.info("Applications to read: " + nanemspaceFileNames.length);

		for (String applicationName : nanemspaceFileNames) {
			try {
				importApplication(applicationName);
			} catch (Exception e) {
				logger.warn("Unable to import application: " + applicationName,
						e);
			}
		}
	}

	/**
	 * Imports a application
	 * 
	 * @param applicationName
	 *            file name where the application was exported.
	 */
	private void importApplication(String applicationName) throws Exception {
		// Open up application file.
		File applicationFile = new File(importDir, applicationName);
		logger.info("Loading application file: "
				+ applicationFile.getAbsolutePath());
		JsonParser jp = getJsonParserForFile(applicationFile);

		JsonToken token = jp.nextToken();
		validateStartArray(token);

		// Move to next object (the application).
		// The application object is the first object followed by all the
		// objects in this application.
		token = jp.nextValue();

		Application application = jp.readValueAs(Application.class);
		@SuppressWarnings("unchecked")
		String organizationId = ((Map<String, String>) application
				.getMetadata("organization")).get("key");
		managementService.importApplication(UUID.fromString(organizationId),
				application);
		echo(application);

		EntityManager em = emf.getEntityManager(application.getUuid());

		while (jp.nextValue() != JsonToken.END_ARRAY) {
			@SuppressWarnings("unchecked")
			Map<String, Object> entityProps = jp.readValueAs(HashMap.class);
			// Import/create the entity
			UUID uuid = getId(entityProps);
			String type = getType(entityProps);
			try {
				em.create(uuid, type, entityProps);
			} catch (Exception e) {
				logger.error("Unable to create entity " + uuid + " of type "
						+ type
						+ ", skipping but this may indicate a bad import...", e);
			}
			echo(entityProps);
		}

		logger.info("----- End of application:" + application.getName());
		jp.close();
	}

	private String getType(Map<String, Object> entityProps) {
		return (String) entityProps.get(PROPERTY_TYPE);
	}

	private UUID getId(Map<String, Object> entityProps) {
		return UUID.fromString((String) entityProps.get(PROPERTY_UUID));
	}

	private void validateStartArray(JsonToken token) {
		if (token != JsonToken.START_ARRAY) {
			throw new RuntimeException("Token should be START ARRAY but it is:"
					+ token.asString());
		}

	}

	/**
	 * Import organizations
	 * 
	 */
	private void importOrganizations() throws Exception {

		String[] organizationFileNames = importDir.list(new PrefixFileFilter(
				"organization."));
		logger.info("Organizations to read: " + organizationFileNames.length);

		for (String organizationFileName : organizationFileNames) {

			try {
				importOrganization(organizationFileName);
			} catch (Exception e) {
				logger.warn("Unable to import organization:"
						+ organizationFileName, e);
			}
		}
	}

	/**
	 * Import an organization.
	 * 
	 * @param organizationFileName
	 *            file where the organization was exported
	 */
	private void importOrganization(String organizationFileName)
			throws Exception {
		OrganizationInfo acc = null;

		// Open up organization dir.
		File organizationFile = new File(importDir, organizationFileName);
		logger.info("Loading organization file: "
				+ organizationFile.getAbsolutePath());
		JsonParser jp = getJsonParserForFile(organizationFile);

		// Get the organization object and the only one in the file.
		acc = jp.readValueAs(OrganizationInfo.class);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		// properties.put("email", acc.getEmail());
		// properties.put("password", "password".getBytes("UTF-8"));

		echo(acc);
		managementService.importOrganization(acc.getUuid(), acc, properties);
		jp.close();
	}

	private JsonParser getJsonParserForFile(File organizationFile)
			throws Exception {
		JsonParser jp = jsonFactory.createJsonParser(organizationFile);
		jp.setCodec(new ObjectMapper());
		return jp;
	}

	/**
	 * Import collections. Collections files are named:
	 * collections.<application_name>.Timestamp.json
	 */
	private void importCollections() throws Exception {
		String[] collectionsFileNames = importDir.list(new PrefixFileFilter(
				"collections."));
		logger.info("Collections to read: " + collectionsFileNames.length);

		for (String collectionName : collectionsFileNames) {
			try {
				importCollection(collectionName);
			} catch (Exception e) {
				logger.warn("Unable to import collection: " + collectionName, e);
			}
		}
	}

	private void importCollection(String collectionFileName) throws Exception {
		// Retrieve the namepsace for this collection. It's part of the name
		String applicationName = getApplicationFromColllection(collectionFileName);
		File collectionFile = new File(importDir, collectionFileName);
		logger.info("Loading collections file: "
				+ collectionFile.getAbsolutePath());
		logger.info("  for application: " + applicationName);
		EntityManager em = emf.getEntityManager(emf
				.lookupApplication(applicationName));

		JsonParser jp = getJsonParserForFile(collectionFile);

		jp.nextToken(); // START_OBJECT this is the outter hashmap

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			importEntitysStuff(jp, em);
		}

		logger.info("----- End of collections -----");
		jp.close();
	}

	/**
	 * Imports the entity's connecting references (collections and connections)
	 * 
	 * @param jp
	 *            JsonPrser pointing to the beginning of the object.
	 * @param em
	 */
	private void importEntitysStuff(JsonParser jp, EntityManager em)
			throws Exception {
		// The entity that owns the collections
		String entityOwnerId = jp.getCurrentName();
		EntityRef ownerEntityRef = em.getRef(UUID.fromString(entityOwnerId));

		jp.nextToken(); // start object

		// Go inside the value after getting the owner entity id.
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			String collectionName = jp.getCurrentName();

			if (collectionName.equals("connections")) {

				jp.nextToken(); // START_OBJECT
				while (jp.nextToken() != JsonToken.END_OBJECT) {
					String connectionType = jp.getCurrentName();

					jp.nextToken(); // START_ARRAY
					while (jp.nextToken() != JsonToken.END_ARRAY) {
						String entryId = jp.getText();
						EntityRef entryRef = em
								.getRef(UUID.fromString(entryId));
						System.out.println(entityOwnerId + " " + connectionType
								+ " " + entryId);
						// Store in DB
						em.createConnection(ownerEntityRef, connectionType,
								entryRef);
					}
				}
			} else {
				// Regular collections

				jp.nextToken(); // START_ARRAY
				while (jp.nextToken() != JsonToken.END_ARRAY) {
					String entryId = jp.getText();
					EntityRef entryRef = em.getRef(UUID.fromString(entryId));

					// store it
					em.addToCollection(ownerEntityRef, collectionName, entryRef);
				}
			}
		}

	}

	/**
	 * Extract a application name from a collectionsFileName in the way:
	 * collections.<a_name_space_name>.TIMESTAMP.json
	 * 
	 * @param collectionFileName
	 *            a collection file name
	 * @return the application name for this collections file name
	 */
	private String getApplicationFromColllection(String collectionFileName) {
		int firstDot = collectionFileName.indexOf(".");
		int secondDot = collectionFileName.indexOf(".", firstDot + 1);

		// The application will be in the subString between the dots.

		return collectionFileName.substring(firstDot + 1, secondDot);
	}

	/**
	 * Open up the import directory based on <code>importDir</code>
	 */
	private void openImportDirectory(CommandLine line) {

		boolean hasInputDir = line.hasOption(INPUT_DIR);

		if (hasInputDir) {
			importDir = new File(line.getOptionValue(INPUT_DIR));
		} else {
			importDir = new File(DEFAULT_INPUT_DIR);
		}

		logger.info("Importing from:" + importDir.getAbsolutePath());
		logger.info("Status. Exists: " + importDir.exists() + " - Readable: "
				+ importDir.canRead());
	}

}
