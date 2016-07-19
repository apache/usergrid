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


import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


/** A {@link org.junit.rules.TestRule} that sets up services. */
public class ITSetup implements TestRule {
    private static final Logger logger = LoggerFactory.getLogger( ITSetup.class );

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

    public static synchronized ITSetup getInstance() {
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

    public SpringResource getSpringResource() {
        return springResource;
    }


    @Override
    public Statement apply( Statement base, Description description ) {
        return statement( base, description );
    }


    private Statement statement( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before( description );

                try {
                    base.evaluate();
                }
                finally {
                    after( description );
                }
            }
        };
    }

    protected void before( Description description ) throws Throwable {
        logger.info( "Setting up for {}", description.getDisplayName() );
    }

    protected void after( Description description ) {
        logger.info( "Tearing down for {}", description.getDisplayName() );
    }

}
