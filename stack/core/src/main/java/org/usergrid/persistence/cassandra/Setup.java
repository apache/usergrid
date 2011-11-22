/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.usergrid.persistence.cassandra;

import static me.prettyprint.hector.api.factory.HFactory.createColumnFamilyDefinition;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.getCfDefs;
import static org.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION;
import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION_ID;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.usergrid.persistence.cassandra.CassandraService.PROPERTIES_CF;
import static org.usergrid.persistence.cassandra.CassandraService.STATIC_APPLICATION_KEYSPACE;
import static org.usergrid.persistence.cassandra.CassandraService.SYSTEM_KEYSPACE;
import static org.usergrid.persistence.cassandra.CassandraService.USE_VIRTUAL_KEYSPACES;
import static org.usergrid.persistence.cassandra.CassandraService.keyspaceForApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.mq.cassandra.QueuesCF;

// TODO: Auto-generated Javadoc
/**
 * Cassandra-specific setup utilities.
 * 
 * @author edanuff
 */
public class Setup {

	private static final Logger logger = LoggerFactory.getLogger(Setup.class);

	private final org.usergrid.persistence.EntityManagerFactory emf;
	private final CassandraService cass;

	/**
	 * Instantiates a new setup object.
	 * 
	 * @param emf
	 *            the emf
	 * @param cass
	 */
	Setup(EntityManagerFactoryImpl emf, CassandraService cass) {
		this.emf = emf;
		this.cass = cass;
	}

	/**
	 * Initialize.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public synchronized void setup() throws Exception {
		cass.init();

		setupSystemKeyspace();

		setupStaticKeyspace();

		((EntityManagerFactoryImpl) emf).initializeApplication(
				DEFAULT_APPLICATION_ID, DEFAULT_APPLICATION, null);

		((EntityManagerFactoryImpl) emf).initializeApplication(
				MANAGEMENT_APPLICATION_ID, MANAGEMENT_APPLICATION, null);
	}

	/**
	 * Initialize system keyspace.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void setupSystemKeyspace() throws Exception {

		logger.info("Initialize system keyspace");

		List<ColumnFamilyDefinition> cf_defs = new ArrayList<ColumnFamilyDefinition>();
		cf_defs.add(createColumnFamilyDefinition(SYSTEM_KEYSPACE,
				APPLICATIONS_CF, ComparatorType.BYTESTYPE));
		cf_defs.add(createColumnFamilyDefinition(SYSTEM_KEYSPACE,
				PROPERTIES_CF, ComparatorType.BYTESTYPE));

		cass.createKeyspace(SYSTEM_KEYSPACE, cf_defs);

		logger.info("System keyspace initialized");
	}

	/**
	 * Initialize application keyspace.
	 * 
	 * @param applicationId
	 *            the application id
	 * @param applicationName
	 *            the application name
	 * @throws Exception
	 *             the exception
	 */
	public void setupApplicationKeyspace(final UUID applicationId,
			String applicationName) throws Exception {

		if (!USE_VIRTUAL_KEYSPACES) {
			String app_keyspace = keyspaceForApplication(applicationId);

			logger.info("Creating application keyspace " + app_keyspace
					+ " for " + applicationName + " application");

			cass.createKeyspace(
					app_keyspace,
					getCfDefs(ApplicationCF.class,
							getCfDefs(QueuesCF.class, app_keyspace),
							app_keyspace));

			/*
			 * String messages_keyspace = app_keyspace +
			 * APPLICATION_MESSAGES_KEYSPACE_SUFFIX;
			 * cass.createKeyspace(messages_keyspace, getCfDefs(QueuesCF.class,
			 * messages_keyspace));
			 */
		}
	}

	public void setupStaticKeyspace() throws Exception {

		if (USE_VIRTUAL_KEYSPACES) {

			logger.info("Creating static application keyspace "
					+ STATIC_APPLICATION_KEYSPACE);

			cass.createKeyspace(
					STATIC_APPLICATION_KEYSPACE,
					getCfDefs(
							ApplicationCF.class,
							getCfDefs(QueuesCF.class,
									STATIC_APPLICATION_KEYSPACE),
							STATIC_APPLICATION_KEYSPACE));

			/*
			 * cass.createKeyspace(STATIC_MESSAGES_KEYSPACE,
			 * getCfDefs(QueuesCF.class, STATIC_MESSAGES_KEYSPACE));
			 */
		}
	}

	public void checkKeyspaces() {
		cass.checkKeyspaces();
	}

	public static void logCFPermissions() {
		System.out.println(SYSTEM_KEYSPACE + "." + APPLICATIONS_CF
				+ ".<rw>=usergrid");
		System.out.println(SYSTEM_KEYSPACE + "." + PROPERTIES_CF
				+ ".<rw>=usergrid");
		for (CFEnum cf : ApplicationCF.values()) {
			System.out.println(STATIC_APPLICATION_KEYSPACE + "." + cf
					+ ".<rw>=usergrid");
		}
		for (CFEnum cf : QueuesCF.values()) {
			System.out.println(STATIC_APPLICATION_KEYSPACE + "." + cf
					+ ".<rw>=usergrid");
		}
	}

}
