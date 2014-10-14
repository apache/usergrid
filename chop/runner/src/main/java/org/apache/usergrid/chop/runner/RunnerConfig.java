/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.chop.runner;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.store.amazon.Ec2Metadata;
import org.apache.usergrid.chop.spi.RunnerRegistry;
import org.safehaus.guicyfig.Env;
import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.redshift.model.transform.ReservedNodeStaxUnmarshaller;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.netflix.config.ConfigurationManager;


/** ... */
@SuppressWarnings( "UnusedDeclaration" )
public class RunnerConfig extends GuiceServletContextListener {
    private final static Logger LOG = LoggerFactory.getLogger( RunnerConfig.class );
    private Injector injector;
    private Project project;
    private Runner runner;
    private ServletFig servletFig;
    private boolean registered = false;


    public Project getProject() {
        return project;
    }


    public Runner getRunner() {
        return runner;
    }


    public ServletFig getServletFig() {
        return servletFig;
    }


    @Override
    protected Injector getInjector() {
        if ( injector != null ) {
            return injector;
        }

        injector = Guice.createInjector( new Module() );
        return injector;
    }


    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {
        super.contextInitialized( servletContextEvent );

        /*
         * --------------------------------------------------------------------
         * Archaius Configuration Settings
         * --------------------------------------------------------------------
         */

        Env env = Env.getEnvironment();

        if ( env == Env.ALL ) {
            ConfigurationManager.getDeploymentContext().setDeploymentEnvironment( "CHOP" );
            LOG.info( "Setting environment to: CHOP" );
        }
        else if ( env == Env.UNIT ) {
            LOG.info( "Operating in UNIT environment" );
        }

        try {
            ConfigurationManager.loadCascadedPropertiesFromResources( "project" );
        }
        catch ( IOException e ) {
            LOG.error( "Failed to load project properties!", e );
            throw new RuntimeException( "Cannot do much without properly loading our configuration.", e );
        }

        /*
         * --------------------------------------------------------------------
         * Environment Based Configuration Property Adjustments
         * --------------------------------------------------------------------
         */

        servletFig = injector.getInstance( ServletFig.class );
        runner = injector.getInstance( Runner.class );
        project = injector.getInstance( Project.class );
        ServletContext context = servletContextEvent.getServletContext();

        /*
         * --------------------------------------------------------------------
         * Adjust Runner Settings to Environment
         * --------------------------------------------------------------------
         */

        if ( env == Env.UNIT || env == Env.INTEG || env == Env.ALL ) {
            runner.bypass( Runner.HOSTNAME_KEY, "localhost" );
            runner.bypass( Runner.IPV4_KEY, "127.0.0.1" );
        }
        else if ( env == Env.CHOP ) {
            Ec2Metadata.applyBypass( runner );
        }

        StringBuilder sb = new StringBuilder();
        sb.append( "https://" )
          .append( runner.getHostname() )
          .append( ':' )
          .append( runner.getServerPort() )
          .append( context.getContextPath() );
        String baseUrl = sb.toString();
        runner.bypass( Runner.URL_KEY, baseUrl );
        LOG.info( "Setting url key {} to base url {}", Runner.URL_KEY, baseUrl );

        File tempDir = new File( System.getProperties().getProperty( "java.io.tmpdir" ) );
        runner.bypass( Runner.RUNNER_TEMP_DIR_KEY, tempDir.getAbsolutePath() );
        LOG.info( "Setting runner temp directory key {} to context temp directory {}",
                Runner.RUNNER_TEMP_DIR_KEY, tempDir.getAbsolutePath() );

        /*
         * --------------------------------------------------------------------
         * Adjust ServletFig Settings to Environment
         * --------------------------------------------------------------------
         */

        servletFig.bypass( ServletFig.SERVER_INFO_KEY, context.getServerInfo() );
        LOG.info( "Setting server info key {} to {}", ServletFig.SERVER_INFO_KEY, context.getServerInfo() );

        servletFig.bypass( ServletFig.CONTEXT_PATH, context.getContextPath() );
        LOG.info( "Setting server context path key {} to {}", ServletFig.CONTEXT_PATH, context.getContextPath() );

        // @todo Is this necessary?
        servletFig.bypass( ServletFig.CONTEXT_TEMPDIR_KEY, tempDir.getAbsolutePath() );
        LOG.info( "Setting runner context temp directory key {} to context temp directory {}",
                ServletFig.CONTEXT_TEMPDIR_KEY, tempDir.getAbsolutePath() );

        /*
         * --------------------------------------------------------------------
         * Start Up The RunnerRegistry and Register
         * --------------------------------------------------------------------
         */

         if ( isTestMode() )
         {
             runner.bypass( Runner.HOSTNAME_KEY, "localhost" );
             runner.bypass( Runner.IPV4_KEY, "127.0.0.1" );
             project.bypass( Project.LOAD_KEY, "bogus-load-key" );
             project.bypass( Project.ARTIFACT_ID_KEY, "bogus-artifact-id" );
             project.bypass( Project.GROUP_ID_KEY, "org.apache.usergrid.chop" );
             project.bypass( Project.CHOP_VERSION_KEY, "bogus-chop-version" );

             SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd.HH.mm.ss" );
             dateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
             project.bypass( Project.CREATE_TIMESTAMP_KEY, dateFormat.format( new Date() ) );

             project.bypass( Project.GIT_URL_KEY, "http://stash.safehaus.org/projects/CHOP/repos/main/browse" );
             project.bypass( Project.GIT_UUID_KEY, "d637a8ce" );
             project.bypass( Project.LOAD_TIME_KEY, dateFormat.format( new Date() ) );
             project.bypass( Project.PROJECT_VERSION_KEY, "1.0.0-SNAPSHOT" );
         }

        if ( runner.getHostname() != null && project.getLoadKey() != null ) {
            if ( env != Env.TEST && env != Env.UNIT ) {
                /*
                 * ------------------------------------------------------------
                 * Register runner on a different thread since jetty runner
                 * is not started yet and the port is not known
                 * ------------------------------------------------------------
                 */
                Thread registryThread = new Thread( new Runnable() {
                    @Override
                    public void run() {
                        registerRunner();
                    }
                }
                );
                registryThread.start();
            }
            else {
                LOG.warn( "Env = {} so we are not registering this runner.", env );
            }

        }
        else {
            LOG.warn( "Runner registry not started, and not registered: insufficient configuration parameters." );
        }
    }


    private void registerRunner(  ) {
        RunnerAppJettyRunner jettyRunner = RunnerAppJettyRunner.getInstance();

        int time = 5000;
        while ( ! jettyRunner.isStarted( 100 ) ) {
            time -= 100;

            if ( time < 0 ) {
                throw new IllegalStateException( "This runner has not been started yet!" );
            }
        }
        runner.bypass( Runner.SERVER_PORT_KEY, "" + jettyRunner.getPort() );
        runner.bypass( Runner.URL_KEY, "https://" + runner.getHostname() + ":" + runner.getServerPort() );

        final RunnerRegistry registry = getInjector().getInstance( RunnerRegistry.class );
        registry.register( runner );
        registered =true;
        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable() {
            @Override
            public void run() {
                if ( registered ) {
                    System.err.println( "Premature shutdown, attempting to unregister this runner." );
                    registry.unregister( runner );
                    LOG.info( "Unregistering runner on shutdownx: {}", runner.getHostname() );
                    registered = false;
                }
            }
        } ) );
        LOG.info( "Registered runner information in coordinator registry." );
    }


    static TestMode mode = null;

    public static TestMode getTestMode() {
        if ( mode == null ) {
            boolean hasKey = System.getProperties().containsKey( TestMode.TEST_MODE_PROPERTY );

            if ( hasKey ) {
                mode = TestMode.valueOf( System.getProperty( TestMode.TEST_MODE_PROPERTY,
                        TestMode.UNDEFINED.toString() ) );
            }
        }

        return mode;
    }

    public static boolean isTestMode() {
        return mode != null && ( mode == TestMode.INTEG || mode == TestMode.UNIT );
    }


    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {
        Env env = Env.getEnvironment();
        RunnerRegistry registry = getInjector().getInstance( RunnerRegistry.class );

        if ( env == Env.CHOP || registered ) {
            registry.unregister( injector.getInstance( Runner.class ) );
            registered = false;
            LOG.info( "Unregistered runner information in coordinator registry." );
        }
        else {
            LOG.warn( "Environment is set to {} so we are not un-registering this runner.", env );
        }

        super.contextDestroyed( servletContextEvent );
    }
}
