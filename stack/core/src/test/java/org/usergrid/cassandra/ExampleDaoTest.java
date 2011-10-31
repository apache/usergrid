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
package org.usergrid.cassandra;

import static me.prettyprint.hector.api.factory.HFactory.createKeyspace;
import static me.prettyprint.hector.api.factory.HFactory.getOrCreateCluster;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import me.prettyprint.cassandra.examples.ExampleDaoV2;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.testutils.EmbeddedServerHelper;

import org.apache.cassandra.config.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExampleDaoTest {

	private static EmbeddedServerHelper embedded;

	Serializer<String> se = StringSerializer.get();

	/**
	 * Set embedded cassandra up and spawn it in a new thread.
	 * 
	 * @throws TTransportException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ConfigurationException
	 */
	@BeforeClass
	/* (groups="db") */
	public static void setup() throws TTransportException, IOException,
			InterruptedException, ConfigurationException {
		embedded = new EmbeddedServerHelper();
		embedded.setup();
	}

	@AfterClass
	/* (groups="db") */
	public static void teardown() throws IOException {
		EmbeddedServerHelper.teardown();
	}

	@Test
	/* (groups="db") */
	public void testInsertGetDelete() throws Exception {
		Cluster c = getOrCreateCluster("MyCluster", "localhost:9160");
		ExampleDaoV2 dao = new ExampleDaoV2(createKeyspace("Keyspace1", c));
		assertNull(dao.get("key", StringSerializer.get()));
		dao.insert("key", "value", StringSerializer.get());
		assertEquals("value", dao.get("key", StringSerializer.get()));
		dao.delete(StringSerializer.get(), "key");
		assertNull(dao.get("key", StringSerializer.get()));
	}

}
