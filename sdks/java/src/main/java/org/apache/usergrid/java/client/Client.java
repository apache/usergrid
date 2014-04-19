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
package org.apache.usergrid.java.client;

import static org.springframework.util.StringUtils.arrayToDelimitedString;
import static org.springframework.util.StringUtils.tokenizeToStringArray;
import static org.apache.usergrid.java.client.utils.JsonUtils.parse;
import static org.apache.usergrid.java.client.utils.ObjectUtils.isEmpty;
import static org.apache.usergrid.java.client.utils.UrlUtils.addQueryParams;
import static org.apache.usergrid.java.client.utils.UrlUtils.encodeParams;
import static org.apache.usergrid.java.client.utils.UrlUtils.path;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.apache.usergrid.java.client.entities.Activity;
import org.apache.usergrid.java.client.entities.Device;
import org.apache.usergrid.java.client.entities.Entity;
import org.apache.usergrid.java.client.entities.Group;
import org.apache.usergrid.java.client.entities.User;
import org.apache.usergrid.java.client.response.ApiResponse;

/**
 * The Client class for accessing the Usergrid API. Start by instantiating this
 * class though the appropriate constructor.
 *
 */
public class Client {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public static boolean FORCE_PUBLIC_API = false;

    // Public API
    public static String PUBLIC_API_URL = "http://api.usergrid.com";

    // Local API of standalone server
    public static String LOCAL_STANDALONE_API_URL = "http://localhost:8080";

    // Local API of Tomcat server in Eclipse
    public static String LOCAL_TOMCAT_API_URL = "http://localhost:8080/ROOT";

    // Local API
    public static String LOCAL_API_URL = LOCAL_STANDALONE_API_URL;

    private String apiUrl = PUBLIC_API_URL;

    private String organizationId;
    private String applicationId;
    private String clientId;
    private String clientSecret;

    private User loggedInUser = null;

    private String accessToken = null;

    private String currentOrganization = null;

    static RestTemplate restTemplate = new RestTemplate();

    /**
     * Default constructor for instantiating a client.
     */
    public Client() {
        init();
    }

    /**
     * Instantiate client for a specific app
     *
     * @param applicationId
     *            the application id or name
     */
    public Client(String organizationId, String applicationId) {
        init();
        this.organizationId = organizationId;
        this.applicationId = applicationId;
    }

    public void init() {

    }

    /**
     * @return the Usergrid API url (default: http://api.usergrid.com)
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * @param apiUrl
     *            the Usergrid API url (default: http://api.usergrid.com)
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * @param apiUrl
     *            the Usergrid API url (default: http://api.usergrid.com)
     * @return Client object for method call chaining
     */
    public Client withApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        return this;
    }


    /**
     * the organizationId to set
     * @param organizationId
     * @return
     */
    public Client withOrganizationId(String organizationId){
        this.organizationId = organizationId;
        return this;
    }



    /**
     * @return the organizationId
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * @param organizationId the organizationId to set
     */
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * @return the application id or name
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * @param applicationId
     *            the application id or name
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }


    /**
     * @param applicationId
     *            the application id or name
     * @return Client object for method call chaining
     */
    public Client withApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    /**
     * @return the client key id for making calls as the application-owner. Not
     *         safe for most mobile use.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId
     *            the client key id for making calls as the application-owner.
     *            Not safe for most mobile use.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * @param clientId
     *            the client key id for making calls as the application-owner.
     *            Not safe for most mobile use.
     * @return Client object for method call chaining
     */
    public Client withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    /**
     * @return the client key id for making calls as the application-owner. Not
     *         safe for most mobile use.
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret
     *            the client key id for making calls as the application-owner.
     *            Not safe for most mobile use.
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * @param clientSecret
     *            the client key id for making calls as the application-owner.
     *            Not safe for most mobile use.
     * @return Client object for method call chaining
     */
    public Client withClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    /**
     * @return the logged-in user after a successful authorizeAppUser request
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * @param loggedInUser
     *            the logged-in user, usually not set by host application
     */
    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    /**
     * @return the OAuth2 access token after a successful authorize request
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @param accessToken
     *            an OAuth2 access token. Usually not set by host application
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * @return the currentOrganization
     */
    public String getCurrentOrganization() {
        return currentOrganization;
    }

    /**
     * @param currentOrganization
     */
    public void setCurrentOrganization(String currentOrganization) {
        this.currentOrganization = currentOrganization;
    }

    /**
     * Low-level HTTP request method. Synchronous, blocks till response or
     * timeout.
     *
     * @param method
     *            HttpMethod method
     * @param cls
     *            class for the return type
     * @param params
     *            parameters to encode as querystring or body parameters
     * @param data
     *            JSON data to put in body
     * @param segments
     *            REST url path segments (i.e. /segment1/segment2/segment3)
     * @return results marshalled into class specified in cls parameter
     */
    public <T> T httpRequest(HttpMethod method, Class<T> cls,
            Map<String, Object> params, Object data, String... segments) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setAccept(Collections
                .singletonList(MediaType.APPLICATION_JSON));
        if (accessToken != null) {
            String auth = "Bearer " + accessToken;
            requestHeaders.set("Authorization", auth);
            log.info("Authorization: " + auth);
        }
        String url = path(apiUrl, segments);

        MediaType contentType = MediaType.APPLICATION_JSON;
        if (method.equals(HttpMethod.POST) && isEmpty(data) && !isEmpty(params)) {
            data = encodeParams(params);
            contentType = MediaType.APPLICATION_FORM_URLENCODED;
        } else {
            url = addQueryParams(url, params);
        }
        requestHeaders.setContentType(contentType);
        HttpEntity<?> requestEntity = null;

        if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)) {
            if (isEmpty(data)) {
                data = JsonNodeFactory.instance.objectNode();
            }
            requestEntity = new HttpEntity<Object>(data, requestHeaders);
        } else {
            requestEntity = new HttpEntity<Object>(requestHeaders);
        }
        log.info("Client.httpRequest(): url: " + url);
        ResponseEntity<T> responseEntity = restTemplate.exchange(url, method,
                requestEntity, cls);
        log.info("Client.httpRequest(): reponse body: "
                + responseEntity.getBody().toString());
        return responseEntity.getBody();
    }

    /**
     * High-level Usergrid API request.
     *
     * @param method
     * @param params
     * @param data
     * @param segments
     * @return
     */
    public ApiResponse apiRequest(HttpMethod method,
            Map<String, Object> params, Object data, String... segments) {
        ApiResponse response = null;
        try {
            response = httpRequest(method, ApiResponse.class, params, data,
                    segments);
            log.info("Client.apiRequest(): Response: " + response);
        } catch (HttpClientErrorException e) {
            log.error("Client.apiRequest(): HTTP error: "
                    + e.getLocalizedMessage());
            response = parse(e.getResponseBodyAsString(), ApiResponse.class);
            if ((response != null) && !isEmpty(response.getError())) {
                log.error("Client.apiRequest(): Response error: "
                        + response.getError());
                if (!isEmpty(response.getException())) {
                    log.error("Client.apiRequest(): Response exception: "
                            + response.getException());
                }
            }
        }
        return response;
    }

    protected void assertValidApplicationId() {
        if (isEmpty(applicationId)) {
            throw new IllegalArgumentException("No application id specified");
        }
    }

    /**
     * Log the user in and get a valid access token.
     *
     * @param email
     * @param password
     * @return non-null ApiResponse if request succeeds, check getError() for
     *         "invalid_grant" to see if access is denied.
     */
    public ApiResponse authorizeAppUser(String email, String password) {
        validateNonEmptyParam(email, "email");
        validateNonEmptyParam(password,"password");
        assertValidApplicationId();
        loggedInUser = null;
        accessToken = null;
        currentOrganization = null;
        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("grant_type", "password");
        formData.put("username", email);
        formData.put("password", password);
        ApiResponse response = apiRequest(HttpMethod.POST, formData, null,
                organizationId, applicationId, "token");
        if (response == null) {
            return response;
        }
        if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
            loggedInUser = response.getUser();
            accessToken = response.getAccessToken();
            currentOrganization = null;
            log.info("Client.authorizeAppUser(): Access token: " + accessToken);
        } else {
            log.info("Client.authorizeAppUser(): Response: " + response);
        }
        return response;
    }

    /**
     * Change the password for the currently logged in user. You must supply the
     * old password and the new password.
     *
     * @param username
     * @param oldPassword
     * @param newPassword
     * @return
     */
    public ApiResponse changePassword(String username, String oldPassword,
            String newPassword) {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("newpassword", newPassword);
        data.put("oldpassword", oldPassword);

        return apiRequest(HttpMethod.POST, null, data, organizationId,  applicationId, "users",
                username, "password");

    }

    /**
     * Log the user in with their numeric pin-code and get a valid access token.
     *
     * @param email
     * @param pin
     * @return non-null ApiResponse if request succeeds, check getError() for
     *         "invalid_grant" to see if access is denied.
     */
    public ApiResponse authorizeAppUserViaPin(String email, String pin) {
        validateNonEmptyParam(email, "email");
        validateNonEmptyParam(pin, "pin");
        assertValidApplicationId();
        loggedInUser = null;
        accessToken = null;
        currentOrganization = null;
        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("grant_type", "pin");
        formData.put("username", email);
        formData.put("pin", pin);
        ApiResponse response = apiRequest(HttpMethod.POST, formData, null,
                organizationId,  applicationId, "token");
        if (response == null) {
            return response;
        }
        if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
            loggedInUser = response.getUser();
            accessToken = response.getAccessToken();
            currentOrganization = null;
            log.info("Client.authorizeAppUser(): Access token: " + accessToken);
        } else {
            log.info("Client.authorizeAppUser(): Response: " + response);
        }
        return response;
    }

    /**
     * Log the user in with their Facebook access token retrived via Facebook
     * OAuth.
     *
     * @param email
     * @param pin
     * @return non-null ApiResponse if request succeeds, check getError() for
     *         "invalid_grant" to see if access is denied.
     */
    public ApiResponse authorizeAppUserViaFacebook(String fb_access_token) {
        validateNonEmptyParam(fb_access_token, "Facebook token");
        assertValidApplicationId();
        loggedInUser = null;
        accessToken = null;
        currentOrganization = null;
        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("fb_access_token", fb_access_token);
        ApiResponse response = apiRequest(HttpMethod.POST, formData, null,
                organizationId,  applicationId, "auth", "facebook");
        if (response == null) {
            return response;
        }
        if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
            loggedInUser = response.getUser();
            accessToken = response.getAccessToken();
            currentOrganization = null;
            log.info("Client.authorizeAppUserViaFacebook(): Access token: "
                    + accessToken);
        } else {
            log.info("Client.authorizeAppUserViaFacebook(): Response: "
                    + response);
        }
        return response;
    }

    /**
     * Log the app in with it's client id and client secret key. Not recommended
     * for production apps.
     *
     * @param email
     * @param pin
     * @return non-null ApiResponse if request succeeds, check getError() for
     *         "invalid_grant" to see if access is denied.
     */
    public ApiResponse authorizeAppClient(String clientId, String clientSecret) {
        validateNonEmptyParam(clientId, "client identifier");
        validateNonEmptyParam(clientSecret, "client secret");
        assertValidApplicationId();
        loggedInUser = null;
        accessToken = null;
        currentOrganization = null;
        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("grant_type", "client_credentials");
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        ApiResponse response = apiRequest(HttpMethod.POST, formData, null,
                organizationId, applicationId, "token");
        if (response == null) {
            return response;
        }
        if (!isEmpty(response.getAccessToken())) {
            loggedInUser = null;
            accessToken = response.getAccessToken();
            currentOrganization = null;
            log.info("Client.authorizeAppClient(): Access token: "
                    + accessToken);
        } else {
            log.info("Client.authorizeAppClient(): Response: " + response);
        }
        return response;
    }

    private void validateNonEmptyParam(Object param, String paramName) {
        if ( isEmpty(param) ) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }

    /**
     * Registers a device using the device's unique device ID.
     *
     * @param context
     * @param properties
     * @return a Device object if success
     */
    public Device registerDevice(UUID deviceId, Map<String, Object> properties) {
        assertValidApplicationId();
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.put("refreshed", System.currentTimeMillis());
        ApiResponse response = apiRequest(HttpMethod.PUT, null, properties,
                organizationId, applicationId, "devices", deviceId.toString());
        return response.getFirstEntity(Device.class);
    }

    /**
     * Registers a device using the device's unique device ID.
     *
     * @param context
     * @param properties
     * @return a Device object if success
     */
    public Device registerDeviceForPush(UUID deviceId,
                                        String notifier,
                                        String token,
                                        Map<String, Object> properties) {
      if (properties == null) {
          properties = new HashMap<String, Object>();
      }
      String notifierKey = notifier + ".notifier.id";
      properties.put(notifierKey, token);
      return registerDevice(deviceId, properties);
    }

    /**
     * Create a new entity on the server.
     *
     * @param entity
     * @return an ApiResponse with the new entity in it.
     */
    public ApiResponse createEntity(Entity entity) {
        assertValidApplicationId();
        if (isEmpty(entity.getType())) {
            throw new IllegalArgumentException("Missing entity type");
        }
        ApiResponse response = apiRequest(HttpMethod.POST, null, entity,
                organizationId, applicationId, entity.getType());
        return response;
    }

    /**
     * Create a new entity on the server from a set of properties. Properties
     * must include a "type" property.
     *
     * @param properties
     * @return an ApiResponse with the new entity in it.
     */
    public ApiResponse createEntity(Map<String, Object> properties) {
        assertValidApplicationId();
        if (isEmpty(properties.get("type"))) {
            throw new IllegalArgumentException("Missing entity type");
        }
        ApiResponse response = apiRequest(HttpMethod.POST, null, properties,
                organizationId, applicationId, properties.get("type").toString());
        return response;
    }

    /**
     * Creates a user.
     *
     * @param username
     *            required
     * @param name
     * @param email
     * @param password
     * @return
     */
    public ApiResponse createUser(String username, String name, String email,
            String password) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", "user");
        if (username != null) {
            properties.put("username", username);
        }
        if (name != null) {
            properties.put("name", name);
        }
        if (email != null) {
            properties.put("email", email);
        }
        if (password != null) {
            properties.put("password", password);
        }
        return createEntity(properties);
    }

    /**
     * Get the groups for the user.
     *
     * @param userId
     * @return a map with the group path as the key and the Group entity as the
     *         value
     */
    public Map<String, Group> getGroupsForUser(String userId) {
        ApiResponse response = apiRequest(HttpMethod.GET, null, null,
                organizationId, applicationId, "users", userId, "groups");
        Map<String, Group> groupMap = new HashMap<String, Group>();
        if (response != null) {
            List<Group> groups = response.getEntities(Group.class);
            for (Group group : groups) {
                groupMap.put(group.getPath(), group);
            }
        }
        return groupMap;
    }

    /**
     * Get a user's activity feed. Returned as a query to ease paging.
     *
     * @param userId
     * @return
     */
    public Query queryActivityFeedForUser(String userId) {
        Query q = queryEntitiesRequest(HttpMethod.GET, null, null,
                organizationId, applicationId, "users", userId, "feed");
        return q;
    }

    /**
     * Posts an activity to a user. Activity must already be created.
     *
     * @param userId
     * @param activity
     * @return
     */
    public ApiResponse postUserActivity(String userId, Activity activity) {
        return apiRequest(HttpMethod.POST, null, activity,  organizationId, applicationId, "users",
                userId, "activities");
    }

    /**
     * Creates and posts an activity to a user.
     *
     * @param verb
     * @param title
     * @param content
     * @param category
     * @param user
     * @param object
     * @param objectType
     * @param objectName
     * @param objectContent
     * @return
     */
    public ApiResponse postUserActivity(String verb, String title,
            String content, String category, User user, Entity object,
            String objectType, String objectName, String objectContent) {
        Activity activity = Activity.newActivity(verb, title, content,
                category, user, object, objectType, objectName, objectContent);
        return postUserActivity(user.getUuid().toString(), activity);
    }

    /**
     * Posts an activity to a group. Activity must already be created.
     *
     * @param userId
     * @param activity
     * @return
     */
    public ApiResponse postGroupActivity(String groupId, Activity activity) {
        return apiRequest(HttpMethod.POST, null, activity, organizationId, applicationId, "groups",
                groupId, "activities");
    }

    /**
     * Creates and posts an activity to a group.
     *
     * @param groupId
     * @param verb
     * @param title
     * @param content
     * @param category
     * @param user
     * @param object
     * @param objectType
     * @param objectName
     * @param objectContent
     * @return
     */
    public ApiResponse postGroupActivity(String groupId, String verb, String title,
            String content, String category, User user, Entity object,
            String objectType, String objectName, String objectContent) {
        Activity activity = Activity.newActivity(verb, title, content,
                category, user, object, objectType, objectName, objectContent);
        return postGroupActivity(groupId, activity);
    }

    /**
     * Post an activity to the stream.
     *
     * @param activity
     * @return
     */
    public ApiResponse postActivity(Activity activity) {
        return createEntity(activity);
    }

    /**
     * Creates and posts an activity to a group.
     *
     * @param verb
     * @param title
     * @param content
     * @param category
     * @param user
     * @param object
     * @param objectType
     * @param objectName
     * @param objectContent
     * @return
     */
    public ApiResponse postActivity(String verb, String title,
            String content, String category, User user, Entity object,
            String objectType, String objectName, String objectContent) {
        Activity activity = Activity.newActivity(verb, title, content,
                category, user, object, objectType, objectName, objectContent);
        return createEntity(activity);
    }

    /**
     * Get a group's activity feed. Returned as a query to ease paging.
     *
     * @param userId
     * @return
     */
    public Query queryActivity() {
        Query q = queryEntitiesRequest(HttpMethod.GET, null, null,
               organizationId, applicationId, "activities");
        return q;
    }



    /**
     * Get a group's activity feed. Returned as a query to ease paging.
     *
     * @param userId
     * @return
     */
    public Query queryActivityFeedForGroup(String groupId) {
        Query q = queryEntitiesRequest(HttpMethod.GET, null, null,
                organizationId,  applicationId, "groups", groupId, "feed");
        return q;
    }

    /**
     * Perform a query request and return a query object. The Query object
     * provides a simple way of dealing with result sets that need to be
     * iterated or paged through.
     *
     * @param method
     * @param params
     * @param data
     * @param segments
     * @return
     */
    public Query queryEntitiesRequest(HttpMethod method,
            Map<String, Object> params, Object data, String... segments) {
        ApiResponse response = apiRequest(method, params, data, segments);
        return new EntityQuery(response, method, params, data, segments);
    }

    /**
     * Perform a query of the users collection.
     *
     * @return
     */
    public Query queryUsers() {
        Query q = queryEntitiesRequest(HttpMethod.GET, null, null,
                organizationId,  applicationId, "users");
        return q;
    }

    /**
     * Perform a query of the users collection using the provided query command.
     * For example: "name contains 'ed'".
     *
     * @param ql
     * @return
     */
    public Query queryUsers(String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", ql);
        Query q = queryEntitiesRequest(HttpMethod.GET, params, null,organizationId,
                applicationId, "users");
        return q;
    }

    /**
     * Perform a query of the users collection within the specified distance of
     * the specified location and optionally using the provided query command.
     * For example: "name contains 'ed'".
     *
     * @param distance
     * @param location
     * @param ql
     * @return
     */
    public Query queryUsersWithinLocation(float distance, float lattitude,
            float longitude, String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql",
                this.makeLocationQL(distance, lattitude, longitude, ql));
        Query q = queryEntitiesRequest(HttpMethod.GET, params, null, organizationId,
                applicationId, "users");
        return q;
    }

    /**
     * Queries the users for the specified group.
     *
     * @param groupId
     * @return
     */
    public Query queryUsersForGroup(String groupId) {
        Query q = queryEntitiesRequest(HttpMethod.GET, null, null, organizationId,
                applicationId, "groups", groupId, "users");
        return q;
    }

    /**
     * Adds a user to the specified groups.
     *
     * @param userId
     * @param groupId
     * @return
     */
    public ApiResponse addUserToGroup(String userId, String groupId) {
        return apiRequest(HttpMethod.POST, null, null, organizationId,  applicationId, "groups",
                groupId, "users", userId);
    }

    /**
     * Creates a group with the specified group path. Group paths can be slash
     * ("/") delimited like file paths for hierarchical group relationships.
     *
     * @param groupPath
     * @return
     */
    public ApiResponse createGroup(String groupPath) {
        return createGroup(groupPath, null);
    }

    /**
     * Creates a group with the specified group path and group title. Group
     * paths can be slash ("/") delimited like file paths for hierarchical group
     * relationships.
     *
     * @param groupPath
     * @param groupTitle
     * @return
     */
    public ApiResponse createGroup(String groupPath, String groupTitle) {
     return createGroup(groupPath, groupTitle, null);
    }

    /**
     * Create a group with a path, title and name
     * @param groupPath
     * @param groupTitle
     * @param groupName
     * @return
     */
    public ApiResponse createGroup(String groupPath, String groupTitle, String groupName){
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("type", "group");
        data.put("path", groupPath);

        if (groupTitle != null) {
            data.put("title", groupTitle);
        }

        if(groupName != null){
            data.put("name", groupName);
        }

        return apiRequest(HttpMethod.POST, null, data,  organizationId, applicationId, "groups");
    }

    /**
     * Perform a query of the users collection using the provided query command.
     * For example: "name contains 'ed'".
     *
     * @param ql
     * @return
     */
    public Query queryGroups(String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", ql);
        Query q = queryEntitiesRequest(HttpMethod.GET, params, null, organizationId,
                applicationId, "groups");
        return q;
    }



    /**
     * Connect two entities together.
     *
     * @param connectingEntityType
     * @param connectingEntityId
     * @param connectionType
     * @param connectedEntityId
     * @return
     */
    public ApiResponse connectEntities(String connectingEntityType,
            String connectingEntityId, String connectionType,
            String connectedEntityId) {
        return apiRequest(HttpMethod.POST, null, null,  organizationId, applicationId,
                connectingEntityType, connectingEntityId, connectionType,
                connectedEntityId);
    }

    /**
     * Disconnect two entities.
     *
     * @param connectingEntityType
     * @param connectingEntityId
     * @param connectionType
     * @param connectedEntityId
     * @return
     */
    public ApiResponse disconnectEntities(String connectingEntityType,
            String connectingEntityId, String connectionType,
            String connectedEntityId) {
        return apiRequest(HttpMethod.DELETE, null, null,  organizationId, applicationId,
                connectingEntityType, connectingEntityId, connectionType,
                connectedEntityId);
    }

    /**
     * Query the connected entities.
     *
     * @param connectingEntityType
     * @param connectingEntityId
     * @param connectionType
     * @param ql
     * @return
     */
    public Query queryEntityConnections(String connectingEntityType,
            String connectingEntityId, String connectionType, String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", ql);
        Query q = queryEntitiesRequest(HttpMethod.GET, params, null,
                organizationId, applicationId, connectingEntityType, connectingEntityId,
                connectionType);
        return q;
    }

    protected String makeLocationQL(float distance, double lattitude,
            double longitude, String ql) {
        String within = String.format("within %d of %d , %d", distance,
                lattitude, longitude);
        ql = ql == null ? within : within + " and " + ql;
        return ql;
    }

    /**
     * Query the connected entities within distance of a specific point.
     *
     * @param connectingEntityType
     * @param connectingEntityId
     * @param connectionType
     * @param distance
     * @param latitude
     * @param longitude
     * @return
     */
    public Query queryEntityConnectionsWithinLocation(
            String connectingEntityType, String connectingEntityId,
            String connectionType, float distance, float lattitude,
            float longitude, String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", makeLocationQL(distance, lattitude, longitude, ql));
        Query q = queryEntitiesRequest(HttpMethod.GET, params, null, organizationId,
                applicationId, connectingEntityType, connectingEntityId,
                connectionType);
        return q;
    }

    public interface Query {

        public ApiResponse getResponse();

        public boolean more();

        public Query next();

    }

    /**
     * Query object
     *
     */
    private class EntityQuery implements Query {
        final HttpMethod method;
        final Map<String, Object> params;
        final Object data;
        final String[] segments;
        final ApiResponse response;

        private EntityQuery(ApiResponse response, HttpMethod method,
                Map<String, Object> params, Object data, String[] segments) {
            this.response = response;
            this.method = method;
            this.params = params;
            this.data = data;
            this.segments = segments;
        }

        private EntityQuery(ApiResponse response, EntityQuery q) {
            this.response = response;
            method = q.method;
            params = q.params;
            data = q.data;
            segments = q.segments;
        }

        /**
         * @return the api response of the last request
         */
        public ApiResponse getResponse() {
            return response;
        }

        /**
         * @return true if the server indicates more results are available
         */
        public boolean more() {
            if ((response != null) && (response.getCursor() != null)
                    && (response.getCursor().length() > 0)) {
                return true;
            }
            return false;
        }

        /**
         * Performs a request for the next set of results
         *
         * @return query that contains results and where to get more from.
         */
        public Query next() {
            if (more()) {
                Map<String, Object> nextParams = null;
                if (params != null) {
                    nextParams = new HashMap<String, Object>(params);
                } else {
                    nextParams = new HashMap<String, Object>();
                }
                nextParams.put("cursor", response.getCursor());
                ApiResponse nextResponse = apiRequest(method, nextParams, data,
                        segments);
                return new EntityQuery(nextResponse, this);
            }
            return null;
        }

    }

    private String normalizeQueuePath(String path) {
        return arrayToDelimitedString(
                tokenizeToStringArray(path, "/", true, true), "/");
    }

    public ApiResponse postMessage(String path, Map<String, Object> message) {
        return apiRequest(HttpMethod.POST, null, message, organizationId,  applicationId,
                "queues", normalizeQueuePath(path));
    }

    public ApiResponse postMessage(String path,
            List<Map<String, Object>> messages) {
        return apiRequest(HttpMethod.POST, null, messages,  organizationId, applicationId,
                "queues", normalizeQueuePath(path));
    }

    public enum QueuePosition {
        START("start"), END("end"), LAST("last"), CONSUMER("consumer");

        private final String shortName;

        QueuePosition(String shortName) {
            this.shortName = shortName;
        }

        static Map<String, QueuePosition> nameMap = new ConcurrentHashMap<String, QueuePosition>();

        static {
            for (QueuePosition op : EnumSet.allOf(QueuePosition.class)) {
                if (op.shortName != null) {
                    nameMap.put(op.shortName, op);
                }
            }
        }

        public static QueuePosition find(String s) {
            if (s == null) {
                return null;
            }
            return nameMap.get(s);
        }

        @Override
        public String toString() {
            return shortName;
        }
    }

    public ApiResponse getMessages(String path, String consumer, UUID last,
            Long time, Integer prev, Integer next, Integer limit,
            QueuePosition pos, Boolean update, Boolean sync) {
        Map<String, Object> params = new HashMap<String, Object>();
        if (consumer != null) {
            params.put("consumer", consumer);
        }
        if (last != null) {
            params.put("last", last);
        }
        if (time != null) {
            params.put("time", time);
        }
        if (prev != null) {
            params.put("prev", prev);
        }
        if (next != null) {
            params.put("next", next);
        }
        if (limit != null) {
            params.put("limit", limit);
        }
        if (pos != null) {
            params.put("pos", pos.toString());
        }
        if (update != null) {
            params.put("update", update);
        }
        if (sync != null) {
            params.put("synchronized", sync);
        }
        return apiRequest(HttpMethod.GET, params, null,  organizationId, applicationId,
                "queues", normalizeQueuePath(path));
    }

    public ApiResponse addSubscriber(String publisherQueue,
            String subscriberQueue) {
        return apiRequest(HttpMethod.POST, null, null, organizationId,  applicationId, "queues",
                normalizeQueuePath(publisherQueue), "subscribers",
                normalizeQueuePath(subscriberQueue));
    }

    public ApiResponse removeSubscriber(String publisherQueue,
            String subscriberQueue) {
        return apiRequest(HttpMethod.DELETE, null, null, organizationId,  applicationId,
                "queues", normalizeQueuePath(publisherQueue), "subscribers",
                normalizeQueuePath(subscriberQueue));
    }

    private class QueueQuery implements Query {
        final HttpMethod method;
        final Map<String, Object> params;
        final Object data;
        final String queuePath;
        final ApiResponse response;

        private QueueQuery(ApiResponse response, HttpMethod method,
                Map<String, Object> params, Object data, String queuePath) {
            this.response = response;
            this.method = method;
            this.params = params;
            this.data = data;
            this.queuePath = normalizeQueuePath(queuePath);
        }

        private QueueQuery(ApiResponse response, QueueQuery q) {
            this.response = response;
            method = q.method;
            params = q.params;
            data = q.data;
            queuePath = q.queuePath;
        }

        /**
         * @return the api response of the last request
         */
        public ApiResponse getResponse() {
            return response;
        }

        /**
         * @return true if the server indicates more results are available
         */
        public boolean more() {
            if ((response != null) && (response.getCursor() != null)
                    && (response.getCursor().length() > 0)) {
                return true;
            }
            return false;
        }

        /**
         * Performs a request for the next set of results
         *
         * @return query that contains results and where to get more from.
         */
        public Query next() {
            if (more()) {
                Map<String, Object> nextParams = null;
                if (params != null) {
                    nextParams = new HashMap<String, Object>(params);
                } else {
                    nextParams = new HashMap<String, Object>();
                }
                nextParams.put("start", response.getCursor());
                ApiResponse nextResponse = apiRequest(method, nextParams, data,
                        queuePath);
                return new QueueQuery(nextResponse, this);
            }
            return null;
        }

    }

    public Query queryQueuesRequest(HttpMethod method,
            Map<String, Object> params, Object data, String queuePath) {
        ApiResponse response = apiRequest(method, params, data, queuePath);
        return new QueueQuery(response, method, params, data, queuePath);
    }

}
