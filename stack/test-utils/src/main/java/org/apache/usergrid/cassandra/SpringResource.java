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
package org.apache.usergrid.cassandra;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;


/**
 * A singleton resource for spring that is used during testing.  This will intialize spring, and then hold on to the
 * spring context within this singleton
 */
public class SpringResource {
    public static final Logger LOG = LoggerFactory.getLogger( SpringResource.class );

    private static SpringResource instance;


    private ConfigurableApplicationContext applicationContext;


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases which uses the specified SchemaManager for
     * Cassandra.
     */
    private SpringResource() {
        LOG.info( "Creating CassandraResource using {} for the ClassLoader.",
            Thread.currentThread().getContextClassLoader() );

        LOG.info( "-------------------------------------------------------------------" );
        LOG.info( "Initializing Spring" );
        LOG.info( "-------------------------------------------------------------------" );


        //wire up cassandra and elasticsearch before we start spring, otherwise this won't work
        new CassandraResource().start();

        new ElasticSearchResource().start();

        final String[] locations = { "usergrid-test-context.xml" };

        this.applicationContext = new ClassPathXmlApplicationContext( locations );
    }


    /**
     * A singleton of this spring resource.  This will instantiate and create
     * the spring context if an instance is not present.
     */
    public static synchronized SpringResource getInstance() {
        if ( instance == null ) {
            instance = new SpringResource();
        }

        return instance;
    }


    /**
     * Gets a bean from the application context.
     *
     * @param requiredType the type of the bean
     * @param <T> the type of the bean
     *
     * @return the bean
     */
    public <T> T getBean( String name, Class<T> requiredType ) {
        return applicationContext.getBean( name, requiredType );
    }


    /**
     * Use this with care.  You should use getBean in most situations
     * @return
     */
    public ApplicationContext getAppContext(){
        return applicationContext;
    }

    /**
     * Gets a bean from the application context.
     *
     * @param requiredType the type of the bean
     * @param <T> the type of the bean
     *
     * @return the bean
     */
    public <T> T getBean( Class<T> requiredType ) {
        return applicationContext.getBean( requiredType );
    }




}
