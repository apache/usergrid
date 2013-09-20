/*
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
 */
package org.usergrid;

import org.junit.rules.TestRule;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.cassandra.CassandraService;

import java.util.UUID;

public interface CoreITSetup extends TestRule {

  boolean USE_DEFAULT_APPLICATION = false;

  EntityManagerFactory getEmf();

  QueueManagerFactory getQmf();

  IndexBucketLocator getIbl();

  CassandraService getCassSvc();

  UUID createApplication(String organizationName, String applicationName) throws Exception;

  void dump(String name, Object obj);
}
