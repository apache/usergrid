/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence;

import java.util.Properties;
import java.util.UUID;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.cassandra.SetupImpl;


public class HybridSetup implements Setup {

    private final Setup setup;

    public HybridSetup(Properties props, EntityManagerFactory emf, CassandraService cass ) {
        
        boolean useCP = cass.getPropertiesMap().get("usergrid.persistence").equals("CP");
        if ( useCP ) {
            setup = new CpSetup( emf, cass);
        } else {
            setup = new SetupImpl( emf, cass );
        }

    }

    @Override
    public void init() throws Exception {
        setup.init();
    }

    @Override
    public void setupSystemKeyspace() throws Exception {
        setup.setupSystemKeyspace();
    }

    @Override
    public void setupStaticKeyspace() throws Exception {
        setup.setupStaticKeyspace();
    }

    @Override
    public boolean keyspacesExist() {
        return setup.keyspacesExist();
    }

    @Override
    public void createDefaultApplications() throws Exception {
        setup.createDefaultApplications();
    }

    @Override
    public void setupApplicationKeyspace(UUID applicationId, String appName) throws Exception {
        setup.setupApplicationKeyspace(applicationId, appName);
    }
    
}
