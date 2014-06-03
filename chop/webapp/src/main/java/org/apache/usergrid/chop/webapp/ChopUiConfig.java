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
package org.apache.usergrid.chop.webapp;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import org.apache.commons.cli.CommandLine;
import org.apache.shiro.guice.aop.ShiroAopModule;
import org.apache.usergrid.chop.webapp.dao.SetupDao;
import org.apache.usergrid.chop.webapp.elasticsearch.ElasticSearchFig;
import org.apache.usergrid.chop.webapp.elasticsearch.EsEmbedded;
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.shiro.CustomShiroWebModule;
import org.apache.usergrid.chop.webapp.service.util.TimeUtil;
import org.safehaus.guicyfig.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.util.Enumeration;

import static org.apache.usergrid.chop.webapp.ChopUiFig.CONTEXT_TEMPDIR_KEY;

/**
 * ...
 */
@SuppressWarnings("UnusedDeclaration")
public class ChopUiConfig extends GuiceServletContextListener {

    private final static Logger LOG = LoggerFactory.getLogger(ChopUiConfig.class);

    private EsEmbedded esEmbedded;
    private Injector injector;
    private ServletContext context;

    @Override
    protected Injector getInjector() {

        if (injector != null) {
            return injector;
        }

        injector = Guice.createInjector(new CustomShiroWebModule(context), new ShiroAopModule(), new ChopUiModule());
        InjectorFactory.setInjector(injector);

        return injector;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        context = servletContextEvent.getServletContext();
        context.setAttribute(Injector.class.getName(), getInjector());

        Injector injector = getInjector();
        ElasticSearchFig elasticSearchFig = injector.getInstance(ElasticSearchFig.class);
        ChopUiFig chopUiFig = injector.getInstance(ChopUiFig.class);

        /*
         * --------------------------------------------------------------------
         * Archaius Configuration Settings
         * --------------------------------------------------------------------
         */
        ConcurrentCompositeConfiguration ccc = new ConcurrentCompositeConfiguration();
        Env env = Env.getEnvironment();
        boolean embedded = false;

        if (env == Env.ALL) {
            ConfigurationManager.getDeploymentContext().setDeploymentEnvironment("PROD");
            LOG.info("Setting environment to: PROD");

            /*
             * --------------------------------------------------------------------
             * Extract Configuration Settings from CommandLine
             * --------------------------------------------------------------------
             */
            if (ChopUiJettyRunner.getCommandLine() != null) {
                CommandLine cl = ChopUiJettyRunner.getCommandLine();

                if( cl.hasOption( 'd' ) ) {
                    String dataDir = cl.getOptionValue( 'd' );
                    LOG.info( "The -d option is given, replacing data directory with {}", dataDir );
                    elasticSearchFig.bypass( ElasticSearchFig.DATA_DIR_KEY, dataDir );
                }
                if (cl.hasOption('e')) {
                    startEmbeddedES(elasticSearchFig);
                }
                if( cl.hasOption( 'c' ) ) {
                    String serverHostPort = cl.getOptionValue( 'c' );
                    int seperatorIndex = serverHostPort.indexOf(":");
                    String serverHost = serverHostPort.substring(0,seperatorIndex);
                    String serverPort = serverHostPort.substring(seperatorIndex+1,serverHostPort.length());

                    LOG.info( "The -c option is given, replacing host with {} and port with {}", serverHost, serverPort );
                    elasticSearchFig.bypass( ElasticSearchFig.SERVERS_KEY, serverHost );
                    elasticSearchFig.bypass( ElasticSearchFig.PORT_KEY, serverPort );
                }
                if( cl.hasOption( 'n' ) ) {
                    String clusterName = cl.getOptionValue( 'n' );
                    elasticSearchFig.bypass( ElasticSearchFig.CLUSTER_NAME_KEY, clusterName );
                }
            } else {
                LOG.warn("ChopUi not started via Launcher - no command line argument processing will take place.");
            }
        } else if (env == Env.UNIT) {
            LOG.info("Operating in UNIT environment");
        }

        try {
            ConfigurationManager.loadCascadedPropertiesFromResources("chop-ui");
        } catch (IOException e) {
            LOG.warn("Failed to cascade load configuration properties: ", e);
        }

        /*
         * --------------------------------------------------------------------
         * Environment Based Configuration Property Adjustments
         * --------------------------------------------------------------------
         */
        if (LOG.isDebugEnabled()) {
            Enumeration<String> names = context.getAttributeNames();
            LOG.debug("Dumping attribute names: ");
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                LOG.debug("attribute {} = {}", name, context.getAttribute(name));
            }
        }

        // Checking if a temp directory is defined - usually null
        String contextTempDir = (String) context.getAttribute(CONTEXT_TEMPDIR_KEY);
        LOG.info("From servlet context: {} = {}", CONTEXT_TEMPDIR_KEY, contextTempDir);

        if (contextTempDir == null) {
            LOG.info("From ChopUiFig {} = {}", CONTEXT_TEMPDIR_KEY, chopUiFig.getContextTempDir());
        }

        setupStorage();
    }

    private static EsEmbedded startEmbeddedES(ElasticSearchFig elasticSearchFig) {
        LOG.info("The -e option has been provided: launching embedded elasticsearch instance.");

        // This will set the parameters needed in the fig to attach to the embedded instance
        EsEmbedded es = new EsEmbedded(elasticSearchFig);
        es.start();

        long pause = 5000;
        LOG.info("Pausing for {} ms so embedded elasticsearch can complete initialization.", pause);

        TimeUtil.sleep(pause);

        return es;
    }

    private void setupStorage() {
        LOG.info("Setting up the storage...");

        IElasticSearchClient esClient = getInjector().getInstance(IElasticSearchClient.class);
        esClient.start();
        SetupDao setupDao = getInjector().getInstance(SetupDao.class);

        LOG.info("esClient: {}", esClient);
        LOG.info("setupDao: {}", setupDao);

        try {
            setupDao.setup();
        } catch (Exception e) {
            LOG.error("Failed to setup the storage!", e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        super.contextDestroyed(servletContextEvent);

        if (esEmbedded != null) {
            esEmbedded.stop();
        }
    }
}
