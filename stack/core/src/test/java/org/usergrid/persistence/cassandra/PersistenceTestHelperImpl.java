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
package org.usergrid.persistence.cassandra;

/*
 import static org.testng.Assert.assertNotNull;
 import static org.testng.Assert.assertNull;
 */

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import me.prettyprint.hector.testutils.EmbeddedServerHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.PersistenceTestHelper;

@Component
public class PersistenceTestHelperImpl implements PersistenceTestHelper {

    public static final boolean FORCE_QUIT = false;

    private static final Logger logger = LoggerFactory
            .getLogger(PersistenceTestHelperImpl.class);

    // private static final String TMP = "tmp";

    // DatastoreTestClient client;

    EntityManagerFactory emf;
    QueueManagerFactory mmf;
    Properties properties;
    CassandraService cassandraService;

    public boolean forceQuit = FORCE_QUIT;

    public PersistenceTestHelperImpl() {

    }

    /*
     * public DatastoreTestHelperImpl(DatastoreTestClient client) { this.client
     * = client; }
     * 
     * public DatastoreTestClient getClient() { return client; }
     * 
     * public void setClient(DatastoreTestClient client) {
     * assertNull(this.client); this.client = client; }
     */

    ClassPathXmlApplicationContext ac = null;


    @Override
    public void setup() throws Exception {
        // assertNotNull(client);

        String maven_opts = System.getenv("MAVEN_OPTS");
        logger.info("Maven options: " + maven_opts);



    }

    @Override
    public void teardown() {


    }

    public void forceQuit() {
        if (forceQuit) {
            logger.warn("\n\n\n******\n\nSystem.exit(0) to workaround Cassandra not stopping!\n\n******\n\n\n");
            System.exit(0);
        }
    }



    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }



}
