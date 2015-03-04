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
package org.apache.usergrid.persistence.cassandra;


import java.util.UUID;
import me.prettyprint.hector.api.ddl.ComparatorType;
import static me.prettyprint.hector.api.factory.HFactory.createColumnFamilyDefinition;
import org.apache.usergrid.mq.cassandra.QueuesCF;
import org.apache.usergrid.persistence.EntityManagerFactory;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.getCfDefs;
import static org.apache.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_ORGANIZATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PRINCIPAL_TOKEN_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PROPERTIES_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.TOKENS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.getApplicationKeyspace;
import static org.apache.usergrid.persistence.cassandra.CassandraService.keyspaceForApplication;
import org.apache.usergrid.persistence.entities.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cassandra-specific setup utilities.
 *
 * @author edanuff
 */
public class SetupImpl implements Setup {

    private static final Logger logger = LoggerFactory.getLogger( SetupImpl.class );

    private final org.apache.usergrid.persistence.EntityManagerFactory emf;
    private final CassandraService cass;


    public SetupImpl( EntityManagerFactory emf, CassandraService cass ) {
        this.emf = emf;
        this.cass = cass;
    }


    public synchronized void init() throws Exception {
        cass.init();
    }


    public void createDefaultApplications() throws Exception {
        // TODO unique check?
        emf.initializeApplication( DEFAULT_ORGANIZATION, emf.getManagementAppId(), MANAGEMENT_APPLICATION, null );
    }


    /**
     * Initialize system keyspace.
     *
     * @throws Exception the exception
     */
    public void setupSystemKeyspace() throws Exception {

        logger.info( "Initialize system keyspace" );

        cass.createColumnFamily( getApplicationKeyspace(), createColumnFamilyDefinition(
                getApplicationKeyspace(), APPLICATIONS_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( getApplicationKeyspace(), createColumnFamilyDefinition(
                getApplicationKeyspace(), PROPERTIES_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( getApplicationKeyspace(), createColumnFamilyDefinition(
                getApplicationKeyspace(), TOKENS_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( getApplicationKeyspace(), createColumnFamilyDefinition(
                getApplicationKeyspace(), PRINCIPAL_TOKEN_CF, ComparatorType.UUIDTYPE ) );

        logger.info( "System keyspace initialized" );
    }


    /**
     * Initialize application keyspace.
     *
     * @param applicationId the application id
     * @param applicationName the application name
     *
     * @throws Exception the exception
     */
    @Override
    public void setupApplicationKeyspace(
            final UUID applicationId, String applicationName ) throws Exception {
        logger.info("This method no longer needed due to using virtual keyspaces all the time.");
    }


    public void setupStaticKeyspace() throws Exception {


        logger.info( "Creating static application keyspace " + getApplicationKeyspace() );

        cass.createColumnFamily( getApplicationKeyspace(),
                createColumnFamilyDefinition( getApplicationKeyspace(), APPLICATIONS_CF,
                        ComparatorType.BYTESTYPE ) );

        cass.createColumnFamilies( getApplicationKeyspace(),
                getCfDefs( ApplicationCF.class, getApplicationKeyspace() ) );
        cass.createColumnFamilies( getApplicationKeyspace(),
                getCfDefs( QueuesCF.class, getApplicationKeyspace() ) );
    }


    public boolean keyspacesExist() {
        return cass.checkKeyspacesExist();
    }


    public static void logCFPermissions() {
        System.out.println( getApplicationKeyspace() + "." + APPLICATIONS_CF + ".<rw>=usergrid" );
        System.out.println( getApplicationKeyspace() + "." + PROPERTIES_CF + ".<rw>=usergrid" );
        for ( CFEnum cf : ApplicationCF.values() ) {
            System.out.println( getApplicationKeyspace() + "." + cf + ".<rw>=usergrid" );
        }
        for ( CFEnum cf : QueuesCF.values() ) {
            System.out.println( getApplicationKeyspace() + "." + cf + ".<rw>=usergrid" );
        }
    }


    /** @return staticly constructed reference to the management application */
    public static Application getManagementApp() {
        return SystemDefaults.managementApp;
    }


    /** @return statically constructed reference to the default application */
    public static Application getDefaultApp() {
        return SystemDefaults.defaultApp;
    }


    static class SystemDefaults {

        private static final Application managementApp =
                new Application( EntityManagerFactoryImpl.MANAGEMENT_APPLICATION_ID);

        private static final Application defaultApp =
                new Application( EntityManagerFactoryImpl.DEFAULT_APPLICATION_ID);

        static {
            managementApp.setName( MANAGEMENT_APPLICATION );
            defaultApp.setName( DEFAULT_APPLICATION );
        }
    }
}
