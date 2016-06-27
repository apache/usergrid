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
package org.apache.usergrid.security.sso;

import com.codahale.metrics.Counter;
import com.google.inject.Injector;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.usergrid.management.*;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.codehaus.jackson.JsonNode;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.util.*;

/**
 * Created by ayeshadastagiri on 6/23/16.
 */
public class UsergridExternalProvider implements ExternalSSOProvider {
    private static final Logger logger = LoggerFactory.getLogger(ApigeeSSO2Provider.class);

    private static final String SSO_PROCESSING_TIME = "sso.processing_time";
    private static final String SSO_TOKENS_REJECTED = "sso.tokens_rejected";
    private static final String SSO_TOKENS_VALIDATED = "sso.tokens_validated";
    public static final String USERGRID_CENTRAL_URL = "usergrid.external.sso.publicKeyUrl";
    public static final String CENTRAL_CONNECTION_POOL_SIZE = "usergrid.central.connection.pool.size";
    public static final String CENTRAL_CONNECTION_TIMEOUT = "usergrid.central.connection.timeout";
    public static final String CENTRAL_READ_TIMEOUT = "usergrid.central.read.timeout";
    private static final String SSO_CREATED_LOCAL_ADMINS = "sso.created_local_admins";

    protected ManagementService management;
    protected MetricsFactory metricsFactory;
    protected Properties properties;

    private static Client jerseyClient = null;

    @Autowired
    private Injector injector;

    @Autowired
    private ApplicationCreator applicationCreator;

    @Autowired
    public void setManagement(ManagementService management) {
        this.management = management;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setMetricFactory() {
        this.metricsFactory = injector.getInstance(MetricsFactory.class);
    }

    MetricsFactory getMetricsFactory() {
        return metricsFactory;
    }

    @Override
    public TokenInfo validateAndReturnTokenInfo(String token, long ttl) throws Exception {
        throw new UnsupportedOperationException("Returning user info not supported from external Usergrid SSO tokens");
    }

    @Override
    public UserInfo validateAndReturnUserInfo(String token, long ttl) throws Exception {
        if (token == null) {
            throw new IllegalArgumentException("ext_access_token must be specified");
        }
        if (ttl == -1) {
            throw new IllegalArgumentException("ttl must be specified");
        }

        com.codahale.metrics.Timer processingTimer = getMetricsFactory().getTimer(
            UsergridExternalProvider.class, SSO_PROCESSING_TIME);

        com.codahale.metrics.Timer.Context timerContext = processingTimer.time();

        try {
            // look up user via UG Central's /management/me endpoint.

            JsonNode accessInfoNode = getMeFromUgCentral(token);

            JsonNode userNode = accessInfoNode.get("user");

            String username = userNode.get("username").asText();

            // if user does not exist locally then we need to fix that

            UserInfo userInfo = management.getAdminUserByUsername(username);
            UUID userId = userInfo == null ? null : userInfo.getUuid();

            if (userId == null) {

                // create local user and and organizations they have on the central Usergrid instance
                logger.info("User {} does not exist locally, creating", username);

                String name = userNode.get("name").asText();
                String email = userNode.get("email").asText();
                String dummyPassword = RandomStringUtils.randomAlphanumeric(40);

                JsonNode orgsNode = userNode.get("organizations");
                Iterator<String> fieldNames = orgsNode.getFieldNames();

                if (!fieldNames.hasNext()) {
                    // no organizations for user exist in response from central Usergrid SSO
                    // so create user's personal organization and use username as organization name
                    fieldNames = Collections.singletonList(username).iterator();
                }

                // create user and any organizations that user is supposed to have

                while (fieldNames.hasNext()) {

                    String orgName = fieldNames.next();

                    if (userId == null) {
//
                        // haven't created user yet so do that now
                        OrganizationOwnerInfo ownerOrgInfo = management.createOwnerAndOrganization(
                            orgName, username, name, email, dummyPassword, true, false);

                        applicationCreator.createSampleFor(ownerOrgInfo.getOrganization());

                        userId = ownerOrgInfo.getOwner().getUuid();
                        userInfo = ownerOrgInfo.getOwner();

                        Counter createdAdminsCounter = getMetricsFactory().getCounter(
                            UsergridExternalProvider.class, SSO_CREATED_LOCAL_ADMINS);
                        createdAdminsCounter.inc();

                        logger.info("Created user {} and org {}", username, orgName);

                    } else {

                        // already created user, so just create an org
                        final OrganizationInfo organization =
                            management.createOrganization(orgName, userInfo, true);

                        applicationCreator.createSampleFor(organization);

                        logger.info("Created user {}'s other org {}", username, orgName);
                    }
                }
            }

            return userInfo;
        } catch (Exception e) {
            timerContext.stop();
            logger.debug("Error validating external token", e);
            throw e;
        }

    }

    @Override
    public Map<String, String> getDecodedTokenDetails(String token) {

        throw new UnsupportedOperationException("Not currently supported with Usergrid external tokens");

    }

    /**
     * Look up Admin User via UG Central's /management/me endpoint.
     *
     * @param extAccessToken Access token issued by UG Central of Admin User
     * @return JsonNode representation of AccessInfo object for Admin User
     * @throws EntityNotFoundException if access_token is not valid.
     */
    private JsonNode getMeFromUgCentral(String extAccessToken) throws EntityNotFoundException {

        // prepare to count tokens validated and rejected

        Counter tokensRejectedCounter = getMetricsFactory().getCounter(
            UsergridExternalProvider.class, SSO_TOKENS_REJECTED);
        Counter tokensValidatedCounter = getMetricsFactory().getCounter(
            UsergridExternalProvider.class, SSO_TOKENS_VALIDATED);

        // create URL of central Usergrid's /management/me endpoint

        String externalUrl = properties.getProperty(USERGRID_CENTRAL_URL).trim();

        // be lenient about trailing slash
        externalUrl = !externalUrl.endsWith("/") ? externalUrl + "/" : externalUrl;
        String me = externalUrl + "management/me?access_token=" + extAccessToken;

        // use our favorite HTTP client to GET /management/me

        Client client = getJerseyClient();
        final org.codehaus.jackson.JsonNode accessInfoNode;
        try {
            accessInfoNode = client.target(me).request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(org.codehaus.jackson.JsonNode.class);

            tokensValidatedCounter.inc();

        } catch (Exception e) {
            // user not found 404
            tokensRejectedCounter.inc();
            String msg = "Cannot find Admin User associated with " + extAccessToken;
            throw new EntityNotFoundException(msg, e);
        }

        return accessInfoNode;
    }

    private Client getJerseyClient() {

        if (jerseyClient == null) {

            synchronized (this) {

                // create HTTPClient and with configured connection pool

                int poolSize = 100; // connections
                final String poolSizeStr = properties.getProperty(CENTRAL_CONNECTION_POOL_SIZE);
                if (poolSizeStr != null) {
                    poolSize = Integer.parseInt(poolSizeStr);
                }

                PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
                connectionManager.setMaxTotal(poolSize);

                int timeout = 20000; // ms
                final String timeoutStr = properties.getProperty(CENTRAL_CONNECTION_TIMEOUT);
                if (timeoutStr != null) {
                    timeout = Integer.parseInt(timeoutStr);
                }

                int readTimeout = 20000; // ms
                final String readTimeoutStr = properties.getProperty(CENTRAL_READ_TIMEOUT);
                if (readTimeoutStr != null) {
                    readTimeout = Integer.parseInt(readTimeoutStr);
                }

                ClientConfig clientConfig = new ClientConfig();
                clientConfig.register(new JacksonFeature());
                clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
                clientConfig.connectorProvider(new ApacheConnectorProvider());

                jerseyClient = ClientBuilder.newClient(clientConfig);
                jerseyClient.property(ClientProperties.CONNECT_TIMEOUT, timeout);
                jerseyClient.property(ClientProperties.READ_TIMEOUT, readTimeout);
            }
        }

        return jerseyClient;

    }
}
