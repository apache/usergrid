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
package org.usergrid.rest;

import static org.junit.Assert.*;
import static org.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import me.prettyprint.hector.testutils.EmbeddedServerHelper;

import org.codehaus.jackson.JsonNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.usergrid.java.client.Client;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.entities.User;
import org.usergrid.utils.MapUtils;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;

/**
 * Base class for testing Usergrid Jersey-based REST API. Implementations should
 * model the paths mapped, not the method names. For example, to test the the
 * "password" mapping on applications.users.UserResource for a PUT method, the
 * test method(s) should following the following naming convention: test_[HTTP
 * verb]_[action mapping]_[ok|fail][_[specific failure condition if multiple]
 */
// @Autowire
public abstract class AbstractRestTest extends JerseyTest {

    private static Logger logger = LoggerFactory
            .getLogger(AbstractRestTest.class);

    static EmbeddedServerHelper embedded = null;
    static boolean usersSetup = false;

    protected static String access_token;

    private ManagementService managementService;

    static ClientConfig clientConfig = new DefaultClientConfig();

    protected static Client client;

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
        setupUsers();
    }

    protected void setupUsers() {

        if (usersSetup)
            return;
     
        JsonNode node = resource().path("/management/token")
                .queryParam("grant_type", "password")
                .queryParam("username", "test@usergrid.com")
                .queryParam("password", "test")
                .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

        String mgmToken = node.get("access_token").getTextValue();
        //

        Map<String, String> payload = hashMap("email", "ed@anuff.com")
                .map("username", "edanuff").map("name", "Ed Anuff")
                .map("password", "sesame").map("pin", "1234");

        node = resource().path("/test-app/users")
                .queryParam("access_token", mgmToken)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        // client.setApiUrl(apiUrl);
       
        usersSetup = true;

    }

    
    public void loginClient() {
        // now create a client that logs in ed
        client = new Client("test-app").withApiUrl(getBaseURI().toString());
        org.usergrid.java.client.response.ApiResponse response = client
                .authorizeAppUser("ed@anuff.com", "sesame");

        assertTrue(response != null && response.getError() == null);

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

    public static void logNode(JsonNode node) {
        logger.info(mapToFormattedJsonString(node));
    }

    /**
     * Hook to get the token for our base user
     */
    @Before
    public void acquireToken() throws Exception {

        WebApplicationContext context = ContextLoader
                .getCurrentWebApplicationContext();
        managementService = (ManagementService) context
                .getBean("managementService");
        ApplicationInfo appInfo = managementService.getApplication("test-app");
        User user = managementService.getAppUserByIdentifier(appInfo.getId(),
                Identifier.from("ed@anuff.com"));
        access_token = managementService.getAccessTokenForAppUser(
                appInfo.getId(), user.getUuid());
        // client.setApiUrl(apiUrl)
        
        loginClient();

    }

    /**
     * Acquire the management token for the test@usergrid.com user
     * 
     * @return
     */
    protected String mgmtToken() {
        JsonNode node = resource().path("/management/token")
                .queryParam("grant_type", "password")
                .queryParam("username", "test@usergrid.com")
                .queryParam("password", "test")
                .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
        String mgmToken = node.get("access_token").getTextValue();
        return mgmToken;

    }

    /**
     * Get the entity from the entity array in the response
     * 
     * @param response
     * @param index
     * @return
     */
    protected JsonNode getEntity(JsonNode response, int index) {
        return response.get("entities").get(index);
    }
}
