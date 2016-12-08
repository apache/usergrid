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
package org.apache.usergrid.tools;


import com.google.inject.Injector;
import me.prettyprint.hector.testutils.EmbeddedServerHelper;
import org.apache.commons.cli.*;
import org.apache.commons.lang.ClassUtils;
import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Properties;

import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Base class for Usergrid Tools commands. Any class that implements this can be called with
 * java -jar {jarname} org.apache.usergrid.tools.{classname}.
 */
public abstract class ToolBase {

    public static final int MAX_ENTITY_FETCH = 100;

    /** Verbose option: -v */
    static final String VERBOSE = "v";

    boolean isVerboseEnabled = false;

    static final Logger logger = LoggerFactory.getLogger( ToolBase.class );

    protected static final String PATH_REPLACEMENT = "-";

    protected EmbeddedServerHelper embedded = null;

    protected EntityManagerFactory emf;

    protected ServiceManagerFactory smf;

    protected ManagementService managementService;

    protected Properties properties;

    protected CassandraService cass;

    protected Injector injector;




    public void startTool( String[] args ) {
        startTool( args, true );
    }

    public void startTool( String[] args, boolean exit ) {
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        try {
            line = parser.parse( createOptions(), args );
        }
        catch ( ParseException exp ) {
            printCliHelp( "Parsing failed.  Reason: " + exp.getMessage() );
        }

        if ( line == null ) {
            return;
        }

        // notification queue listener not needed for tools
        System.setProperty("usergrid.notifications.listener.run", "false");
        System.setProperty("usergrid.push.worker_count", "0");

        //job scheduler also not needed for tools
        System.setProperty("usergrid.scheduler.enabled", "false");
        System.setProperty("usergrid.cluster.enabled", "false");




        if ( line.hasOption( "host" ) ) {
            System.setProperty( "cassandra.url", line.getOptionValue( "host" ) );
            System.setProperty( "elasticsearch.hosts", line.getOptionValue( "eshost" ) );
            System.setProperty( "elasticsearch.cluster_name", line.getOptionValue( "escluster" ) );
            System.setProperty( "usergrid.cluster_name", line.getOptionValue( "ugcluster" )  );
        }

        try {
            runTool( line );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        if ( exit ) {
            System.exit( 0 );
        }
    }


    public void printCliHelp( String message ) {
        System.out.println( message );
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "java -jar usergrid-tools.jar " + getToolName(), createOptions() );
        System.exit( -1 );
    }


    public String getToolName() {
        return ClassUtils.getShortClassName( this.getClass() );
    }


    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption = OptionBuilder.withArgName( "host" ).hasArg()
            .withDescription( "Cassandra host" ).create( "host" );

        Option esHostOption = OptionBuilder.withArgName( "eshost" ).hasArg()
            .withDescription( "ElasticSearch host" ).create( "eshost" );

        Option esClusterOption = OptionBuilder.withArgName( "escluster" ).hasArg()
            .withDescription( "ElasticSearch cluster name" ).create( "escluster" );

        Option ugClusterOption = OptionBuilder.withArgName( "ugcluster" ).hasArg()
            .withDescription( "Usergrid cluster name" ).create( "ugcluster" );

        Option remoteOption = OptionBuilder
            .withDescription( "Use remote Cassandra instance" ).create( "remote" );

        Option verbose = OptionBuilder
            .withDescription( "Print on the console an echo of the content written to the file" )
            .create( VERBOSE );

        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( esHostOption );
        options.addOption( esClusterOption );
        options.addOption( ugClusterOption );
        options.addOption( remoteOption );
        options.addOption( verbose );

        return options;
    }


    public void startEmbedded() throws Exception {
        // assertNotNull(client);

        String maven_opts = System.getenv( "MAVEN_OPTS" );
        logger.info( "Maven options: " + maven_opts );

        logger.info( "Starting Cassandra" );
        embedded = new EmbeddedServerHelper();
        embedded.setup();
    }


    public void startSpring() {

        String[] locations = { "toolsApplicationContext.xml" };
        ApplicationContext ac = new ClassPathXmlApplicationContext( locations );

        AutowireCapableBeanFactory acbf = ac.getAutowireCapableBeanFactory();
        acbf.autowireBeanProperties( this, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false );
        acbf.initializeBean( this, "testClient" );

        assertNotNull( emf );
        assertTrue( "EntityManagerFactory is instance of EntityManagerFactory",
                emf instanceof EntityManagerFactory );

        injector = ac.getBean( Injector.class );


    }


    public void setupCassandra() throws Exception {

        Setup setup = ( (CpEntityManagerFactory) emf ).getSetup();
        logger.info( "Setting up Usergrid schema" );
        setup.initSchema(false);
        setup.initMgmtApp();
        logger.info( "Usergrid schema setup" );

        logger.info( "Setting up Usergrid management services" );

        managementService.setup();

        logger.info( "Usergrid management services setup" );
    }


    public void teardownEmbedded() {
        logger.info( "Stopping Cassandra" );
        EmbeddedServerHelper.teardown();
    }


    void setVerbose( CommandLine line ) {
        if ( line.hasOption( VERBOSE ) ) {
            isVerboseEnabled = true;
        }
    }


    /** Log the content in the default logger(info) */
    void echo( String content ) {
        if ( isVerboseEnabled ) {
            logger.info( content );
        }
    }


    /** Print the object in JSon format. */
    void echo( Object obj ) {
        echo( mapToFormattedJsonString( obj ) );
    }


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    @Autowired
    public void setServiceManagerFactory( ServiceManagerFactory smf ) {
        this.smf = smf;
        logger.info( "ManagementResource.setServiceManagerFactory" );
    }


    @Autowired
    public void setManagementService( ManagementService managementService ) {
        this.managementService = managementService;
    }


    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;

        logger.info( "Properties set: \n" +
            "   cassandra.url: {}\n" +
            "   cassandra.datacenter.local: {}\n" +
            "   cassandra.username: {}\n" +
            "   cassandra.password: {}\n" +
            "   cassandra.keyspace.strategy: {}\n" +
            "   cassandra.keyspace.application: {}\n" +
            "   cassandra.lock.keyspace: {}\n" +
            "   cassandra.keyspace.replication: {}\n" +
            "   cassandra.connections: {}\n" +
            "   usergrid.notifications.listener.run: {}\n" +
            "   usergrid.push.worker_count: {}\n" +
            "   usergrid.scheduler.enabled: {}\n",
            properties.get("cassandra.url"),
            properties.get("cassandra.datacenter.local"),
            properties.get("cassandra.username"),
            properties.get("cassandra.password"),
            properties.get("cassandra.keyspace.strategy"),
            properties.get("cassandra.keyspace.application"),
            properties.get("cassandra.lock.keyspace"),
            properties.get("cassandra.keyspace.replication"),
            properties.get("cassandra.connections"),
            properties.get("usergrid.notifications.listener.run"),
            properties.get("usergrid.push.worker_count"),
            properties.get("usergrid.scheduler.enabled")
        );
    }


    @Autowired
    public void setCassandraService( CassandraService cass ) {
        this.cass = cass;
    }


    public abstract void runTool( CommandLine line ) throws Exception;
}
