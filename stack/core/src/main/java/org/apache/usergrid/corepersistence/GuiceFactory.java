/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Named;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.persistence.PersistenceModule;

import com.google.common.base.Preconditions;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;

import me.prettyprint.cassandra.service.CassandraHost;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;


/**
 * Factory for configuring Guice then returning it
 */
@Component
public class GuiceFactory implements FactoryBean<Injector> {

    private static final Logger logger = LoggerFactory.getLogger( GuiceFactory.class );

    @Autowired
    private CassandraHostConfigurator chc;

    @Autowired
    @Named("properties")
    private Properties systemProperties;

    @Autowired
    private ApplicationContext applicationContext;

    private Injector injector;


    public GuiceFactory() {
        //        this.chc = chc;
        //        this.systemProperties = systemProperties;
        //        this.applicationContext = applicationContext;
    }


    @Override
    public Injector getObject() throws Exception {

        if ( this.injector != null ) {
            return injector;
        }

        try {

            logger.info( "Loading Core Persistence properties" );

            String hostsString = "";
            CassandraHost[] hosts = chc.buildCassandraHosts();
            if ( hosts.length == 0 ) {
                throw new RuntimeException( "Fatal error: no Cassandra hosts configured" );
            }

            for ( CassandraHost host : hosts ) {
                if ( StringUtils.isEmpty( host.getHost() ) ) {
                    throw new RuntimeException( "Fatal error: Cassandra hostname cannot be empty" );
                }
                hostsString += host.getHost() + ",";
            }

            hostsString = hostsString.substring( 0, hostsString.length() - 1 );

            logger.info( "hostsString: " + hostsString );

            Properties cpProps = new Properties();

            // Some Usergrid properties must be mapped to Core Persistence properties
            cpProps.put( "cassandra.hosts", hostsString );
            cpProps.put( "cassandra.port", hosts[0].getPort() );

            cpProps.put( "cassandra.cluster_name", getAndValidateProperty( "cassandra.cluster" ) );

            cpProps
                .put( "collections.keyspace.strategy.class", getAndValidateProperty( "cassandra.keyspace.strategy" ) );

            cpProps.put( "collections.keyspace.strategy.options",
                getAndValidateProperty( "cassandra.keyspace.replication" ) );

            logger.debug( "Set Cassandra properties for Core Persistence: " + cpProps.toString() );

            // Make all Usergrid properties into Core Persistence config
            cpProps.putAll( systemProperties );
            //logger.debug("All properties fed to Core Persistence: " + cpProps.toString() );

            ConfigurationManager.loadProperties( cpProps );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Fatal error loading configuration.", e );
        }

        List<Module> moduleList = new ArrayList<>();
        if(applicationContext.containsBean("serviceModule")){
            Module serviceModule =(Module)applicationContext.getBean("serviceModule");
            moduleList.add( serviceModule);
        }
        moduleList.add(new CoreModule());
        moduleList.add(new PersistenceModule(applicationContext));
        //we have to inject a couple of spring beans into our Guice.  Wire it with PersistenceModule
        injector = Guice.createInjector( moduleList );

        return injector;
    }


    private String getAndValidateProperty( final String propName ) {

        final String propValue = systemProperties.getProperty( propName );

        Preconditions.checkNotNull( propValue, propName + " cannot be unset. Set this in your properties" );

        return propValue;
    }


    @Override
    public Class<?> getObjectType() {
        return Injector.class;
    }


    @Override
    public boolean isSingleton() {
        return true;
    }
}
