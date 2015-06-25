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


import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.core.guice.CommonModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.netflix.config.ConfigurationManager;

import me.prettyprint.cassandra.service.CassandraHost;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;


/**
 * Factory for configuring Guice then returning it
 */
@Singleton
public class GuiceFactory implements FactoryBean<Injector>, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger( GuiceFactory.class );

    private final CassandraHostConfigurator chc;

    private final Properties systemProperties;


    private ApplicationContext applicationContext;

    private Injector injector;



    public GuiceFactory( final CassandraHostConfigurator chc, final Properties systemProperties  ) {
        this.chc = chc;
        this.systemProperties = systemProperties;
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
            String sep = "";
            for ( CassandraHost host : hosts ) {
                if ( StringUtils.isEmpty( host.getHost() ) ) {
                    throw new RuntimeException( "Fatal error: Cassandra hostname cannot be empty" );
                }
                hostsString = hostsString + sep + host.getHost();
                sep = ",";
            }

            logger.info( "hostsString: " + hostsString );

            Properties cpProps = new Properties();

            // Some Usergrid properties must be mapped to Core Persistence properties
            cpProps.put( "cassandra.hosts", hostsString );
            cpProps.put( "cassandra.port", hosts[0].getPort() );
            cpProps.put( "cassandra.cluster_name",  systemProperties.getProperty( "cassandra.cluster" ) );

            String cassRemoteString = ( String ) systemProperties.getProperty( "cassandra.use_remote" );
            if ( cassRemoteString != null && cassRemoteString.equals( "false" ) ) {
                cpProps.put( "cassandra.embedded", "true" );
            }
            else {
                cpProps.put( "cassandra.embedded", "false" );
            }

            cpProps.put( "collections.keyspace.strategy.class",
                systemProperties.getProperty( "cassandra.keyspace.strategy" ) );

            cpProps.put( "collections.keyspace.strategy.options",
                systemProperties.getProperty( "cassandra.keyspace.replication" ) );

            logger.debug( "Set Cassandra properties for Core Persistence: " + cpProps.toString() );

            // Make all Usergrid properties into Core Persistence config
            cpProps.putAll( systemProperties );
            //logger.debug("All properties fed to Core Persistence: " + cpProps.toString() );

            ConfigurationManager.loadProperties( cpProps );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Fatal error loading configuration.", e );
        }

        //this is seriously fugly, and needs removed
        injector = Guice.createInjector( new CoreModule( applicationContext ) );

        return injector;
    }


    @Override
    public Class<?> getObjectType() {
        return Injector.class;
    }


    @Override
    public boolean isSingleton() {
        return true;
    }


    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
