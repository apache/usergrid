/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest;


import java.net.URI;
import java.util.Properties;

import javax.ws.rs.core.UriBuilder;

import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;


/** A {@link org.junit.rules.TestRule} that sets up services. */
public class ITSetup  {

    private static ITSetup instance;

    private EntityManagerFactory emf;
    private Properties properties;
    private ManagementService managementService;


    private SpringResource springResource;


    private ITSetup( ) {

        this.springResource = ConcurrentProcessSingleton.getInstance().getSpringResource();

        emf =                springResource.getBean( EntityManagerFactory.class );
        managementService =  springResource.getBean( ManagementService.class );
        properties = springResource.getBean( "properties", Properties.class );


    }

    public static synchronized ITSetup getInstance(){
        if(instance == null){
            instance = new ITSetup();
        }

        return instance;
    }



    public ManagementService getMgmtSvc() {
        return managementService;
    }


    public EntityManagerFactory getEmf() {
        return emf;
    }

    public Properties getProps() {
        return properties;
    }

}
