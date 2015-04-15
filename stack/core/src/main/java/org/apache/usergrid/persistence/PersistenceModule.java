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

package org.apache.usergrid.persistence;


import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Properties;

import org.springframework.context.ApplicationContext;

import org.apache.usergrid.locking.cassandra.HectorLockManagerImpl;

import com.google.common.base.Preconditions;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.spring.SpringIntegration;

import me.prettyprint.cassandra.connection.RoundRobinBalancingPolicy;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCluster;
import me.prettyprint.hector.api.Cluster;


/**
 * Replacement for configuration of our spring modules with guice
 */
public class PersistenceModule extends AbstractModule {



    private final ApplicationContext applicationContext;

    public PersistenceModule( final ApplicationContext applicationContext ) {

        this.applicationContext = applicationContext;
    }




    @Override
    protected void configure() {
        SpringIntegration.bindAll( binder(), applicationContext );
    }



//    <bean id="cassandraCluster" class="me.prettyprint.cassandra.service.ThriftCluster">
//   		<constructor-arg value="${cassandra.cluster}" />
//   		<constructor-arg ref="cassandraHostConfigurator" />
//   	</bean>
//    @Provides
//    @Singleton
//    @Inject
//    public Cluster configureThrift( @Named( "cassandra.cluster" ) final String cassCluster,
//                                          @Named( "cassandra.connections" ) final int cassandraConnections ){
//
//        final int setSize = cassandraConnections == 0 ? 50: cassandraConnections;
//
//        CassandraHostConfigurator hostConfigurator = new CassandraHostConfigurator( cassCluster );
//
//        hostConfigurator.setMaxActive( setSize );
//        hostConfigurator.setLoadBalancingPolicy( new RoundRobinBalancingPolicy() );
//
//
//        ThriftCluster thriftCluster = new ThriftCluster(cassCluster, hostConfigurator);
//
//        return thriftCluster;
//
//    }
//
//
//    @Provides
//    @Singleton
//    @Inject
//    public Properties configureProps(final PropertiesProvider propertiesProvider ){
//
//        final Properties props = new Properties(  );
//
//        for(final String propFile: propertiesProvider.getPropertiesFiles()){
//
//            final URL url = Resources.getResource( propFile );
//
//            Preconditions.checkNotNull( url, "Could not find properties file '" + propFile + "' on the classpath" );
//
//
//            final CharSource propsInput = Resources.asCharSource( url, Charset.defaultCharset() );
//            try {
//                props.load( propsInput.openStream() );
//            }
//            catch ( IOException e ) {
//                throw new RuntimeException( "Unable to load properties file '" + propFile + "'", e );
//            }
//        }
//
//        //bind these properties
//        Names.bindProperties( binder(), props );
//
//        return props;
//    }
//
//    @Provides
//    @Singleton
//    @Inject
//    public void configureLocks(final Cluster hectorCluster, @Named("cassandra.lock.keyspace") final String lockKeyspace, @Named("cassandra.lock.keyspace") final String writeCl, final String readCl ){
//
//
//        final HectorLockManagerImpl hectorLockManager = new HectorLockManagerImpl();
//
//
////
////        <bean name="consistencyLevelPolicy" class="me.prettyprint.cassandra.model.ConfigurableConsistencyLevel">
////               <property name="defaultReadConsistencyLevel" value="${cassandra.readcl}"/>
////               <property name="defaultWriteConsistencyLevel" value="${cassandra.writecl}"/>
////           </bean>
//
////        <bean name="lockManager" class="org.apache.usergrid.locking.cassandra.HectorLockManagerImpl" >
////       		<property name="cluster" ref="cassandraCluster"/>
////       		<property name="keyspaceName" value="${cassandra.lock.keyspace}"/>
////       		<property name="consistencyLevelPolicy" ref="consistencyLevelPolicy"/>
////       	</bean>
//
//    }
//
//
//    /**
//     * Interface to allow users to provide and inject properties
//     */
//    public interface PropertiesProvider{
//        /**
//         * Get the properties files to load
//         * @return
//         */
//        String[] getPropertiesFiles();
//    }
}
