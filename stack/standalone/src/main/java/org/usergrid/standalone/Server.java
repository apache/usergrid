/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.standalone;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.Servlet;
import javax.servlet.jsp.JspFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jasper.runtime.JspFactoryImpl;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.util.ClassLoaderUtil;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.usergrid.management.ManagementService;
import org.usergrid.management.UserInfo;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.usergrid.persistence.cassandra.Setup;
import org.usergrid.rest.SwaggerServlet;
import org.usergrid.rest.filters.ContentTypeFilter;
import org.usergrid.services.ServiceManagerFactory;
import org.usergrid.standalone.cassandra.EmbeddedServerHelper;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

public class Server implements ApplicationContextAware {

    public static final boolean INSTALL_JSP_SERVLETS = true;

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static Server instance = null;

    CommandLine line = null;

    boolean initializeDatabaseOnStart = false;
    boolean startDatabaseWithServer = false;

    HttpServer httpServer;
    ServletHandler handler;
    EmbeddedServerHelper embeddedCassandra = null;

    protected EntityManagerFactory emf;

    protected ServiceManagerFactory smf;

    protected ManagementService management;

    protected Properties properties;

    protected QueueManagerFactory qmf;

    int port = NetworkListener.DEFAULT_NETWORK_PORT;

    boolean daemon = true;

    public Server() {
        instance = this;
    }

    public static void main(String[] args) {
        instance = new Server();
        instance.startServerFromCommandLine(args);
    }

    public static Server getInstance() {
        return instance;
    }

    public void startServerFromCommandLine(String[] args) {
        CommandLineParser parser = new GnuParser();
        line = null;
        try {
            line = parser.parse(createOptions(), args);
        } catch (ParseException exp) {
            printCliHelp("Parsing failed.  Reason: " + exp.getMessage());
        }

        if (line == null) {
            return;
        }

        startDatabaseWithServer = line.hasOption("db");
        initializeDatabaseOnStart = line.hasOption("init");

        if (line.hasOption("port")) {
            try {
                port = ((Number) line.getParsedOptionValue("port")).intValue();
            } catch (ParseException exp) {
                printCliHelp("Parsing failed.  Reason: " + exp.getMessage());
                return;
            }
        }
        startServer();
    }

    public synchronized void startServer() {

        if (startDatabaseWithServer) {
            startCassandra();
        }

        httpServer = HttpServer.createSimpleServer(".", port);

        handler = new ServletHandler();

        handler.addContextParameter(SpringServlet.CONTEXT_CONFIG_LOCATION,
                "classpath:/usergrid-standalone-context.xml");

        handler.addServletListener(ContextLoaderListener.class.getName());
        handler.addServletListener(RequestContextListener.class.getName());

        com.sun.jersey.api.json.JSONConfiguration.badgerFish();

        handler.addInitParameter(PackagesResourceConfig.PROPERTY_PACKAGES,
                "org.usergrid");
        handler.addInitParameter(JSONConfiguration.FEATURE_POJO_MAPPING,
                "true");
        handler.addInitParameter(
        		ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
                "org.usergrid.rest.filters.MeteringFilter,org.usergrid.rest.filters.JSONPCallbackFilter,org.usergrid.rest.security.shiro.filters.OAuth2AccessTokenSecurityFilter,org.usergrid.rest.security.shiro.filters.BasicAuthSecurityFilter,org.usergrid.rest.security.shiro.filters.ClientCredentialsSecurityFilter");
        handler.addInitParameter(
        		ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
                "org.usergrid.rest.security.CrossOriginRequestFilter,org.usergrid.rest.filters.MeteringFilter");
        handler.addInitParameter(
        		ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
                "org.usergrid.rest.security.SecuredResourceFilterFactory,com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory");
        handler.addInitParameter(ResourceConfig.FEATURE_DISABLE_WADL,
                "true");
        handler.addInitParameter(
        		ServletContainer.JSP_TEMPLATES_BASE_PATH,
                "/WEB-INF/jsp");
        handler.addInitParameter(
        		ServletContainer.PROPERTY_WEB_PAGE_CONTENT_REGEX,
                "/(((images|css|js|jsp|WEB-INF/jsp)/.*)|(favicon\\.ico))");

        handler.setServletInstance(new SpringServlet());
        // handler.setServletPath("/ROOT");
        // handler.setContextPath("/ROOT");

        handler.setProperty("load-on-startup", 1);

        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("targetFilterLifecycle", "true");
        handler.addFilter(new ContentTypeFilter(), "contentTypeFilter",
                Collections.EMPTY_MAP);

        handler.addFilter(new DelegatingFilterProxy(), "shiroFilter",
                initParameters);

        handler.addFilter(new SwaggerServlet(), "swagger", null);

        // handler.addFilter(new SpringServlet(), "spring", null);

        setupJspMappings();

        httpServer.getServerConfiguration().addHttpHandler(handler, "/*");

        ClasspathStaticHttpHandler static_handler = new ClasspathStaticHttpHandler(
                "/html/css/");
        httpServer.getServerConfiguration().addHttpHandler(static_handler,
                "/css/*");

        httpServer.getServerConfiguration().setJmxEnabled(true);

		setThreadSize();
		
        try {
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (daemon) {
            while (true) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted");
                }
            }
        }
    }

	private int getThreadSizeFromSystemProperties() {
		// the default value is number of cpu core * 2. 
		// see org.glassfich.grizzly.strategies.AbstractIOStrategy.createDefaultWorkerPoolconfig()
		int threadSize = Runtime.getRuntime().availableProcessors() * 2;
		
		String threadSizeString = System.getProperty("server.threadSize");
		if(threadSizeString!=null) {
			try {
				threadSize = Integer.parseInt(threadSizeString);
			} catch(Exception e) {
				// ignore all Exception
			}
		}
		else {
			try {
				threadSize = Integer.parseInt(System.getProperty("server.threadSizeScale")) * Runtime.getRuntime().availableProcessors();
			} catch(Exception e) {
				// ignore all Exception
			}
		}
		
		return threadSize;
		
	}
	
	private void setThreadSize() {

		int threadSize = getThreadSizeFromSystemProperties();
		
		Collection<NetworkListener> listeners = httpServer.getListeners();
		for(NetworkListener listener : listeners) {
			listener.getTransport().getKernelThreadPoolConfig();
			TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance(); 
			ThreadPoolConfig config = builder.getWorkerThreadPoolConfig(); 
			config.setCorePoolSize(threadSize);
			config.setMaxPoolSize(threadSize);
			TCPNIOTransport transport = builder.build(); 
			listener.setTransport(transport);
		}
		
		logger.info("thread size set as {}", threadSize);
		
	}
	
    private void setupJspMappings() {
        if (!INSTALL_JSP_SERVLETS) {
            return;
        }

        JspFactoryImpl factory = new JspFactoryImpl();
        JspFactory.setDefaultFactory(factory);

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.TestResource.error_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/TestResource/error.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.TestResource.test_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/TestResource/test.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UsersResource.error_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UsersResource/error.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UsersResource.resetpw_005femail_005fform_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UsersResource/resetpw_email_form.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UsersResource.resetpw_005femail_005fsuccess_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UsersResource/resetpw_email_success.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UserResource.activate_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UserResource/activate.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UserResource.confirm_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UserResource/confirm.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UserResource.error_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UserResource/error.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UserResource.resetpw_005femail_005fform_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UserResource/resetpw_email_form.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UserResource.resetpw_005femail_005fsuccess_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UserResource/resetpw_email_success.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UserResource.resetpw_005fset_005fform_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UserResource/resetpw_set_form.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.users.UserResource.resetpw_005fset_005fsuccess_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/users/UserResource/resetpw_set_success.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.organizations.OrganizationResource.activate_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/organizations/OrganizationResource/activate.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.organizations.OrganizationResource.confirm_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/organizations/OrganizationResource/confirm.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.organizations.OrganizationResource.error_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/organizations/OrganizationResource/error.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.ManagementResource.authorize_005fform_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/ManagementResource/authorize_form.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.management.ManagementResource.error_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/management/ManagementResource/error.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UsersResource.error_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UsersResource/error.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UsersResource.resetpw_005femail_005fform_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UsersResource/resetpw_email_form.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UsersResource.resetpw_005femail_005fsuccess_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UsersResource/resetpw_email_success.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UserResource.activate_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UserResource/activate.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UserResource.confirm_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UserResource/confirm.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UserResource.error_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UserResource/error.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UserResource.resetpw_005femail_005fform_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UserResource/resetpw_email_form.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UserResource.resetpw_005femail_005fsuccess_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UserResource/resetpw_email_success.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UserResource.resetpw_005fset_005fform_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UserResource/resetpw_set_form.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UserResource.resetpw_005fset_005fsuccess_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/users/UserResource/resetpw_set_success.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.ApplicationResource.authorize_005fform_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/ApplicationResource/authorize_form.jsp");

        mapServlet(
                "jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.ApplicationResource.error_jsp",
                "/WEB-INF/jsp/org/usergrid/rest/applications/ApplicationResource/error.jsp");

    }

    private void mapServlet(String cls, String mapping) {

        try {
            Servlet servlet = (Servlet) ClassLoaderUtil.load(cls);
            if (servlet != null) {
                ServletHandler handler = new ServletHandler(servlet);
                handler.setServletPath(mapping);
                httpServer.getServerConfiguration().addHttpHandler(handler,
                        mapping);
            }

        } catch (Exception e) {
            logger.error("Unable to add JSP page: " + mapping);
        }

        logger.info("jsp: " + JspFactory.getDefaultFactory());
    }

    public synchronized void stopServer() {
        Collection<NetworkListener> listeners = httpServer.getListeners();
        for(NetworkListener listener : listeners) {
            try {
                listener.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }

        stopCassandra();
        if (ctx instanceof XmlWebApplicationContext) {
            ((XmlWebApplicationContext) ctx).close();
        }
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public boolean isRunning() {
        return (httpServer != null);
    }

    public void printCliHelp(String message) {
        System.out.println(message);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                "java -jar usergrid-standalone-0.0.1-SNAPSHOT.jar ",
                createOptions());
        System.exit(-1);
    }

    public Options createOptions() {

        Options options = new Options();
        OptionBuilder.withDescription("Initialize database");
        Option initOption = OptionBuilder.create("init");

        OptionBuilder.withDescription("Start database");
        Option dbOption = OptionBuilder.create("db");

        OptionBuilder.withDescription("Http port");
        OptionBuilder.hasArg();
        OptionBuilder.withArgName("PORT");
        OptionBuilder.withLongOpt("port");
        OptionBuilder.withType(Number.class);
        Option portOption = OptionBuilder.create('p');

        options.addOption(initOption);
        options.addOption(dbOption);
        options.addOption(portOption);

        return options;
    }

    public synchronized void startCassandra() {
        if (embeddedCassandra == null) {
            embeddedCassandra = new EmbeddedServerHelper();

            if (initializeDatabaseOnStart) {
                logger.info("Initializing Cassandra");
                try {
                    embeddedCassandra.setup();
                } catch (Exception e) {
                    logger.error("Unable to initialize Cassandra", e);
                    System.exit(0);
                }
            }

        }
        logger.info("Starting Cassandra");
        try {
            embeddedCassandra.start();
        } catch (Exception e) {
            logger.error("Unable to start Cassandra", e);
            System.exit(0);
        }

    }

    public synchronized void stopCassandra() {
        logger.info("Stopping Cassandra");
        embeddedCassandra.stop();
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    @Autowired
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public ServiceManagerFactory getServiceManagerFactory() {
        return smf;
    }

    @Autowired
    public void setServiceManagerFactory(ServiceManagerFactory smf) {
        this.smf = smf;
    }

    public ManagementService getManagementService() {
        return management;
    }

    @Autowired
    public void setManagementService(ManagementService management) {
        this.management = management;
    }

    public Properties getProperties() {
        return properties;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public QueueManagerFactory getQueueManagerFactory() {
        return qmf;
    }

    @Autowired
    public void setQueueManagerFactory(QueueManagerFactory qmf) {
        this.qmf = qmf;
    }

    public boolean isInitializeDatabaseOnStart() {
        return initializeDatabaseOnStart;
    }

    public void setInitializeDatabaseOnStart(boolean initializeDatabaseOnStart) {
        this.initializeDatabaseOnStart = initializeDatabaseOnStart;
    }

    public boolean isStartDatabaseWithServer() {
        return startDatabaseWithServer;
    }

    public void setStartDatabaseWithServer(boolean startDatabaseWithServer) {
        this.startDatabaseWithServer = startDatabaseWithServer;
    }

    boolean databaseInitializationPerformed = false;

    public void springInit() {
        logger.info("Initializing server with Spring");

        // If we're running an embedded Cassandra, we always need to initialize
        // it since Hector wipes the data on startup.
        //
        if (initializeDatabaseOnStart) {

            if (databaseInitializationPerformed) {
                logger.info("Can only attempt to initialized database once per JVM process");
                return;
            }
            databaseInitializationPerformed = true;

            logger.info("Initializing Cassandra database");
            Map<String, String> properties = emf.getServiceProperties();
            if (properties != null) {
                logger.error("System properties are initialized, database is set up already.");
                return;
            }

            try {
                emf.setup();
            } catch (Exception e) {
                logger.error(
                        "Unable to complete core database setup, possibly due to it being setup already",
                        e);
            }

            try {
                management.setup();
            } catch (Exception e) {
                logger.error(
                        "Unable to complete management database setup, possibly due to it being setup already",
                        e);
            }

            logger.info("Usergrid schema setup");
        }

    }

    ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext ctx)
            throws BeansException {
        this.ctx = ctx;
    }

    public String getAccessTokenForAdminUser(String email) {
        try {
            UserInfo user = management.getAdminUserByEmail(email);
            return management.getAccessTokenForAdminUser(user.getUuid(), 0);
        } catch (Exception e) {
            logger.error("Unable to get user: " + email);
        }
        return null;
    }

    public UUID getAdminUUID(String email) {
        try {
            UserInfo user = management.getAdminUserByEmail(email);
            return user.getUuid();
        } catch (Exception e) {
            logger.error("Unable to get user: " + email);
        }
        return null;
    }

}
