package org.usergrid.rest;

import static org.junit.Assert.assertNull;
import me.prettyprint.cassandra.testutils.EmbeddedServerHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.usergrid.utils.MapUtils;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;

public abstract class AbstractRestTest extends JerseyTest {

	private static Logger logger = LoggerFactory
			.getLogger(AbstractRestTest.class);

	static EmbeddedServerHelper embedded = null;

	static ClientConfig clientConfig = new DefaultClientConfig();
	static {
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING,
				Boolean.TRUE);
	}

	public AbstractRestTest() throws TestContainerException {
		super(
				new WebAppDescriptor.Builder("org.usergrid.rest")
						.contextParam("contextConfigLocation",
								"classpath:testApplicationContext.xml")
						.servletClass(SpringServlet.class)
						.contextListenerClass(ContextLoaderListener.class)
						.requestListenerClass(RequestContextListener.class)
						.initParam("com.sun.jersey.config.property.packages",
								"org.usergrid.rest")
						.initParam(
								"com.sun.jersey.api.json.POJOMappingFeature",
								"true")
						.initParam(
								"com.sun.jersey.spi.container.ContainerRequestFilters",
								"org.usergrid.rest.filters.MeteringFilter,org.usergrid.rest.security.shiro.filters.OAuth2AccessTokenSecurityFilter,org.usergrid.rest.security.shiro.filters.BasicAuthSecurityFilter")
						.initParam(
								"com.sun.jersey.spi.container.ContainerResponseFilters",
								"org.usergrid.rest.security.CrossOriginRequestFilter,org.usergrid.rest.filters.MeteringFilter")
						.initParam(
								"com.sun.jersey.spi.container.ResourceFilters",
								"org.usergrid.rest.security.SecuredResourceFilterFactory,com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory")
						.initParam("com.sun.jersey.config.feature.DisableWADL",
								"true")
						.initParam(
								"com.sun.jersey.config.property.JSPTemplatesBasePath",
								"/WEB-INF/jsp")
						.initParam(
								"com.sun.jersey.config.property.WebPageContentRegex",
								"/(((images|css|js|jsp|WEB-INF/jsp)/.*)|(favicon\\.ico))")
						.addFilter(
								DelegatingFilterProxy.class,
								"shiroFilter",
								MapUtils.hashMap("targetFilterLifecycle",
										"true")).clientConfig(clientConfig)
						.build());
	}

	@Override
	protected TestContainerFactory getTestContainerFactory() {
		return new com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory();
	}

	@BeforeClass
	public static void setup() throws Exception {
		logger.info("setup");
		assertNull(embedded);
		embedded = new EmbeddedServerHelper();
		embedded.setup();
	}

	@AfterClass
	public static void teardown() throws Exception {
		logger.info("teardown");
		EmbeddedServerHelper.teardown();
		embedded = null;
	}

}
