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
package org.apache.usergrid.launcher;


import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


public class Server implements org.springframework.context.ApplicationContextAware {

    public static final boolean INSTALL_JSP_SERVLETS = true;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger( Server.class );

    public static Server instance = null;

    org.apache.commons.cli.CommandLine line = null;

    boolean initializeDatabaseOnStart = false;
    boolean startDatabaseWithServer = false;

    org.glassfish.grizzly.http.server.HttpServer httpServer;
    org.glassfish.grizzly.servlet.ServletHandler handler;
    EmbeddedServerHelper embeddedCassandra = null;

    protected org.apache.usergrid.persistence.EntityManagerFactory emf;

    protected org.apache.usergrid.services.ServiceManagerFactory smf;

    protected org.apache.usergrid.management.ManagementService management;

    protected java.util.Properties properties;

    protected org.apache.usergrid.mq.QueueManagerFactory qmf;

    int port = org.glassfish.grizzly.http.server.NetworkListener.DEFAULT_NETWORK_PORT;

    boolean daemon = true;


    public Server() {
        instance = this;
    }


    public static void main( String[] args ) {
        instance = new Server();
        instance.startServerFromCommandLine( args );
    }


    public static Server getInstance() {
        return instance;
    }


    public void startServerFromCommandLine( String[] args ) {
        org.apache.commons.cli.CommandLineParser parser = new org.apache.commons.cli.GnuParser();
        line = null;
        try {
            line = parser.parse( createOptions(), args );
        }
        catch ( org.apache.commons.cli.ParseException exp ) {
            printCliHelp( "Parsing failed.  Reason: " + exp.getMessage() );
        }

        if ( line == null ) {
            return;
        }

        startDatabaseWithServer = line.hasOption( "db" );
        initializeDatabaseOnStart = line.hasOption( "init" );

        if ( line.hasOption( "port" ) ) {
            try {
                port = ( ( Number ) line.getParsedOptionValue( "port" ) ).intValue();
            }
            catch ( org.apache.commons.cli.ParseException exp ) {
                printCliHelp( "Parsing failed.  Reason: " + exp.getMessage() );
                return;
            }
        }
        startServer();
    }


    @SuppressWarnings("InfiniteLoopStatement")
    public synchronized void startServer() {

        if ( startDatabaseWithServer ) {
            startCassandra();
        }

        httpServer = org.glassfish.grizzly.http.server.HttpServer.createSimpleServer( ".", port );

        handler = new org.glassfish.grizzly.servlet.ServletHandler();

        handler.addContextParameter( com.sun.jersey.spi.spring.container.servlet.SpringServlet.CONTEXT_CONFIG_LOCATION,
                "classpath:/usergrid-standalone-context.xml" );

        handler.addServletListener( org.springframework.web.context.ContextLoaderListener.class.getName() );
        handler.addServletListener( org.springframework.web.context.request.RequestContextListener.class.getName() );

        com.sun.jersey.api.json.JSONConfiguration.badgerFish();

        handler.addInitParameter( com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES, "org.apache.usergrid" );
        handler.addInitParameter( com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING, "true" );
        handler.addInitParameter( com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
                "org.apache.usergrid.rest.filters.MeteringFilter,org.apache.usergrid.rest.filters.JSONPCallbackFilter," +
                        "org.apache.usergrid.rest.security.shiro.filters.OAuth2AccessTokenSecurityFilter," +
                        "org.apache.usergrid.rest.security.shiro.filters.BasicAuthSecurityFilter," +
                        "org.apache.usergrid.rest.security.shiro.filters.ClientCredentialsSecurityFilter" );
        handler.addInitParameter( com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
                "org.apache.usergrid.rest.security.CrossOriginRequestFilter,org.apache.usergrid.rest.filters.MeteringFilter" );
        handler.addInitParameter( com.sun.jersey.api.core.ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
                "org.apache.usergrid.rest.security.SecuredResourceFilterFactory,com.sun.jersey.api.container.filter"
                        + ".RolesAllowedResourceFilterFactory" );
        handler.addInitParameter( com.sun.jersey.api.core.ResourceConfig.FEATURE_DISABLE_WADL, "true" );
        handler.addInitParameter( com.sun.jersey.spi.container.servlet.ServletContainer.JSP_TEMPLATES_BASE_PATH, "/WEB-INF/jsp" );
        handler.addInitParameter( com.sun.jersey.spi.container.servlet.ServletContainer.PROPERTY_WEB_PAGE_CONTENT_REGEX,
                "/(((images|css|js|jsp|WEB-INF/jsp)/.*)|(favicon\\.ico))" );

        handler.setServletInstance( new com.sun.jersey.spi.spring.container.servlet.SpringServlet() );
        // handler.setServletPath("/ROOT");
        // handler.setContextPath("/ROOT");

        handler.setProperty( "load-on-startup", 1 );

        java.util.Map<String, String> initParameters = new java.util.HashMap<String, String>();
        initParameters.put( "targetFilterLifecycle", "true" );
        handler.addFilter( new org.apache.usergrid.rest.filters.ContentTypeFilter(), "contentTypeFilter", java.util.Collections.EMPTY_MAP );

        handler.addFilter( new org.springframework.web.filter.DelegatingFilterProxy(), "shiroFilter", initParameters );

        handler.addFilter( new org.apache.usergrid.rest.SwaggerServlet(), "swagger", null );

        // handler.addFilter(new SpringServlet(), "spring", null);

        setupJspMappings();

        httpServer.getServerConfiguration().addHttpHandler( handler, "/*" );

        // TODO: find replacement ClasspathStaticHandler, because we had to remove it for ASF policy reasons 
//        ClasspathStaticHttpHandler static_handler = new ClasspathStaticHttpHandler( "/html/css/" );
//        httpServer.getServerConfiguration().addHttpHandler( static_handler, "/css/*" );

        httpServer.getServerConfiguration().setJmxEnabled( true );

        setThreadSize();

        try {
            httpServer.start();
        }
        catch ( java.io.IOException e ) {
            e.printStackTrace();
        }

        if ( daemon ) {
            while ( true ) {
                try {
                    Thread.sleep( Long.MAX_VALUE );
                }
                catch ( InterruptedException e ) {
                    logger.warn( "Interrupted" );
                }
            }
        }
    }


    private int getThreadSizeFromSystemProperties() {
        // the default value is number of cpu core * 2.
        // see org.glassfich.grizzly.strategies.AbstractIOStrategy.createDefaultWorkerPoolconfig()
        int threadSize = Runtime.getRuntime().availableProcessors() * 2;

        String threadSizeString = System.getProperty( "server.threadSize" );
        if ( threadSizeString != null ) {
            try {
                threadSize = Integer.parseInt( threadSizeString );
            }
            catch ( Exception e ) {
                // ignore all Exception
            }
        }
        else {
            try {
                threadSize = Integer.parseInt( System.getProperty( "server.threadSizeScale" ) ) * Runtime.getRuntime()
                                                                                                         .availableProcessors();
            }
            catch ( Exception e ) {
                // ignore all Exception
            }
        }

        return threadSize;
    }


    private void setThreadSize() {

        int threadSize = getThreadSizeFromSystemProperties();

        java.util.Collection<org.glassfish.grizzly.http.server.NetworkListener> listeners = httpServer.getListeners();
        for ( org.glassfish.grizzly.http.server.NetworkListener listener : listeners ) {
            listener.getTransport().getKernelThreadPoolConfig();
            org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder builder = org.glassfish.grizzly.nio.transport
                    .TCPNIOTransportBuilder.newInstance();
            org.glassfish.grizzly.threadpool.ThreadPoolConfig config = builder.getWorkerThreadPoolConfig();
            config.setCorePoolSize( threadSize );
            config.setMaxPoolSize( threadSize );
            org.glassfish.grizzly.nio.transport.TCPNIOTransport transport = builder.build();
            listener.setTransport( transport );
        }

        logger.info( "thread size set as {}", threadSize );
    }


    private void setupJspMappings() {
        if ( !INSTALL_JSP_SERVLETS ) {
            return;
        }

        org.apache.jasper.runtime.JspFactoryImpl factory = new org.apache.jasper.runtime.JspFactoryImpl();
        javax.servlet.jsp.JspFactory.setDefaultFactory( factory );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.TestResource.error_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/TestResource/error.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.TestResource.test_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/TestResource/test.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UsersResource.error_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UsersResource/error.jsp" );

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UsersResource.resetpw_005femail_005fform_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UsersResource/resetpw_email_form.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UsersResource"
                + ".resetpw_005femail_005fsuccess_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UsersResource/resetpw_email_success.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UserResource.activate_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UserResource/activate.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UserResource.confirm_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UserResource/confirm.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UserResource.error_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UserResource/error.jsp" );

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UserResource.resetpw_005femail_005fform_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UserResource/resetpw_email_form.jsp" );

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UserResource.resetpw_005femail_005fsuccess_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UserResource/resetpw_email_success.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UserResource.resetpw_005fset_005fform_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UserResource/resetpw_set_form.jsp" );

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.users.UserResource.resetpw_005fset_005fsuccess_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/users/UserResource/resetpw_set_success.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.organizations.OrganizationResource.activate_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/organizations/OrganizationResource/activate.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.organizations.OrganizationResource.confirm_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/organizations/OrganizationResource/confirm.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.organizations.OrganizationResource.error_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/organizations/OrganizationResource/error.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.ManagementResource.authorize_005fform_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/ManagementResource/authorize_form.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.management.ManagementResource.error_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/management/ManagementResource/error.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UsersResource.error_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UsersResource/error.jsp" );

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UsersResource.resetpw_005femail_005fform_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UsersResource/resetpw_email_form.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UsersResource"
                + ".resetpw_005femail_005fsuccess_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UsersResource/resetpw_email_success.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UserResource.activate_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UserResource/activate.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UserResource.confirm_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UserResource/confirm.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UserResource.error_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UserResource/error.jsp" );

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UserResource.resetpw_005femail_005fform_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UserResource/resetpw_email_form.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UserResource"
                + ".resetpw_005femail_005fsuccess_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UserResource/resetpw_email_success.jsp" );

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UserResource.resetpw_005fset_005fform_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UserResource/resetpw_set_form.jsp" );

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.users.UserResource.resetpw_005fset_005fsuccess_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/users/UserResource/resetpw_set_success.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.ApplicationResource.authorize_005fform_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/ApplicationResource/authorize_form.jsp" );

        mapServlet( "jsp.WEB_002dINF.jsp.org.apache.usergrid.rest.applications.ApplicationResource.error_jsp",
                "/WEB-INF/jsp/org/apache/usergrid/rest/applications/ApplicationResource/error.jsp" );
    }


    private void mapServlet( String cls, String mapping ) {

        try {
            javax.servlet.Servlet servlet = ( javax.servlet.Servlet ) org.glassfish.grizzly.http.server.util
                    .ClassLoaderUtil.load( cls );
            if ( servlet != null ) {
                org.glassfish.grizzly.servlet.ServletHandler handler = new org.glassfish.grizzly.servlet.ServletHandler( servlet );
                handler.setServletPath( mapping );
                httpServer.getServerConfiguration().addHttpHandler( handler, mapping );
            }
        }
        catch ( Exception e ) {
            logger.error( "Unable to add JSP page: " + mapping );
        }

        logger.info( "jsp: " + javax.servlet.jsp.JspFactory.getDefaultFactory() );
    }


    public synchronized void stopServer() {
        java.util.Collection<org.glassfish.grizzly.http.server.NetworkListener> listeners = httpServer.getListeners();
        for ( org.glassfish.grizzly.http.server.NetworkListener listener : listeners ) {
            try {
                listener.stop();
            }
            catch ( java.io.IOException e ) {
                e.printStackTrace();
            }
        }

        if ( httpServer != null ) {
            httpServer.stop();
            httpServer = null;
        }

        if ( embeddedCassandra != null ) {
            stopCassandra();
            embeddedCassandra = null;
        }

        if ( ctx instanceof org.springframework.web.context.support.XmlWebApplicationContext ) {
            ( ( org.springframework.web.context.support.XmlWebApplicationContext ) ctx ).close();
        }
    }


    public void setDaemon( boolean daemon ) {
        this.daemon = daemon;
    }


    public boolean isRunning() {
        return ( httpServer != null );
    }


    static void printCliHelp( String message ) {
        System.out.println( message );
        org.apache.commons.cli.HelpFormatter formatter = new org.apache.commons.cli.HelpFormatter();
        formatter.printHelp( "java -jar usergrid-standalone-0.0.1-SNAPSHOT.jar ", createOptions() );
        System.exit( -1 );
    }


    static Options createOptions() {
        // the nogui option will be required due to combining the graphical
        // launcher with this standalone CLI based server
        Options options = new Options();
        OptionBuilder.withDescription( "Start launcher without UI" );
        OptionBuilder.isRequired( true );
        Option noguiOption = OptionBuilder.create( "nogui" );

        OptionBuilder.isRequired( false );
        OptionBuilder.withDescription( "Initialize database" );
        Option initOption = OptionBuilder.create( "init" );

        OptionBuilder.withDescription( "Start database" );
        Option dbOption = OptionBuilder.create( "db" );

        OptionBuilder.withDescription( "Http port (without UI)" );
        OptionBuilder.hasArg();
        OptionBuilder.withArgName( "PORT" );
        OptionBuilder.withLongOpt( "port" );
        OptionBuilder.withType( Number.class );
        Option portOption = OptionBuilder.create( 'p' );

        options.addOption( initOption );
        options.addOption( dbOption );
        options.addOption( portOption );
        options.addOption( noguiOption );

        return options;
    }


    public synchronized void startCassandra() {
        if ( embeddedCassandra == null ) {
            embeddedCassandra = new EmbeddedServerHelper();

            if ( initializeDatabaseOnStart ) {
                logger.info( "Initializing Cassandra" );
                try {
                    embeddedCassandra.setup();
                }
                catch ( Exception e ) {
                    logger.error( "Unable to initialize Cassandra", e );
                    System.exit( 0 );
                }
            }
        }
        logger.info( "Starting Cassandra" );
        try {
            embeddedCassandra.start();
        }
        catch ( Exception e ) {
            logger.error( "Unable to start Cassandra", e );
            System.exit( 0 );
        }
    }


    public synchronized void stopCassandra() {
        logger.info( "Stopping Cassandra" );
        embeddedCassandra.stop();
    }


    public org.apache.usergrid.persistence.EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }


    @org.springframework.beans.factory.annotation.Autowired
    public void setEntityManagerFactory( org.apache.usergrid.persistence.EntityManagerFactory emf ) {
        this.emf = emf;
    }


    public org.apache.usergrid.services.ServiceManagerFactory getServiceManagerFactory() {
        return smf;
    }


    @org.springframework.beans.factory.annotation.Autowired
    public void setServiceManagerFactory( org.apache.usergrid.services.ServiceManagerFactory smf ) {
        this.smf = smf;
    }


    public org.apache.usergrid.management.ManagementService getManagementService() {
        return management;
    }


    @org.springframework.beans.factory.annotation.Autowired
    public void setManagementService( org.apache.usergrid.management.ManagementService management ) {
        this.management = management;
    }


    public java.util.Properties getProperties() {
        return properties;
    }


    @org.springframework.beans.factory.annotation.Autowired
    public void setProperties( java.util.Properties properties ) {
        this.properties = properties;
    }


    public org.apache.usergrid.mq.QueueManagerFactory getQueueManagerFactory() {
        return qmf;
    }


    @org.springframework.beans.factory.annotation.Autowired
    public void setQueueManagerFactory( org.apache.usergrid.mq.QueueManagerFactory qmf ) {
        this.qmf = qmf;
    }


    public boolean isInitializeDatabaseOnStart() {
        return initializeDatabaseOnStart;
    }


    public void setInitializeDatabaseOnStart( boolean initializeDatabaseOnStart ) {
        this.initializeDatabaseOnStart = initializeDatabaseOnStart;
    }


    public boolean isStartDatabaseWithServer() {
        return startDatabaseWithServer;
    }


    public void setStartDatabaseWithServer( boolean startDatabaseWithServer ) {
        this.startDatabaseWithServer = startDatabaseWithServer;
    }


    boolean databaseInitializationPerformed = false;


    public void springInit() {
        logger.info( "Initializing server with Spring" );

        // If we're running an embedded Cassandra, we always need to initialize
        // it since Hector wipes the data on startup.
        //
        if ( initializeDatabaseOnStart ) {

            if ( databaseInitializationPerformed ) {
                logger.info( "Can only attempt to initialized database once per JVM process" );
                return;
            }
            databaseInitializationPerformed = true;

            logger.info( "Initializing Cassandra database" );
            java.util.Map<String, String> properties = emf.getServiceProperties();
            if ( properties != null ) {
                logger.error( "System properties are initialized, database is set up already." );
                return;
            }

            try {
                emf.setup();
            }
            catch ( Exception e ) {
                logger.error( "Unable to complete core database setup, possibly due to it being setup already", e );
            }

            try {
                management.setup();
            }
            catch ( Exception e ) {
                logger.error( "Unable to complete management database setup, possibly due to it being setup already",
                        e );
            }

            logger.info( "Usergrid schema setup" );
        }
    }


    org.springframework.context.ApplicationContext ctx;


    @Override
    public void setApplicationContext( org.springframework.context.ApplicationContext ctx ) throws org.springframework.beans.BeansException {
        this.ctx = ctx;
    }


    public String getAccessTokenForAdminUser( String email ) {
        try {
            org.apache.usergrid.management.UserInfo user = management.getAdminUserByEmail( email );
            return management.getAccessTokenForAdminUser( user.getUuid(), 0 );
        }
        catch ( Exception e ) {
            logger.error( "Unable to get user: " + email );
        }
        return null;
    }


    public java.util.UUID getAdminUUID( String email ) {
        try {
            org.apache.usergrid.management.UserInfo user = management.getAdminUserByEmail( email );
            return user.getUuid();
        }
        catch ( Exception e ) {
            logger.error( "Unable to get user: " + email );
        }
        return null;
    }
}
