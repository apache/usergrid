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
package org.usergrid.management.cassandra;

/*******************************************************************************
 * Copyright 2010,2011 Ed Anuff and Usergrid, all rights reserved.
 ******************************************************************************/

/*
 import static org.testng.Assert.assertNotNull;
 import static org.testng.Assert.assertNull;
 */

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.usergrid.management.ManagementService;
import org.usergrid.management.ManagementTestHelper;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.security.tokens.TokenService;

@Component
public class ManagementTestHelperImpl implements ManagementTestHelper {

    public static final boolean FORCE_QUIT = false;

    private static final Logger logger = LoggerFactory
            .getLogger(ManagementTestHelperImpl.class);

    // private static final String TMP = "tmp";

    // DatastoreTestClient client;

    EntityManagerFactory emf;

    javax.persistence.EntityManagerFactory jpaEmf;

    ManagementService management;

    TokenService tokens;

    Properties properties;

    public boolean forceQuit = FORCE_QUIT;

    public ManagementTestHelperImpl() {
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




    @Override
    public void setup() throws Exception {
        // assertNotNull(client);

        String maven_opts = System.getenv("MAVEN_OPTS");
        logger.info("Maven options: " + maven_opts);


    }

    @Override
    public void teardown() {

    }


    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }



    @Override
    public ManagementService getManagementService() {
        return management;
    }



    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public TokenService getTokenService() {
        return tokens;
    }



}
