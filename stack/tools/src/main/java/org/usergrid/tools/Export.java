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
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.persistence.ConnectionRef;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Results.Level;
import org.usergrid.utils.JsonUtils;

import com.google.common.collect.BiMap;

public class Export extends ToolBase {

	public static final boolean FORCE_USE_PRODUCTION = false;

	public static final int MAX_ENTITY_FETCH = 100;

	private static final Logger logger = LoggerFactory.getLogger(Export.class);

	/** Verbose option: -v */
	private static final String VERBOSE = "v";

	private boolean isVerboseEnabled = false;

	/** Output dir option: -outputDir */
	private static final String OUTPUT_DIR = "outputDir";

	private static File exportDir;

	private String baseOutputDirName = "export";

	JsonFactory jsonFactory = new JsonFactory();

	@Override
	@SuppressWarnings("static-access")
	public Options createOptions() {

		Option hostOption = OptionBuilder.withArgName("host").hasArg()
				.withDescription("Cassandra host").create("host");

		Option remoteOption = OptionBuilder.withDescription(
				"Use remote Cassandra instance").create("remote");

		Option outputDir = OptionBuilder.hasArg()
				.withDescription("output file name -outputDir")
				.create(OUTPUT_DIR);

		Option verbose = OptionBuilder
				.withDescription(
						"Print on the console an echo of the content written to the file")
				.create(VERBOSE);

		Options options = new Options();
		options.addOption(hostOption);
		options.addOption(remoteOption);
		options.addOption(outputDir);
		options.addOption(verbose);

		return options;
	}

	@Override
	public void runTool(CommandLine line) throws Exception {
		startSpring();

		setVerbose(line);

		prepareBaseOutputFileName(line);
		exportDir = createOutputParentDir();
		logger.info("Export directory: " + exportDir.getAbsolutePath());

		// Export organizations separately.
		exportOrganizations();

		// Loop through the organizations
		BiMap<UUID, String> organizations = managementService
				.getOrganizations();
		for (Entry<UUID, String> organization : organizations.entrySet()) {

			if (organization.equals(properties
					.getProperty("usergrid.test-account.organization"))) {
				// Skip test data from being exported.
				continue;
			}

			exportApplicationsForOrg(organization);
		}
	}

	private void exportApplicationsForOrg(Entry<UUID, String> organization)
			throws Exception {
		logger.info("" + organization);

		// Loop through the applications per organization
		BiMap<UUID, String> applications = managementService
				.getApplicationsForOrganization(organization.getKey());
		for (Entry<UUID, String> application : applications.entrySet()) {

			logger.info(application.getValue() + " : " + application.getKey());

			// Get the JSon serializer.
			JsonGenerator jg = getJsonGenerator(createOutputFile("application",
					application.getValue()));

			EntityManager em = emf.getEntityManager(application.getKey());

			// Write application
			Entity nsEntity = em.get(application.getKey());
			nsEntity.setMetadata("organization", organization);
			jg.writeStartArray();
			jg.writeObject(nsEntity);

			// Create a generator for the application collections.
			JsonGenerator collectionsJg = getJsonGenerator(createOutputFile(
					"collections", application.getValue()));
			collectionsJg.writeStartObject();

			Map<String, Object> metadata = em
					.getApplicationCollectionMetadata();
			echo(JsonUtils.mapToFormattedJsonString(metadata));

			// Loop through the collections. This is the only way to loop
			// through the entities in the application (former namespace).
			for (String collectionName : metadata.keySet()) {
				Results r = em.getCollection(em.getApplicationRef(),
						collectionName, null, 100000, Results.Level.IDS, false);

				echo(r.size() + " entity ids loaded");
				int size = r.size();

				for (int i = 0; i < size; i += MAX_ENTITY_FETCH) {

					// Batch the read to avoid big amount of data
					int finish = Math.min(i + MAX_ENTITY_FETCH, size);
					List<UUID> entityIds = r.getIds().subList(i, finish);

					logger.info("Retrieving entities " + i + " through "
							+ (finish - 1) + " Found:" + entityIds.size());

					Results entities = em.get(entityIds,
							Results.Level.ALL_PROPERTIES);

					for (Entity entity : entities) {
						// Export the entity first and later the collections for
						// this entity.
						jg.writeObject(entity);
						echo(entity);

						saveCollectionMembers(collectionsJg, em,
								application.getValue(), entity);
					}
				}
			}

			// Close writer for the collections for this application.
			collectionsJg.writeEndObject();
			collectionsJg.close();

			// Close writer and file for this application.
			jg.writeEndArray();
			jg.close();
		}

	}

	/**
	 * Serialize and save the collection members of this <code>entity</code>
	 * 
	 * @param collectionsFile
	 * @param em
	 *            Entity Manager
	 * @param application
	 *            Application name
	 * @param entity
	 *            entity
	 * @throws Exception
	 */
	private void saveCollectionMembers(JsonGenerator jg, EntityManager em,
			String application, Entity entity) throws Exception {

		Set<String> collections = em.getCollections(entity);

		// Only create entry for Entities that have collections
		if ((collections == null) || collections.isEmpty()) {
			return;
		}

		jg.writeFieldName(entity.getUuid().toString());
		jg.writeStartObject();

		for (String collectionName : collections) {

			jg.writeFieldName(collectionName);
			// Start collection array.
			jg.writeStartArray();

			Results collectionMembers = em.getCollection(entity,
					collectionName, null, 100000, Level.IDS, false);

			List<UUID> entityIds = collectionMembers.getIds();

			if ((entityIds != null) && !entityIds.isEmpty()) {
				for (UUID childEntityUUID : entityIds) {
					jg.writeObject(childEntityUUID.toString());
				}
			}

			// End collection array.
			jg.writeEndArray();
		}

		// Write collections
		if ((collections != null) && !collections.isEmpty()) {
			saveConnections(entity, em, jg);
		}

		// End the object if it was Started
		jg.writeEndObject();
	}

	/**
	 * Persists the connection for this entity.
	 */
	private void saveConnections(Entity entity, EntityManager em,
			JsonGenerator jg) throws Exception {

		jg.writeFieldName("connections");
		jg.writeStartObject();

		Set<String> connectionTypes = em.getConnectionTypes(entity);
		for (String connectionType : connectionTypes) {

			jg.writeFieldName(connectionType);
			jg.writeStartArray();

			Results results = em.getConnectedEntities(entity.getUuid(),
					connectionType, null, Level.IDS);
			List<ConnectionRef> connections = results.getConnections();

			for (ConnectionRef connectionRef : connections) {
				jg.writeObject(connectionRef.getConnectedEntity().getUuid());
			}

			jg.writeEndArray();
		}
		jg.writeEndObject();
	}

	/*-
	 * Set<String> collections = em.getCollections(entity);
	 * for (String collection : collections) {
	 *   Results collectionMembers = em.getCollection(
	 *    entity, collection, null,
	 *    MAX_ENTITY_FETCH, Level.IDS, false);
	 *    write entity_id : { "collectionName" : [ids]
	 *  }
	 * }
	 * 
	 * 
	 *   {
	 *     entity_id :
	 *       { collection_name :
	 *         [
	 *           collected_entity_id,
	 *           collected_entity_id
	 *         ]
	 *       },
	 *     f47ac10b-58cc-4372-a567-0e02b2c3d479 :
	 *       { "activtites" :
	 *         [
	 *           f47ac10b-58cc-4372-a567-0e02b2c3d47A,
	 *           f47ac10b-58cc-4372-a567-0e02b2c3d47B
	 *         ]
	 *       }
	 *   }
	 * 
	 * http://jackson.codehaus.org/1.8.0/javadoc/org/codehaus/jackson/JsonGenerator.html
	 * 
	 *
	 *-
	 * List<ConnectedEntityRef> connections = em.getConnections(entityId, query);
	 */

	private void prepareBaseOutputFileName(CommandLine line) {

		boolean hasOutputDir = line.hasOption(OUTPUT_DIR);

		if (hasOutputDir) {
			baseOutputDirName = line.getOptionValue(OUTPUT_DIR);
		}
	}

	private void exportOrganizations() throws Exception,
			UnsupportedEncodingException {
		// Loop through the organizations
		BiMap<UUID, String> organizationNames = managementService
				.getOrganizations();
		for (Entry<UUID, String> organizationName : organizationNames
				.entrySet()) {

			// Let's skip the test entities.
			if (organizationName.equals(properties
					.getProperty("usergrid.test-account.organization"))) {
				continue;
			}

			OrganizationInfo acc = managementService
					.getOrganizationByUuid(organizationName.getKey());
			logger.info("Exporting Organization: " + acc.getName());

			// One file per Organization.
			saveOrganizationInFile(acc);
		}

	}

	/**
	 * Serialize an Organization into a json file.
	 * 
	 * @param acc
	 *            OrganizationInfo
	 */
	private void saveOrganizationInFile(OrganizationInfo acc) {
		try {
			File outFile = createOutputFile("organization", acc.getName());
			JsonGenerator jg = getJsonGenerator(outFile);
			jg.writeObject(acc);
			jg.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private JsonGenerator getJsonGenerator(File outFile) throws IOException {
		PrintWriter out = new PrintWriter(outFile, "UTF-8");
		JsonGenerator jg = jsonFactory.createJsonGenerator(out);
		jg.setPrettyPrinter(new DefaultPrettyPrinter());
		jg.setCodec(new ObjectMapper());
		return jg;

	}

	private void setVerbose(CommandLine line) {
		if (line.hasOption(VERBOSE)) {
			isVerboseEnabled = true;
		}
	}

	/**
	 * Write the string onto the writer and check if verbose is enabled to log
	 * also an echo of what is being written to the writer.
	 * 
	 * @param out
	 *            PrintWriter
	 * @param content
	 *            string to be written
	 */
	@SuppressWarnings("unused")
	private void writeOutput(PrintWriter out, String content) {
		echo(content);
		out.print(content);

	}

	/**
	 * Log the content in the default logger(info)
	 * 
	 * @param content
	 */
	private void echo(String content) {
		if (isVerboseEnabled) {
			logger.info(content);
		}
	}

	/**
	 * Print the object in JSon format.
	 * 
	 * @param obj
	 */
	private void echo(Object obj) {
		echo(JsonUtils.mapToFormattedJsonString(obj));
	}

	private File createOutputParentDir() {
		return createDir(baseOutputDirName);
	}

	private File createOutputFile(String type, String name) {
		return new File(exportDir, prepareOutputFileName(type, name));
	}

	@SuppressWarnings("unused")
	private File createOutputFile(File parent, String type, String name) {
		return new File(parent, prepareOutputFileName(type, name));
	}

	@SuppressWarnings("unused")
	private File createCollectionsDir(String applicationName) {
		return createDir(exportDir, "application." + applicationName
				+ ".collections");
	}

	private File createDir(String dirName) {
		return createDir(null, dirName);
	}

	private File createDir(File parent, String dirName) {
		File file = null;

		if (parent == null) {
			file = new File(dirName);
		} else {
			file = new File(parent, dirName);
		}

		boolean wasCreated = false;

		if (file.exists() && file.isDirectory()) {
			wasCreated = true;
		} else {
			wasCreated = file.mkdir();
		}

		if (!wasCreated) {
			throw new RuntimeException("Unable to create directory:" + dirName);
		}

		return file;
	}

	/**
	 * 
	 * @param type
	 *            just a label such us: organization, application.
	 * @param name
	 * @return the file name concatenated with the type and the name of the
	 *         collection
	 */
	private String prepareOutputFileName(String type, String name) {
		// Add application and timestamp
		StringBuilder str = new StringBuilder();
		// str.append(baseOutputFileName);
		// str.append(".");
		str.append(type);
		str.append(".");
		str.append(name);
		str.append(".");
		str.append(System.currentTimeMillis());
		str.append(".json");

		String outputFileName = str.toString();

		logger.info("Creating output filename:" + outputFileName);

		return outputFileName;
	}

	public void streamOutput(File file, List<Entity> entities) throws Exception {
		JsonFactory jsonFactory = new JsonFactory();
		// or, for data binding,
		// org.codehaus.jackson.mapper.MappingJsonFactory
		JsonGenerator jg = jsonFactory.createJsonGenerator(file,
				JsonEncoding.UTF8);
		// or Stream, Reader

		jg.writeStartArray();
		for (Entity entity : entities) {
			jg.writeObject(entity);

		}
		jg.writeEndArray();

		jg.close();
	}

	// to generate the activities and user relationship, follow this:

	// write field name (id)
	// write start object
	// write field name (collection name)
	// write start array
	// write object/string
	// write another object
	// write end array
	// write end object
	// ...... more objects
	//

}
