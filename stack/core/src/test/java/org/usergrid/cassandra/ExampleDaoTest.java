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
