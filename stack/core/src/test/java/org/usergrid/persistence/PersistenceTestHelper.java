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
package org.usergrid.persistence;

import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.cassandra.CassandraService;

public interface PersistenceTestHelper {

	public abstract EntityManagerFactory getEntityManagerFactory();

	public abstract void setEntityManagerFactory(EntityManagerFactory emf);

	public abstract QueueManagerFactory getMessageManagerFactory();

	public abstract void setMessageManagerFactory(QueueManagerFactory mmf);

	public abstract Properties getProperties();

	public abstract void setProperties(Properties properties);

	public abstract void setup() throws Exception;

	public abstract void teardown() throws Exception;

	public abstract void setCassandraService(CassandraService cassandraService);

	public abstract CassandraService getCassandraService();

	public ApplicationContext getApplicationContext();

}
