package org.usergrid.standalone;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
import org.glassfish.grizzly.servlet.ServletHandler;
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
import org.usergrid.services.ServiceManagerFactory;
import org.usergrid.standalone.cassandra.EmbeddedServerHelper;

import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

public class Server implements ApplicationContextAware {

	public static final boolean INSTALL_JSP_SERVLETS = true;

	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	public static Server instance = null;

	CommandLine line = null;

	boolean initializeDatabaseOnStart = false;
	boolean startDatabaseWithServer = false;

	HttpServer httpServer;
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

		ServletHandler handler = new ServletHandler();

		handler.addContextParameter("contextConfigLocation",
				"classpath:standaloneApplicationContext.xml");

		handler.addServletListener(ContextLoaderListener.class.getName());
		handler.addServletListener(RequestContextListener.class.getName());

		com.sun.jersey.api.json.JSONConfiguration.badgerFish();

		handler.addInitParameter("com.sun.jersey.config.property.packages",
				"org.usergrid");
		handler.addInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
				"true");
		handler.addInitParameter(
				"com.sun.jersey.spi.container.ContainerRequestFilters",
				"org.usergrid.rest.filters.MeteringFilter,org.usergrid.rest.filters.JSONPCallbackFilter,org.usergrid.rest.security.shiro.filters.OAuth2AccessTokenSecurityFilter,org.usergrid.rest.security.shiro.filters.BasicAuthSecurityFilter,org.usergrid.rest.security.shiro.filters.ClientCredentialsSecurityFilter");
		handler.addInitParameter(
				"com.sun.jersey.spi.container.ContainerResponseFilters",
				"org.usergrid.rest.security.CrossOriginRequestFilter,org.usergrid.rest.filters.MeteringFilter");
		handler.addInitParameter(
				"com.sun.jersey.spi.container.ResourceFilters",
				"org.usergrid.rest.security.SecuredResourceFilterFactory,com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory");
		handler.addInitParameter("com.sun.jersey.config.feature.DisableWADL",
				"true");
		handler.addInitParameter(
				"com.sun.jersey.config.property.JSPTemplatesBasePath",
				"/WEB-INF/jsp");
		handler.addInitParameter(
				"com.sun.jersey.config.property.WebPageContentRegex",
				"/(((images|css|js|jsp|WEB-INF/jsp)/.*)|(favicon\\.ico))");

		handler.setServletInstance(new SpringServlet());
		// handler.setServletPath("/ROOT");
		// handler.setContextPath("/ROOT");

		handler.setProperty("load-on-startup", 1);

		Map<String, String> initParameters = new HashMap<String, String>();
		initParameters.put("targetFilterLifecycle", "true");
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

	private void setupJspMappings() {
		if (!INSTALL_JSP_SERVLETS) {
			return;
		}

		JspFactoryImpl factory = new JspFactoryImpl();
		JspFactory.setDefaultFactory(factory);

		mapServlet(
				"jsp.WEB_002dINF.jsp.org.usergrid.rest.TestResource.test_jsp",
				"/WEB-INF/jsp/org/usergrid/rest/TestResource/test.jsp");

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
				"jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UsersResource.resetpw_005femail_005fform_jsp",
				"/WEB-INF/jsp/org/usergrid/rest/applications/users/UsersResource/resetpw_email_form.jsp");

		mapServlet(
				"jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UsersResource.resetpw_005femail_005fsuccess_jsp",
				"/WEB-INF/jsp/org/usergrid/rest/applications/users/UsersResource/resetpw_email_success.jsp");

		mapServlet(
				"jsp.WEB_002dINF.jsp.org.usergrid.rest.applications.users.UserResource.activate_jsp",
				"/WEB-INF/jsp/org/usergrid/rest/applications/users/UserResource/activate.jsp");

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
				"jsp.WEB_002dINF.jsp.org.usergrid.rest.management.ManagementResource.authorize_005fform_jsp",
				"/WEB-INF/jsp/org/usergrid/rest/management/ManagementResource/authorize_form.jsp");

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

			logger.info("Starting Cassandra");
			try {
				embeddedCassandra.start();
			} catch (Exception e) {
				logger.error("Unable to start Cassandra", e);
				System.exit(0);
			}
		} else {
			logger.info("Can only start Cassandra once per JVM process");
		}

	}

	public synchronized void stopCassandra() {
		// logger.info("Stopping Cassandra");
		// EmbeddedServerHelper.teardown();
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
			Setup setup = ((EntityManagerFactoryImpl) emf).getSetup();
			setup.checkKeyspaces();
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
			return management.getAccessTokenForAdminUser(user.getUuid());
		} catch (Exception e) {
			logger.error("Unable to get user: " + email);
		}
		return null;
	}

}
