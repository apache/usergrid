/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.android.sdk;

import static org.apache.usergrid.android.sdk.utils.ObjectUtils.isEmpty;
import static org.apache.usergrid.android.sdk.utils.UrlUtils.addQueryParams;
import static org.apache.usergrid.android.sdk.utils.UrlUtils.encodeParams;
import static org.apache.usergrid.android.sdk.utils.UrlUtils.path;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.apache.usergrid.android.sdk.URLConnectionFactory;
import org.apache.usergrid.android.sdk.callbacks.ApiResponseCallback;
import org.apache.usergrid.android.sdk.callbacks.ClientAsyncTask;
import org.apache.usergrid.android.sdk.callbacks.GroupsRetrievedCallback;
import org.apache.usergrid.android.sdk.callbacks.QueryResultsCallback;
import org.apache.usergrid.android.sdk.entities.Activity;
import org.apache.usergrid.android.sdk.entities.Collection;
import org.apache.usergrid.android.sdk.entities.Device;
import org.apache.usergrid.android.sdk.entities.Entity;
import org.apache.usergrid.android.sdk.entities.Group;
import org.apache.usergrid.android.sdk.entities.Message;
import org.apache.usergrid.android.sdk.entities.User;
import org.apache.usergrid.android.sdk.response.ApiResponse;
import org.apache.usergrid.android.sdk.utils.DeviceUuidFactory;
import org.apache.usergrid.android.sdk.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;


/**
 * The UGClient class for accessing the Usergrid API. Start by instantiating this
 * class though the appropriate constructor. Most calls to the API will be handled
 * by the methods in this class.
 * 
 * @see org.apache.usergrid.android.sdk.UGClient
 * @see <a href="http://apigee.com/docs/app-services/content/installing-apigee-sdk-android">Usergrid SDK install guide</a>
 */
public class UGClient {

    /**
     * Most current version of the Usergrid Android SDK
     */
    public static final String SDK_VERSION  = "0.0.8";
    /**
     * Platform type of this SDK
     */
    public static final String SDK_TYPE     = "Android";

    /**
     * @y.exclude
     */
    public static final String OPTION_KEY_BASE_URL = "baseURL";

    /**
     * @y.exclude
     */
    public static boolean FORCE_PUBLIC_API = false;

    /** 
     * Public API
     */
    public static String PUBLIC_API_URL = "https://api.usergrid.com";

    /** 
     * Local API of standalone server
     */
    public static String LOCAL_STANDALONE_API_URL = "http://localhost:8080";

    /**
     * Local API of Tomcat server in Eclipse
     */
    public static String LOCAL_TOMCAT_API_URL = "http://localhost:8080/ROOT";

    /**
     * Local API
     */
    public static String LOCAL_API_URL = LOCAL_STANDALONE_API_URL;

    /**
     * Standard HTTP methods use in generic request methods
     * @see apiRequest 
     * @see doHttpRequest
     */
    protected static final String HTTP_METHOD_DELETE = "DELETE";
    /**
     * Standard HTTP methods use in generic request methods
     * @see apiRequest 
     * @see doHttpRequest
     */
    protected static final String HTTP_METHOD_GET    = "GET";
    /**
     * Standard HTTP methods use in generic request methods
     * @see apiRequest 
     * @see doHttpRequest
     */
    protected static final String HTTP_METHOD_POST   = "POST";
    /**
     * Standard HTTP methods use in generic request methods
     * @see apiRequest 
     * @see doHttpRequest
     */
    protected static final String HTTP_METHOD_PUT    = "PUT";
    
    protected static final String LOGGING_TAG    = "UGCLIENT";

    private String apiUrl = PUBLIC_API_URL;

    private String organizationId;
    private String applicationId;
    private String clientId;
    private String clientSecret;

    private User loggedInUser = null;

    private String accessToken = null;

    private String currentOrganization = null;
    private URLConnectionFactory urlConnectionFactory = null;
    
    private LocationManager locationManager;
    private UUID deviceID;
    
    /**
    * Interface for EntityQuery and QueueQuery
    */
    public interface Query {

        public ApiResponse getResponse();

        public boolean more();

        public Query next();

    }

    /**
     * @y.exclude
     */
    public static boolean isUuidValid(UUID uuid) {
    	return( uuid != null );
    }

    protected static String arrayToDelimitedString(String[] arrayOfStrings, String delimiter) {
    	StringBuilder sb = new StringBuilder();
    	
    	for( int i = 0; i < arrayOfStrings.length; ++i ) {
    		if( i > 0 ) {
    			sb.append(delimiter);
    		}
    		
    		sb.append(arrayOfStrings[i]);
    	}
    	
    	return sb.toString();
    }
    
    protected static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
    	if (str == null) {
    		return null;
    	}

    	StringTokenizer st = new StringTokenizer(str, delimiters);
    	
    	int numTokens = st.countTokens();
    	List<String> listTokens;
    	
    	if( numTokens > 0 ) {

    		listTokens = new ArrayList<String>(numTokens);

    		while (st.hasMoreTokens()) {

    			String token = st.nextToken();

    			if (trimTokens) {
    				token = token.trim();
    			}

    			if (!ignoreEmptyTokens || token.length() > 0) {
    				listTokens.add(token);
    			}
    		}
    	} else {
    		listTokens = new ArrayList<String>();
    	}
    	
    	return listTokens.toArray(new String[listTokens.size()]);
    }
    

    /****************** CONSTRUCTORS ***********************/
    /****************** CONSTRUCTORS ***********************/

    /**
     * @y.exclude
     */
    public UGClient() {
        init();
    }

    /**
     * Instantiate a data client for a specific app. This is used to call most 
     * SDK methods.
     * 
     * @param  organizationId  the Usergrid organization name
     * @param  applicationId  the Usergrid application id or name
     */
    public UGClient(String organizationId, String applicationId) {
        init();
        this.organizationId = organizationId;
        this.applicationId = applicationId;        
    }

    /**
     * Instantiate a data client for a specific app with a base URL other than the default
     * api.usergrid.com. This is used to call most SDK methods.
     * 
     * @param  organizationId  the Usergrid organization name
     * @param  applicationId  the Usergrid application id or name
     * @param  baseURL  the base URL to use for all API calls
     */
    public UGClient(String organizationId, String applicationId, String baseURL) {
        init();
        this.organizationId = organizationId;
        this.applicationId = applicationId;
        
        if( baseURL != null ) {
        	this.setApiUrl(baseURL);
        }
    }

    public void init() {
    }
    

    /****************** ACCESSORS/MUTATORS ***********************/
    /****************** ACCESSORS/MUTATORS ***********************/

    /**
     * Sets a new URLConnectionFactory object in the UGClient
     *
     * @param  urlConnectionFactory  a new URLConnectionFactory object
     * @y.exclude
     */
    public void setUrlConnectionFactory(URLConnectionFactory urlConnectionFactory) {
    	this.urlConnectionFactory = urlConnectionFactory;
    }

    /**
     * @return the Usergrid API url (default: http://api.usergrid.com)
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * Sets the base URL for API requests
     *
     * @param apiUrl the API base url to be set (default: http://api.usergrid.com)
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * Sets the base URL for API requests and returns the updated UGClient object
     *
     * @param apiUrl the Usergrid API url (default: http://api.usergrid.com)
     * @return UGClient object for method call chaining
     */
    public UGClient withApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        return this;
    }
    
    
    /**
     * Sets the Usergrid organization ID and returns the UGClient object
     *
     * @param  organizationId  the organizationId to set
     * @return  the updated UGClient object
     */
    public UGClient withOrganizationId(String organizationId){
        this.organizationId = organizationId;
        return this;
    }
    
    

    /**
     * Gets the current Usergrid organization ID set in the UGClient
     *
     * @return the current organizationId
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the Usergrid organization ID
     *
     * @param  organizationId  the organizationId to set     
     */
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Gets the current Usergrid application ID set in the UGClient
     *
     * @return the current organizationId or name
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets the Usergrid application Id
     *
     * @param  applicationId  the application id or name
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
   

    /**
     * Sets the Usergrid application ID and returns the UGClient object
     *
     * @param  applicationId  the application ID to set
     * @return  the updated UGClient object
     */
    public UGClient withApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    /**
     * Gets the application (not organization) client ID credential for making calls as the 
     * application-owner. Not safe for most mobile use. 
     * @return the client id 
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the application (not organization) client ID credential, used for making 
     * calls as the application-owner. Not safe for most mobile use.
     * @param clientId the client id 
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Sets the client ID credential in the UGClient object. Not safe for most mobile use.
     *
     * @param clientId the client key id
     * @return UGClient object for method call chaining
     */
    public UGClient withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    /**
     * Gets the application (not organization) client secret credential for making calls as the 
     * application-owner. Not safe for most mobile use. 
     * @return the client secret 
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Sets the application (not organization) client secret credential, used for making 
     * calls as the application-owner. Not safe for most mobile use.
     *
     * @param clientSecret the client secret 
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Sets the client secret credential in the UGClient object. Not safe for most mobile use.
     *
     * @param clientSecret the client secret
     * @return UGClient object for method call chaining
     */
    public UGClient withClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    /**
     * Gets the UUID of the logged-in user after a successful authorizeAppUser request
     * @return the UUID of the logged-in user
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Sets the UUID of the logged-in user. Usually not set by host application
     * @param loggedInUser the UUID of the logged-in user
     */
    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    /**
     * Gets the OAuth2 access token for the current logged-in user after a 
     * successful authorize request
     *
     * @return the OAuth2 access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Saves the OAuth2 access token in the UGClient after a successful authorize
     * request. Usually not set by host application.
     *
     * @param accessToken an OAuth2 access token
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Gets the current organization from UGClient 
     *
     * @return the currentOrganization
     */
    public String getCurrentOrganization() {
        return currentOrganization;
    }

    /**     
     * Sets the current organizanization from UGClient 
     *
     * @param currentOrganization The organization this data client should use.
     */
    public void setCurrentOrganization(String currentOrganization) {
        this.currentOrganization = currentOrganization;
    }

    /****************** LOGGING ***********************/
    /****************** LOGGING ***********************/


    /**
     * Logs a trace-level logging message with tag 'DATA_CLIENT'
     *
     * @param   logMessage  the message to log
     */
    public void logTrace(String logMessage) {
        if( logMessage != null ) {
            Log.v(LOGGING_TAG,logMessage);
        }
    }
    
    /**
     * Logs a debug-level logging message with tag 'DATA_CLIENT'
     *
     * @param   logMessage  the message to log
     */
    public void logDebug(String logMessage) {
        if( logMessage != null ) {
            Log.d(LOGGING_TAG,logMessage);
        }
    }
    
    /**
     * Logs an info-level logging message with tag 'DATA_CLIENT'
     *
     * @param   logMessage  the message to log
     */
    public void logInfo(String logMessage) {
        if( logMessage != null ) {
            Log.i(LOGGING_TAG,logMessage);
        }
    }
    
    /**
     * Logs a warn-level logging message with tag 'DATA_CLIENT'
     *
     * @param   logMessage  the message to log
     */
    public void logWarn(String logMessage) {
        if( logMessage != null ) {
            Log.w(LOGGING_TAG,logMessage);
        }
    }
    
    /**
     * Logs an error-level logging message with tag 'DATA_CLIENT'
     *
     * @param   logMessage  the message to log
     */
    public void logError(String logMessage) {
        if( logMessage != null ) {
            Log.e(LOGGING_TAG,logMessage);
        }
    }

    /**
     * Logs a debug-level logging message with tag 'DATA_CLIENT'
     *
     * @param   logMessage  the message to log
     */
    public void writeLog(String logMessage) {
        if( logMessage != null ) {
            //TODO: do we support different log levels in this class?
            Log.d(LOGGING_TAG, logMessage);
        }
    }
    
    /****************** API/HTTP REQUEST ***********************/
    /****************** API/HTTP REQUEST ***********************/

    /**
     *  Forms and initiates a raw synchronous http request and processes the response.
     *
     *  @param  httpMethod the HTTP method in the format: 
     *      HTTP_METHOD_<method_name> (e.g. HTTP_METHOD_POST)
     *  @param  params the URL parameters to append to the request URL
     *  @param  data the body of the request
     *  @param  segments  additional URL path segments to append to the request URL 
     *  @return  ApiResponse object
     */
	public ApiResponse doHttpRequest(String httpMethod, Map<String, Object> params, Object data, String... segments) {
		
        ApiResponse response = null;
		OutputStream out = null;
		InputStream in = null;
		HttpURLConnection conn = null;
		
		String urlAsString = path(apiUrl, segments);
		
		try {
	        String contentType = "application/json";
	        if (httpMethod.equals(HTTP_METHOD_POST) && isEmpty(data) && !isEmpty(params)) {
	            data = encodeParams(params);
	            contentType = "application/x-www-form-urlencoded";
	        } else {
	            urlAsString = addQueryParams(urlAsString, params);
	        }

			//logTrace("Invoking " + httpMethod + " to '" + urlAsString + "'");

			URL url = new URL(urlAsString);
			conn = (HttpURLConnection) url.openConnection();
            
			conn.setRequestMethod(httpMethod);
			conn.setRequestProperty("Content-Type", contentType);
			conn.setUseCaches(false);
			
			if  ((accessToken != null) && (accessToken.length() > 0)) {
				String authStr = "Bearer " + accessToken;
				conn.setRequestProperty("Authorization", authStr);
			}

			conn.setDoInput(true);
			
	        if (httpMethod.equals(HTTP_METHOD_POST) || httpMethod.equals(HTTP_METHOD_PUT)) {
	            if (isEmpty(data)) {
	                data = JsonNodeFactory.instance.objectNode();
	            }
	            
	            String dataAsString = null;
	            
	            if ((data != null) && (!(data instanceof String))) {
	            	ObjectMapper objectMapper = new ObjectMapper();
	    			objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	    			dataAsString = objectMapper.writeValueAsString(data);
	            } else {
	            	dataAsString = (String) data;
	            }
	            
	    		//logTrace("Posting/putting data: '" + dataAsString + "'");

				byte[] dataAsBytes = dataAsString.getBytes();

				conn.setRequestProperty("Content-Length", Integer.toString(dataAsBytes.length));
				conn.setDoOutput(true);

				out = conn.getOutputStream();
				out.write(dataAsBytes);
				out.flush();
				out.close();
				out = null;
	        }
	        
			in = conn.getInputStream();
			if( in != null ) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				StringBuilder sb = new StringBuilder();
				String line;
				
				while( (line = reader.readLine()) != null ) {
					sb.append(line);
					sb.append('\n');
				}
				
				String responseAsString = sb.toString();

				//logTrace("response from server: '" + responseAsString + "'");
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                response = (ApiResponse) objectMapper.readValue(responseAsString, ApiResponse.class);
				response.setRawResponse(responseAsString);

				response.setUGClient(this);
			} else {
				response = null;
				logTrace("no response body from server");
			}

			//final int responseCode = conn.getResponseCode();
			//logTrace("responseCode from server = " + responseCode);
		}
		catch(Exception e) {
			logError("Error " + httpMethod + " to '" + urlAsString + "'" );
			if( e != null ) {
				e.printStackTrace();
				logError(e.getLocalizedMessage());
			}
			response = null;
		}
		catch(Throwable t) {
			logError("Error " + httpMethod + " to '" + urlAsString + "'" );
			if( t != null ) {
				t.printStackTrace();
				logError(t.getLocalizedMessage());
			}
			response = null;
		}
		finally {
			try {
				if( out != null ) {
					out.close();
				}
			
				if( in != null ) {
					in.close();
				}
				
				if( conn != null ) {
					conn.disconnect();
				}
			} catch(Exception ignored) {
			}
		}
		
	    return response;
	}


    /**
     * High-level synchronous API request. Implements the http request
     * for most SDK methods by calling 
     * {@link #doHttpRequest(String,Map,Object,String...)}
     * 
     *  @param  httpMethod the HTTP method in the format: 
     *      HTTP_METHOD_<method_name> (e.g. HTTP_METHOD_POST)
     *  @param  params the URL parameters to append to the request URL
     *  @param  data the body of the request
     *  @param  segments  additional URL path segments to append to the request URL 
     *  @return  ApiResponse object
     */
    public ApiResponse apiRequest(String httpMethod,
            Map<String, Object> params, Object data, String... segments) {
        ApiResponse response = null;
        
        response = doHttpRequest(httpMethod, params, data, segments);
        
        if( (response == null) ) {
        	logError("doHttpRequest returned null");
        }
        
        return response;
    }

    protected void assertValidApplicationId() {
        if (isEmpty(applicationId)) {
            throw new IllegalArgumentException("No application id specified");
        }
    }

    /****************** ROLES/PERMISSIONS ***********************/
    /****************** ROLES/PERMISSIONS ***********************/

    /**
     * Assigns permissions to the specified user, group, or role.
     * 
     * @param entityType the entity type of the entity the permissions are being assigned to. 'user', 'group' and 'role' are valid.
     * @param entityID the UUID of 'name' property of the entity the permissions are being assigned to.
     * @param permissions a comma-separated list of the permissions to be assigned in the format: <operations>:<path>, e.g. get, put, post, delete: /users
     * @throws IllegalArgumentException thrown if an entityType other than 'group' or 'user' is passed to the method
     * @return ApiResponse object
     */
    public ApiResponse assignPermissions(String entityType, String entityID, String permissions) {

        if (!entityType.substring(entityType.length() - 1 ).equals("s")) {
            entityType += "s";
        }
        
        if (!validateTypeForPermissionsAndRoles(entityType, "permission")) {
            throw new IllegalArgumentException("Permissions can only be assigned to group, user, or role entities");
        }

        Map<String, Object> data = new HashMap<String, Object>();
        if (permissions != null){
            data.put("permission", permissions);
        }

        return apiRequest(HTTP_METHOD_POST, null, data, organizationId,  applicationId, entityType,
                entityID, "permissions");

    }

    /**
     * Assigns permissions to the specified user, group, or role. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param entityType the entity type of the entity the permissions are being assigned to. 'user', 'group' and 'role' are valid.
     * @param entityID the UUID of 'name' property of the entity the permissions are being assigned to.
     * @param permissions a comma-separated list of the permissions to be assigned in the format: <operations>:<path>, e.g. get, put, post, delete: /users     
     * @param  callback  an ApiResponseCallback to handle the async response
     */
    public void assignPermissionsAsync(final String entityType,
            final String entityID, final String permissions, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return assignPermissions(entityType, entityID, permissions);
            }
        }).execute();
    }

    /**
     * Removes permissions from the specified user, group or role.
     * 
     * @param entityType the entity type of the entity the permissions are being removed from. 'user', 'group' and 'role' are valid.
     * @param entityID the UUID of 'name' property of the entity the permissions are being removed from.
     * @param permissions a comma-separated list of the permissions to be removed in the format: <operations>:<path>, e.g. get, put, post, delete: /users
     * @throws IllegalArgumentException thrown if an entityType other than 'group' or 'user' is passed to the method
     * @return ApiResponse object
     */
    public ApiResponse removePermissions(String entityType, String entityID, String permissions) {

        if (!validateTypeForPermissionsAndRoles(entityType, "permission")) {
            throw new IllegalArgumentException("Permissions can only be assigned to group, user, or role entities");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        if (permissions != null){
            params.put("permission", permissions);
        }
        
        return apiRequest(HTTP_METHOD_DELETE, params, null, organizationId,  applicationId, entityType,
                entityID, "permissions");

    }

    /**
     * Removes permissions from the specified user, group or role. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param entityType the entity type of the entity the permissions are being removed from. 'user', 'group', and 'role' are valid.
     * @param entityID the UUID of 'name' property of the entity the permissions are being removed from.
     * @param permissions a comma-separated list of the permissions to be removed in the format: <operations>:<path>, e.g. get, put, post, delete: /users     
     * @param  callback  an ApiResponseCallback to handle the async response
     */
    public void removePermissionsAsync(final String entityType,
            final String entityID, final String permissions, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return removePermissions(entityType, entityID, permissions);
            }
        }).execute();
    }

    /**
     * Creates a new role and assigns permissions to it.
     * 
     * @param roleName the name of the new role
     * @param permissions a comma-separated list of the permissions to be assigned in the format: <operations>:<path>, e.g. get, put, post, delete: /users
     * @return ApiResponse object
     */
    public ApiResponse createRole(String roleName, String permissions) {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", "role");
        properties.put("name", roleName);

        ApiResponse response = this.createEntity(properties);

        String uuid = null;

        if (response.getEntityCount() == 1){
            uuid = response.getFirstEntity().getUuid().toString();
        }

        return assignPermissions("role", uuid, permissions);

    }

    /**
     * Creates a new role and assigns permissions to it.
     * 
     * @param roleName the name of the new role
     * @param permissions a comma-separated list of the permissions to be assigned in the format: <operations>:<path>, e.g. get, put, post, delete: /users
     * @param  callback  an ApiResponseCallback to handle the async response     
     */
    public void createRoleAsync(final String roleName, final String permissions, 
                  final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return createRole(roleName, permissions);
            }
        }).execute();
    }

    /**
     * Assigns a role to a user or group entity.
     * 
     * @param roleName the name of the role to be assigned to the entity
     * @param entityType the entity type of the entity the role is being assigned to. 'user' and 'group' are valid.
     * @param entityID the UUID or 'name' property of the entity the role is being assigned to.     
     * @throws IllegalArgumentException thrown if an entityType other than 'group' or 'user' is passed to the method
     * @return ApiResponse object
     */
    public ApiResponse assignRole(String roleName, String entityType, String entityID) {

        if (!entityType.substring(entityType.length() - 1 ).equals("s")) {
            entityType += "s";
        }

        if (!validateTypeForPermissionsAndRoles(entityType, "role")) {
            throw new IllegalArgumentException("Permissions can only be assigned to a group or user");
        }

        return apiRequest(HTTP_METHOD_POST, null, null, organizationId,  applicationId, "roles", roleName, 
                      entityType, entityID);

    }

    /**
     * Assigns a role to a user or group entity. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param roleName the name of the role to be assigned to the entity
     * @param entityType the entity type of the entity the role is being assigned to. 'user' and 'group' are valid.
     * @param entityID the UUID or 'name' property of the entity the role is being removed from.     
     * @param callback  an ApiResponseCallback to handle the async response
     */
    public void assignRoleAsync(final String roleName, final String entityType,
            final String entityID, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return assignRole(roleName, entityType, entityID);
            }
        }).execute();
    }

    /**
     * Removes a role from a user or group entity.
     * 
     * @param roleName the name of the role to be removed from the entity
     * @param entityType the entity type of the entity the role is being removed from. 'user' and 'group' are valid.
     * @param entityID the UUID or 'name' property of the entity the role is being removed from.     
     * @throws IllegalArgumentException thrown if an entityType other than 'group' or 'user' is passed to the method
     * @return ApiResponse object
     */
    public ApiResponse removeRole(String roleName, String entityType, String entityID) {

        if (!entityType.substring(entityType.length() - 1 ).equals("s")) {
            entityType += "s";
        }

        if (!validateTypeForPermissionsAndRoles(entityType, "role")) {
            throw new IllegalArgumentException("Permissions can only be removed from a group or user");
        }

        return apiRequest(HTTP_METHOD_DELETE, null, null, organizationId,  applicationId, "roles", roleName, 
                      entityType, entityID);

    }

    /**
     * Removes a role from a user or group entity. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param roleName the name of the role to be removed from the entity
     * @param entityType the entity type of the entity the role is being removed from. 'user' and 'group' are valid.
     * @param entityID the UUID or 'name' property of the entity the role is being removed from.     
     * @param callback  an ApiResponseCallback to handle the async response
     */
    public void removeRoleAsync(final String roleName, final String entityType,
            final String entityID, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return removeRole(roleName, entityType, entityID);
            }
        }).execute();
    }

    /**
     * Checks if a permission or role can be assigned to an entity
     * @y.exclude
     */
    private Boolean validateTypeForPermissionsAndRoles(String type, String permissionOrRole){
        ArrayList<String> validTypes = new ArrayList<String>();        
        validTypes.add("groups");        
        validTypes.add("users");
        
        if (permissionOrRole.equals("permission")){
            validTypes.add("roles");
        }

        return validTypes.contains(type);
    }

    /****************** LOG IN/LOG OUT/OAUTH ***********************/
    /****************** LOG IN/LOG OUT/OAUTH ***********************/

    /**
     * Logs the user in and get a valid access token.
     * 
     * @param usernameOrEmail the username or email associated with the user entity in Usergrid
     * @param password the user's Usergrid password
     * @return non-null ApiResponse if request succeeds, check getError() for
     *         "invalid_grant" to see if access is denied.
     */
    public ApiResponse authorizeAppUser(String usernameOrEmail, String password) {
        validateNonEmptyParam(usernameOrEmail, "email");
        validateNonEmptyParam(password,"password");
        assertValidApplicationId();
        loggedInUser = null;
        accessToken = null;
        currentOrganization = null;
        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("grant_type", "password");
        formData.put("username", usernameOrEmail);
        formData.put("password", password);
        ApiResponse response = apiRequest(HTTP_METHOD_POST, formData, null,
                organizationId, applicationId, "token");
        if (response == null) {
            return response;
        }
        if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
            loggedInUser = response.getUser();
            accessToken = response.getAccessToken();
            currentOrganization = null;
            logInfo("authorizeAppUser(): Access token: " + accessToken);
        } else {
            logInfo("authorizeAppUser(): Response: " + response);
        }
        return response;
    }

	/**
	 * Log the user in and get a valid access token. Executes asynchronously in
	 * background and the callbacks are called in the UI thread.
	 * 
	 * @param  usernameOrEmail  the username or email associated with the user entity in Usergrid
     * @param  password  the users Usergrid password
     * @param  callback  an ApiResponseCallback to handle the async response
	 */
	public void authorizeAppUserAsync(final String usernameOrEmail,
			final String password, final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return authorizeAppUser(usernameOrEmail, password);
			}
		}).execute();
	}

    /**
     * Change the password for the currently logged in user. You must supply the
     * old password and the new password.
     * 
     * @param username the username or email address associated with the user entity in Usergrid
     * @param oldPassword the user's old password
     * @param newPassword the user's new password
     * @return ApiResponse object
     */
    public ApiResponse changePassword(String username, String oldPassword,
            String newPassword) {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("newpassword", newPassword);
        data.put("oldpassword", oldPassword);

        return apiRequest(HTTP_METHOD_POST, null, data, organizationId,  applicationId, "users",
                username, "password");

    }

    public void changePasswordAsync(final String username, final String oldPassword,
            final String newPassword, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return changePassword(username, oldPassword, newPassword);
            }
        }).execute();
    }

    /**
     * Log the user in with their numeric pin-code and get a valid access token.
     * 
     * @param  email  the email address associated with the user entity in Usergrid
     * @param  pin  the pin associated with the user entity in Usergrid
     * @return  non-null ApiResponse if request succeeds, check getError() for
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
        ApiResponse response = apiRequest(HTTP_METHOD_POST, formData, null,
                organizationId,  applicationId, "token");
        if (response == null) {
            return response;
        }
        if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
            loggedInUser = response.getUser();
            accessToken = response.getAccessToken();
            currentOrganization = null;
            logInfo("authorizeAppUser(): Access token: " + accessToken);
        } else {
            logInfo("authorizeAppUser(): Response: " + response);
        }
        return response;
    }

	/**
	 * Log the user in with their numeric pin-code and get a valid access token.
	 * Executes asynchronously in background and the callbacks are called in the
	 * UI thread.
	 * 
	 * @param  email  the email address associated with the user entity in Usergrid
     * @param  pin  the pin associated with the user entity in Usergrid     
     * @param callback A callback for the async response.
	 */
	public void authorizeAppUserViaPinAsync(final String email,
			final String pin, final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return authorizeAppUserViaPin(email, pin);
			}
		}).execute();
	}

    /**
     * Log the app in with it's application (not organization) client id and 
     * client secret key. Not recommended for production apps. Executes asynchronously 
     * in background and the callbacks are called in the UI thread.
     * 
     * @param  clientId  the Usergrid application's client ID 
     * @param  clientSecret  the Usergrid application's client secret
     * @param  callback  an ApiResponseCallback to handle the async response
     */
    public void authorizeAppClientAsync(final String clientId,
            final String clientSecret, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {

            @Override
            public ApiResponse doTask() {
                return authorizeAppClient(clientId, clientSecret);
            }
        }).execute();
    }

    private void validateNonEmptyParam(Object param, String paramName) {
        if ( isEmpty(param) ) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }

    /**
     * Log the user in with their Facebook access token retrieved via Facebook
     * OAuth. Sets the user's identifier and Usergrid OAuth2 access token in UGClient 
     * if successfully authorized.
     * 
     * @param fb_access_token the valid OAuth token returned by Facebook     
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
        ApiResponse response = apiRequest(HTTP_METHOD_POST, formData, null,
                organizationId,  applicationId, "auth", "facebook");
        if (response == null) {
            return response;
        }
        if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
            loggedInUser = response.getUser();
            accessToken = response.getAccessToken();
            currentOrganization = null;
            logInfo("authorizeAppUserViaFacebook(): Access token: "
                    + accessToken);
        } else {
            logInfo("authorizeAppUserViaFacebook(): Response: "
                    + response);
        }
        return response;
    }
    
	/**
     * Log the user in with their Facebook access token retrieved via Facebook
     * OAuth. Sets the user's identifier and Usergrid OAuth2 access token in UGClient 
     * if successfully authorized. Executes asynchronously in background and the 
     * callbacks are called in the UI thread.
     * 
     * @param  fb_access_token the valid OAuth token returned by Facebook 
     * @param  callback  an ApiResponseCallback to handle the async response          
     */
	public void authorizeAppUserViaFacebookAsync(final String fb_access_token,
			final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return authorizeAppUserViaFacebook(fb_access_token);
			}
		}).execute();
	}

    /**
     * Log out a user and destroy the access token currently stored in UGClient 
     * on the server and in the UGClient.
     * 
     * @param  username  The username to be logged out
     * @return  non-null ApiResponse if request succeeds
     */
    public ApiResponse logOutAppUser(String username) {
        String token = getAccessToken();
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("token",token);
        ApiResponse response = apiRequest(HTTP_METHOD_PUT, params, null,
                organizationId,  applicationId, "users",username,"revoketoken?");
        if (response == null) {
            return response;
        } else {
            logInfo("logoutAppUser(): Response: " + response);
            setAccessToken(null);
        }
        return response;
    }

    /**
     * Log out a user and destroy the access token currently stored in UGClient 
     * on the server and in the UGClient.
     * Executes asynchronously in background and the callbacks are called in the
     * UI thread.
     * 
     * @param  username  The username to be logged out
     * @param  callback  an ApiResponseCallback to handle the async response     
     */
    public void logOutAppUserAsync(final String username, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return logOutAppUser(username);
            }
        }).execute();
    }

   /**
     * Destroy a specific user token on the server. The token will also be cleared 
     * from the UGClient instance, if it matches the token provided.
     * 
     * @param username The username to be logged out
     * @param token The access token to be destroyed on the server
     * @return  non-null ApiResponse if request succeeds
     */
    public ApiResponse logOutAppUserForToken(String username, String token) {                
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("token",token);
        ApiResponse response = apiRequest(HTTP_METHOD_PUT, params, null,
                organizationId,  applicationId, "users",username,"revoketoken?");
        if (response == null) {
            return response;
        } else {
            logInfo("logoutAppWithTokenUser(): Response: " + response);
            if (token.equals(getAccessToken())) {
                setAccessToken(null);
            }
        }
        return response;
    }

    /**
     * Destroy a specific user token on the server. The token will also be cleared 
     * from the UGClient instance, if it matches the token provided.
     * Executes asynchronously in background and the callbacks are called in the UI thread.
     * 
     * @param  username  The username to be logged out
     * @param  token  The access token to be destroyed on the server   
     * @param callback A callback for the async response  
     */
    public void logOutAppUserForTokenAsync(final String username, final String token, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return logOutAppUserForToken(username, token);
            }
        }).execute();
    }

    /**
     * Log out a user and destroy all associated tokens on the server.
     * The token stored in UGClient will also be destroyed.
     * 
     * @param  username The username to be logged out
     * @return  non-null ApiResponse if request succeeds
     */
    public ApiResponse logOutAppUserForAllTokens(String username) {
        ApiResponse response = apiRequest(HTTP_METHOD_PUT, null, null,
                organizationId,  applicationId, "users",username,"revoketokens");
        if (response == null) {
            return response;
        } else {
            logInfo("logoutAppUserForAllTokens(): Response: " + response);
            setAccessToken(null);
        }
        return response;
    }

    /**
     * Log out a user and destroy all associated tokens on the server.
     * The token stored in UGClient will also be destroyed.
     * Executes asynchronously in background and the callbacks are called in the UI thread.
     * 
     * @param  username  The username to be logged out
     * @param callback A callback for the response
     */
    public void logOutAppUserForAllTokensAsync(final String username, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return logOutAppUserForAllTokens(username);
            }
        }).execute();
    }

    /**
     * Log the app in with it's application (not organization) client id and 
     * client secret key. Not recommended for production apps.
     * 
     * @param  clientId  the Usergrid application's client ID 
     * @param  clientSecret  the Usergrid application's client secret     
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
        ApiResponse response = apiRequest(HTTP_METHOD_POST, formData, null,
                organizationId, applicationId, "token");
        if (response == null) {
            return response;
        }
        if (!isEmpty(response.getAccessToken())) {
            loggedInUser = null;
            accessToken = response.getAccessToken();
            currentOrganization = null;
            logInfo("authorizeAppClient(): Access token: "
                    + accessToken);
        } else {
            logInfo("authorizeAppClient(): Response: " + response);
        }
        return response;
    }


    /****************** GENERIC ENTITY MANAGEMENT ***********************/
    /****************** GENERIC ENTITY MANAGEMENT ***********************/

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
        ApiResponse response = apiRequest(HTTP_METHOD_POST, null, entity,
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
        ApiResponse response = apiRequest(HTTP_METHOD_POST, null, properties,
                organizationId, applicationId, properties.get("type").toString());
        return response;
    }
    
    /**
  	 * Create a new entity on the server. Executes asynchronously in background
  	 * and the callbacks are called in the UI thread.
  	 * 
  	 * @param entity An instance with data to use to create the entity
  	 * @param callback A callback for the async response
  	 */
  	public void createEntityAsync(final Entity entity,
  			final ApiResponseCallback callback) {
  		(new ClientAsyncTask<ApiResponse>(callback) {
  			@Override
  			public ApiResponse doTask() {
  				return createEntity(entity);
  			}
  		}).execute();
  	}

  	
  	/**
  	 * Create a new entity on the server from a set of properties. Properties
  	 * must include a "type" property. Executes asynchronously in background and
  	 * the callbacks are called in the UI thread.
  	 * 
  	 * @param properties The values to use, with keys as property names and values 
  	 * as property values
  	 * @param callback A callback for the async response
  	 */
  	public void createEntityAsync(final Map<String, Object> properties,
  			final ApiResponseCallback callback) {
  		(new ClientAsyncTask<ApiResponse>(callback) {
  			@Override
  			public ApiResponse doTask() {
  				return createEntity(properties);
  			}
  		}).execute();
  	}
  	
  	/**
  	 * Create a set of entities on the server from an ArrayList. Each item in the array
  	 * contains a set of properties that define a entity.
  	 * 
  	 * @param type The type of entities to create.
  	 * @param entities A list of maps where keys are entity property names and values
  	 * are property values.
  	 * @return An instance with response data from the server.
  	 */
  	public ApiResponse createEntities(String type, ArrayList<Map<String, Object>> entities) {
        assertValidApplicationId();                
        if (isEmpty(type)) {
            throw new IllegalArgumentException("Missing entity type");
        }
        ApiResponse response = apiRequest(HTTP_METHOD_POST, null, entities,
   		     organizationId, applicationId, type);	           		
   		return response;	
    }
    
    /**
  	 * Create a set of entities on the server from an ArrayList. Each item in the array
  	 * contains a set of properties that define a entity. Executes asynchronously in 
  	 * background and the callbacks are called in the UI thread.
  	 * 
  	 * @param type The type of entities to create.
  	 * @param entities A list of maps where keys are entity property names and values
  	 * are property values.
  	 * @param callback A callback for the async response
  	 */
    public void createEntitiesAsync(final String type, final ArrayList<Map<String, Object>> entities,
  			final ApiResponseCallback callback) {
  		(new ClientAsyncTask<ApiResponse>(callback) {
  			@Override
  			public ApiResponse doTask() {
  				return createEntities(type, entities);
  			}
  		}).execute();
  	}

    /**
     * Creates an object instance that corresponds to the provided entity type.
     * Supported object types are Activity, Device, Group, Message, and User.
     * All other types will return a generic Entity instance with no type assigned.
     *
     * @param  type  the entity type of which to create an object instance
     * @return  an object instance that corresponds to the type provided
    */
    public Entity createTypedEntity(String type) {
        Entity entity = null;
        
        if( Activity.isSameType(type) ) {
            entity = new Activity(this);
        } else if( Device.isSameType(type) ) {
            entity = new Device(this);
        } else if( Group.isSameType(type) ) {
            entity = new Group(this);
        } else if( Message.isSameType(type) ) {
            entity = new Message(this);
        } else if( User.isSameType(type) ) {
            entity = new User(this);
        } else {
            entity = new Entity(this);
        }
        
        return entity;
    }

    /**
     * Requests all entities of specified type that match the provided query string.
     *
     * @param  type  the entity type to be retrieved
     * @param  queryString  a query string to send with the request
     * @return  a non-null ApiResponse object if successful
    */
    public ApiResponse getEntities(String type,String queryString)
    {
        Map<String, Object> params = null;

        if (queryString.length() > 0) {
            params = new HashMap<String, Object>();
            params.put("ql", queryString);
        }
        
        return apiRequest(HTTP_METHOD_GET, // method
                            params, // params
                            null, // data
                            organizationId,
                            applicationId,
                            type);
    }
    
    /**
     * Asynchronously requests all entities of specified type that match the provided query string.
     *
     * @param  type  the entity type to be retrieved
     * @param  queryString  a query string to send with the request
     * @param  callback an ApiResponseCallback to handle the async response
    */
    public void getEntitiesAsync(final String type,
            final String queryString, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return getEntities(type, queryString);
            }
        }).execute();
    }

    /**
     * Update an existing entity on the server.
     * 
     * @param entityID the entity to update
     * @param updatedProperties the new properties
     * @return an ApiResponse with the updated entity in it.
     */
    public ApiResponse updateEntity(String entityID, Map<String, Object> updatedProperties) {
    	assertValidApplicationId();
    	if (isEmpty(updatedProperties.get("type"))) {
            throw new IllegalArgumentException("Missing entity type");
    	}
    	ApiResponse response = apiRequest(HTTP_METHOD_PUT, null, updatedProperties,
    			organizationId, applicationId, updatedProperties.get("type").toString(), entityID);
    	return response;
    }

    
    /**
     * Update an existing entity on the server. Properties
     * must include a "type" property. Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     *
     * @param entityID the entity to update
     * @param updatedProperties the new properties
     * @param callback A callback for the async response
     */
    public void updateEntityAsync(final String entityID, final Map<String, Object> updatedProperties,
                                      final ApiResponseCallback callback) {
          (new ClientAsyncTask<ApiResponse>(callback) {
        	  @Override
        	  public ApiResponse doTask() {
        		  return updateEntity(entityID, updatedProperties);
        	  }
          }).execute();
    }

    /**
     * Updates the password associated with a user entity
     *
     * @param  usernameOrEmail  the username or email address associated with the user entity
     * @param  oldPassword  the user's old password
     * @param  newPassword  the user's new password
     * @return an ApiResponse with the updated entity in it.
     */
    public ApiResponse updateUserPassword(String usernameOrEmail, String oldPassword, String newPassword) {
    	Map<String,Object> updatedProperties = new HashMap<String,Object>();
    	updatedProperties.put("oldpassword", oldPassword);
    	updatedProperties.put("newpassword", newPassword);
    	return apiRequest(HTTP_METHOD_POST, null, updatedProperties,
    			organizationId, applicationId, "users", usernameOrEmail);
    }

    
    /**
     * Remove an existing entity on the server.
     * 
     * @param entityType the collection of the entity
     * @param entityID the specific entity to delete
     * @return an ApiResponse indicating whether the removal was successful
     */
    public ApiResponse removeEntity(String entityType, String entityID) {
    	assertValidApplicationId();
    	if (isEmpty(entityType)) {
            throw new IllegalArgumentException("Missing entity type");
    	}
    	ApiResponse response = apiRequest(HTTP_METHOD_DELETE, null, null,
    			organizationId, applicationId, entityType, entityID);
    	return response;
    }
    
    /**
     * Remove an existing entity on the server.
     * Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     *
     * @param entityType the collection of the entity
     * @param entityID the specific entity to delete
     * @param callback A callback with the async response
     */
    public void removeEntityAsync(final String entityType, final String entityID,
    								final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
            	return removeEntity(entityType, entityID);
    		}
    	}).execute();
    }
    
    /**
     * Perform a query request and return a query object. The Query object
     * provides a simple way of dealing with result sets that need to be
     * iterated or paged through.
     * 
     * See {@link #doHttpRequest(String,Map,Object,String...)} for
     * more on the parameters.
     * 
     * @param httpMethod The HTTP method to use in the query
     * @param params Query parameters.
     * @param data The request body.
     * @param segments Additional URL path segments to append to the request URL
     * @return An instance representing query results
     */
    public Query queryEntitiesRequest(String httpMethod,
            Map<String, Object> params, Object data, String... segments) {
        ApiResponse response = apiRequest(httpMethod, params, data, segments);
        return new EntityQuery(response, httpMethod, params, data, segments);
    }

    /**
     * Perform a query request and return a query object. The Query object
     * provides a simple way of dealing with result sets that need to be
     * iterated or paged through. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * See {@link #doHttpRequest(String,Map,Object,String...)} for
     * more on the parameters.
     * 
     * @param callback A callback for the async response
     * @param httpMethod The HTTP method to use in the query
     * @param params Query parameters.
     * @param data The request body.
     * @param segments Additional URL path segments to append to the request URL
     */
    public void queryEntitiesRequestAsync(final QueryResultsCallback callback,
            final String httpMethod, final Map<String, Object> params,
            final Object data, final String... segments) {
        (new ClientAsyncTask<Query>(callback) {
            @Override
            public Query doTask() {
                return queryEntitiesRequest(httpMethod, params, data, segments);
            }
        }).execute();
    }

    /**
     * Query object for handling the response from certain query requests
     * @y.exclude
     */
    private class EntityQuery implements Query {
        final String httpMethod;
        final Map<String, Object> params;
        final Object data;
        final String[] segments;
        final ApiResponse response;

        private EntityQuery(ApiResponse response, String httpMethod,
                Map<String, Object> params, Object data, String[] segments) {
            this.response = response;
            this.httpMethod = httpMethod;
            this.params = params;
            this.data = data;
            this.segments = segments;
        }

        private EntityQuery(ApiResponse response, EntityQuery q) {
            this.response = response;
            httpMethod = q.httpMethod;
            params = q.params;
            data = q.data;
            segments = q.segments;
        }

        /**
         * Gets the API response from the last request
         *
         * @return an ApiResponse object
         */
        public ApiResponse getResponse() {
            return response;
        }

        /**
         * Checks if there are more results available based on whether a 
         * 'cursor' property was present in the last result set.
         *
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
         * Performs a request for the next set of results based on the cursor
         * from the last result set.
         * 
         * @return query that contains results and where to get more.
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
                ApiResponse nextResponse = apiRequest(httpMethod, nextParams, data,
                        segments);
                return new EntityQuery(nextResponse, this);
            }
            return null;
        }

    }


    /****************** USER ENTITY MANAGEMENT ***********************/
    /****************** USER ENTITY MANAGEMENT ***********************/

    /**
     * Creates a user entity.
     * 
     * @param  username  required. The username to be associated with the user entity.
     * @param  name  the user's full name. Can be null.
     * @param  email  the user's email address.
     * @param  password  the user's password
     * @return  an ApiResponse object
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
	 * Creates a user. Executes asynchronously in background and the callbacks
	 * are called in the UI thread.
	 * 
	 * @param  username required. The username to be associated with the user entity.
     * @param  name  the user's full name. Can be null.
     * @param  email  the user's email address.
     * @param  password  the user's password.
	 * @param  callback  an ApiResponse callback for handling the async response.
	 */
	public void createUserAsync(final String username, final String name,
			final String email, final String password,
			final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return createUser(username, name, email, password);
			}
		}).execute();
	}

    /**
     * Retrieves the /users collection.
     * 
     * @return a Query object
     */
    public Query queryUsers() {
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, null, null,
                organizationId,  applicationId, "users");
        return q;
    }
    
    /**
     * Retrieves the /users collection. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param  callback  a QueryResultsCallback object to handle the async response
     */
    public void queryUsersAsync(QueryResultsCallback callback) {
        queryEntitiesRequestAsync(callback, HTTP_METHOD_GET, null, null,
                organizationId, applicationId, "users");
    }


    /**
     * Performs a query of the users collection using the provided query command.
     * For example: "name contains 'ed'".
     * 
     * @param  ql  the query to execute
     * @return  a Query object
     */
    public Query queryUsers(String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", ql);
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, params, null,organizationId,
                applicationId, "users");
        return q;
    }

    /**
     * Perform a query of the users collection using the provided query command.
     * For example: "name contains 'ed'". Executes asynchronously in background
     * and the callbacks are called in the UI thread.
     * 
     * @param  ql  the query to execute
     * @param  callback  a QueryResultsCallback object to handle the async response
     */
    public void queryUsersAsync(String ql, QueryResultsCallback callback) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", ql);
        queryEntitiesRequestAsync(callback, HTTP_METHOD_GET, params, null, 
                organizationId, applicationId, "users");
    }
    
    /**
     * Perform a query of the users collection within the specified distance of
     * the specified location and optionally using the provided additional query.
     * For example: "name contains 'ed'".
     * 
     * @param  distance  distance from the location in meters
     * @param  latitude  the latitude of the location to measure from
     * @param  longitude  the longitude of the location to measure from
     * @param  ql  an optional additional query to send with the request
     * @return  a Query object
     */
    public Query queryUsersWithinLocation(float distance, float latitude,
            float longitude, String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql",
                this.makeLocationQL(distance, latitude, longitude, ql));
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, params, null, organizationId,
                applicationId, "users");
        return q;
    }

    
    /****************** GROUP ENTITY MANAGEMENT ***********************/
    /****************** GROUP ENTITY MANAGEMENT ***********************/

    /**
     * Creates a group with the specified group path. Group paths can be slash
     * ("/") delimited like file paths for hierarchical group relationships.
     * 
     * @param  groupPath  the path to use for the new group.
     * @return  an ApiResponse object
     */
    public ApiResponse createGroup(String groupPath) {
        return createGroup(groupPath, null);
    }

    /**
     * Creates a group with the specified group path and group title. Group
     * paths can be slash ("/") delimited like file paths for hierarchical group
     * relationships.
     * 
     * @param  groupPath  the path to use for the new group
     * @param  groupTitle  the title to use for the new group
     * @return  an ApiResponse object
     */
    public ApiResponse createGroup(String groupPath, String groupTitle) {
     return createGroup(groupPath, groupTitle, null);  
    }
    
    /**
     * Create a group with a path, title and name. Group
     * paths can be slash ("/") delimited like file paths for hierarchical group
     * relationships.
     *
     * @param  groupPath  the path to use for the new group
     * @param  groupTitle  the title to use for the new group
     * @param  groupName  the name to use for the new group
     * @return  an ApiResponse object
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
        
        return apiRequest(HTTP_METHOD_POST, null, data,  organizationId, applicationId, "groups");
    }

    /**
     * Creates a group with the specified group path. Group paths can be slash
     * ("/") delimited like file paths for hierarchical group relationships.
     * Executes asynchronously in background and the callbacks are called in the
     * UI thread.
     * 
     * @param  groupPath  the path to use for the new group.
     * @param  callback  an ApiResponseCallback object to handle the async response
     */
    public void createGroupAsync(String groupPath,
            final ApiResponseCallback callback) {
        createGroupAsync(groupPath, null, null);
    }

    /**
     * Creates a group with the specified group path and group title. Group
     * paths can be slash ("/") deliminted like file paths for hierarchical
     * group relationships. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param  groupPath  the path to use for the new group.
     * @param  groupTitle  the title to use for the new group.
     * @param  callback  an ApiResponseCallback object to handle the async response
     */
    public void createGroupAsync(final String groupPath,
            final String groupTitle, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return createGroup(groupPath, groupTitle);
            }
        }).execute();
    }

    /**
     * Gets the groups associated with a user entity
     * 
     * @param  userId  the UUID of the user entity
     * @return  a map with the group path as the key and a Group object as the value
     */
    public Map<String, Group> getGroupsForUser(String userId) {
        ApiResponse response = apiRequest(HTTP_METHOD_GET, null, null,
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
	 * Gets the groups associated with a user entity. Executes asynchronously in background and
	 * the callbacks are called in the UI thread.
	 * 
     * @param  userId  the UUID of the user entity
	 * @param  callback  a GroupsRetrievedCallback object to handle the async response
	 */
	public void getGroupsForUserAsync(final String userId,
			final GroupsRetrievedCallback callback) {
		(new ClientAsyncTask<Map<String, Group>>(callback) {
			@Override
			public Map<String, Group> doTask() {
				return getGroupsForUser(userId);
			}
		}).execute();
	}

    /**
     * Gets the user entities associated with the specified group.
     * 
     * @param  groupId  UUID of the group entity
     * @return  a Query object with the results of the query
     */
    public Query queryUsersForGroup(String groupId) {
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, null, null, organizationId,
                applicationId, "groups", groupId, "users");
        return q;
    }

    /**
     * Gets the user entities associated with the specified group. Executes 
     * asynchronously in background and the callbacks are called in the UI thread.
     * 
     * @param  groupId  UUID of the group entity
     * @param  callback a QueryResultsCallback object to handle the async response
     */
    public void queryUsersForGroupAsync(String groupId,
            QueryResultsCallback callback) {
        queryEntitiesRequestAsync(callback, HTTP_METHOD_GET, null, null,
                getApplicationId(), "groups", groupId, "users");
    }

    /**
     * Connects a user entity to the specified group entity.
     * 
     * @param  userId  UUID of the user entity
     * @param  groupId  UUID of the group entity 
     * @return  an ApiResponse object
     */
    public ApiResponse addUserToGroup(String userId, String groupId) {
        return apiRequest(HTTP_METHOD_POST, null, null, organizationId,  applicationId, "groups",
                groupId, "users", userId);
    }

    /**
     * Connects a user entity to the specified group entity. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param  userId  UUID of the user entity
     * @param  groupId  UUID of the group entity 
     * @param  callback  an ApiResponseCallback object to handle the async response
     */
    public void addUserToGroupAsync(final String userId, final String groupId,
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return addUserToGroup(userId, groupId);
            }
        }).execute();
    }

    /**
     * Disconnects a user entity from the specified group entity.
     * 
     * @param  userId  UUID of the user entity
     * @param  groupId  UUID of the group entity 
     * @return  an ApiResponse object
     */
    public ApiResponse removeUserFromGroup(String userId, String groupId) {
        return apiRequest(HTTP_METHOD_DELETE, null, null, organizationId,  applicationId, "groups",
                groupId, "users", userId);
    }

    /**
     * Disconnects a user entity from the specified group entity. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param  userId  UUID of the user entity
     * @param  groupId  UUID of the group entity 
     * @param  callback  an ApiResponseCallback object to handle the async response
     */
    public void removeUserFromGroupAsync(final String userId, final String groupId,
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return removeUserFromGroup(userId, groupId);
            }
        }).execute();
    }

    /****************** ACTIVITY ENTITY MANAGEMENT ***********************/
    /****************** ACTIVITY ENTITY MANAGEMENT ***********************/

    /**
     * Get a user's activity feed. Returned as a query to ease paging.
     * 
     * @param  userId  UUID of user entity
     * @return  a Query object
     */
    public Query queryActivityFeedForUser(String userId) {
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, null, null,
                organizationId, applicationId, "users", userId, "feed");
        return q;
    }
    
	/**
	 * Get a user's activity feed. Returned as a query to ease paging. Executes
	 * asynchronously in background and the callbacks are called in the UI
	 * thread.
	 * 
	 * 
	 * @param  userId  UUID of user entity
	 * @param  callback  a QueryResultsCallback object to handle the async response
	 */
	public void queryActivityFeedForUserAsync(final String userId, final QueryResultsCallback callback) {
		(new ClientAsyncTask<Query>(callback) {
			@Override
			public Query doTask() {
				return queryActivityFeedForUser(userId);
			}
		}).execute();
	}


    /**
     * Posts an activity to a user entity's activity stream. Activity must already be created.
     * 
     * @param userId 
     * @param activity 
     * @return An instance with the server response
     */
    public ApiResponse postUserActivity(String userId, Activity activity) {
        return apiRequest(HTTP_METHOD_POST, null, activity,  organizationId, applicationId, "users",
                userId, "activities");
    }

    /**
     * Creates and posts an activity to a user entity's activity stream.
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
        Activity activity = Activity.newActivity(this, verb, title, content,
                category, user, object, objectType, objectName, objectContent);
        return postUserActivity(user.getUuid().toString(), activity);
    }

	/**
	 * Creates and posts an activity to a user. Executes asynchronously in
	 * background and the callbacks are called in the UI thread.
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
	 * @param callback
	 */
	public void postUserActivityAsync(final String verb, final String title,
			final String content, final String category, final User user,
			final Entity object, final String objectType,
			final String objectName, final String objectContent,
			final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return postUserActivity(verb, title, content, category, user,
						object, objectType, objectName, objectContent);
			}
		}).execute();
	}

    /**
     * Posts an activity to a group. Activity must already be created.
     * 
     * @param groupId
     * @param activity
     * @return
     */
    public ApiResponse postGroupActivity(String groupId, Activity activity) {
        return apiRequest(HTTP_METHOD_POST, null, activity, organizationId, applicationId, "groups",
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
        Activity activity = Activity.newActivity(this, verb, title, content,
                category, user, object, objectType, objectName, objectContent);
        return postGroupActivity(groupId, activity);
    }

	/**
	 * Creates and posts an activity to a group. Executes asynchronously in
	 * background and the callbacks are called in the UI thread.
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
	 * @param callback
	 */
	public void postGroupActivityAsync(final String groupId, final String verb, final String title,
			final String content, final String category, final User user,
			final Entity object, final String objectType,
			final String objectName, final String objectContent,
			final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return postGroupActivity(groupId, verb, title, content, category, user,
						object, objectType, objectName, objectContent);
			}
		}).execute();
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
        Activity activity = Activity.newActivity(this, verb, title, content,
                category, user, object, objectType, objectName, objectContent);
        return createEntity(activity);
    }
    
    /**
     * Get a group's activity feed. Returned as a query to ease paging.
     *      
     * @return
     */
    public Query queryActivity() {
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, null, null,
               organizationId, applicationId, "activities");
        return q;
    }

    

    /**
     * Get a group's activity feed. Returned as a query to ease paging.
     * 
     * @param groupId
     * @return
     */
    public Query queryActivityFeedForGroup(String groupId) {
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, null, null,
                organizationId,  applicationId, "groups", groupId, "feed");
        return q;
    }

    /**
     * Get a group's activity feed. Returned as a query to ease paging. Executes
     * asynchronously in background and the callbacks are called in the UI
     * thread.
     * 
     * 
     * @param groupId
     * @param callback
     */
    public void queryActivityFeedForGroupAsync(final String groupId,
            final QueryResultsCallback callback) {
        (new ClientAsyncTask<Query>(callback) {
            @Override
            public Query doTask() {
                return queryActivityFeedForGroup(groupId);
            }
        }).execute();
    }
    

    /****************** ENTITY CONNECTIONS ***********************/
    /****************** ENTITY CONNECTIONS ***********************/

    /**
     * Connect two entities together.
     * 
     * @param connectingEntityType The type of the first entity.
     * @param connectingEntityId The ID of the first entity.
     * @param connectionType The type of connection between the entities.
     * @param connectedEntityId The ID of the second entity.
     * @return An instance with the server's response.
     */
    public ApiResponse connectEntities(String connectingEntityType,
            String connectingEntityId, String connectionType,
            String connectedEntityId) {
        return apiRequest(HTTP_METHOD_POST, null, null,  organizationId, applicationId,
                connectingEntityType, connectingEntityId, connectionType,
                connectedEntityId);
    }
    
    /**
     * Connect two entities together
     * 
     * @param connectorType The type of the first entity in the connection.
     * @param connectorID The first entity's ID.
     * @param connectionType The type of connection to make.
     * @param connecteeType The type of the second entity.
     * @param connecteeID The second entity's ID
     * @return An instance with the server's response.
     */
    public ApiResponse connectEntities(String connectorType,
    		String connectorID,
    		String connectionType,
    		String connecteeType,
    		String connecteeID) {
		return apiRequest(HTTP_METHOD_POST, null, null, organizationId, applicationId,
				connectorType, connectorID, connectionType, connecteeType, connecteeID);
    }


	/**
	 * Connect two entities together. Executes asynchronously in background and
	 * the callbacks are called in the UI thread.
	 * 
     * @param connectingEntityType The type of the first entity.
     * @param connectingEntityId The UUID or 'name' property of the first entity.
     * @param connectionType The type of connection between the entities.
     * @param connectedEntityId The UUID of the second entity.
	 * @param callback A callback with the async response.
	 */
	public void connectEntitiesAsync(final String connectingEntityType,
			final String connectingEntityId, final String connectionType,
			final String connectedEntityId, final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return connectEntities(connectingEntityType,
						connectingEntityId, connectionType, connectedEntityId);
			}
		}).execute();
	}

    /**
     * Connect two entities together. Allows the 'name' of the connected entity
     * to be specified but requires the type also be specified. Executes asynchronously 
     * in background and the callbacks are called in the UI thread.
     * 
     * @param connectingEntityType The type of the first entity.
     * @param connectingEntityId The UUID or 'name' property of the first entity.
     * @param connectionType The type of connection between the entities.
     * @param connectedEntityType The type of connection between the entities.
     * @param connectedEntityId The UUID or 'name' property of the second entity.
     * @param callback A callback with the async response.
     */
    public void connectEntitiesAsync(final String connectingEntityType,
            final String connectingEntityId, final String connectionType,
            final String connectedEntityType, final String connectedEntityId, 
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return connectEntities(connectingEntityType,
                        connectingEntityId, connectionType, connectedEntityType, connectedEntityId);
            }
        }).execute();
    }
	
    /**
     * Disconnect two entities.
     * 
     * @param connectingEntityType The collection name or UUID of the first entity.
     * @param connectingEntityId The name or UUID of the first entity.
     * @param connectionType The type of connection between the entities.     
     * @param connectedEntityId The name or UUID of the second entity.
     * @return An instance with the server's response.
     */
    public ApiResponse disconnectEntities(String connectingEntityType,
            String connectingEntityId, String connectionType,
            String connectedEntityId) {
        return apiRequest(HTTP_METHOD_DELETE, null, null,  organizationId, applicationId,
                connectingEntityType, connectingEntityId, connectionType,
                connectedEntityId);
    }

	/**
	 * Disconnect two entities. Executes asynchronously in background and the
	 * callbacks are called in the UI thread.
	 * 
     * @param connectingEntityType The collection name or UUID of the first entity.
     * @param connectingEntityId The name or UUID of the first entity.
     * @param connectionType The type of connection between the entities.     
     * @param connectedEntityId The name or UUID of the second entity.
	 */
	public void disconnectEntitiesAsync(final String connectingEntityType,
			final String connectingEntityId, final String connectionType,
			final String connectedEntityId, final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return disconnectEntities(connectingEntityType,
						connectingEntityId, connectionType, connectedEntityId);
			}
		}).execute();
	}

    /**
     * Disconnect two entities.
     * 
     * @param connectingEntityType The collection name or UUID of the first entity.
     * @param connectingEntityId The name or UUID of the first entity.
     * @param connectionType The type of connection between the entities.
     * @param connectedEntityType The collection name or UUID of the second entity.
     * @param connectedEntityId The name or UUID of the second entity.
     * @return An instance with the server's response.
     */
    public ApiResponse disconnectEntities(String connectingEntityType,
            String connectingEntityId, String connectionType,
            String connectedEntityType, String connectedEntityId) {
        return apiRequest(HTTP_METHOD_DELETE, null, null,  organizationId, applicationId,
                connectingEntityType, connectingEntityId, connectionType,
                connectedEntityType, connectedEntityId);
    }

    /**
     * Disconnect two entities. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param connectingEntityType The collection name or UUID of the first entity.
     * @param connectingEntityId The name or UUID of the first entity.
     * @param connectionType The type of connection between the entities.
     * @param connectedEntityType The collection name or UUID of the second entity.
     * @param connectedEntityId The name or UUID of the second entity.
     * @param callback A callback with the async response.
     */
    public void disconnectEntitiesAsync(final String connectingEntityType,
            final String connectingEntityId, final String connectionType,
            final String connectedEntityType, final String connectedEntityId, 
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return disconnectEntities(connectingEntityType,
                        connectingEntityId, connectionType, connectedEntityType, connectedEntityId);
            }
        }).execute();
    }
	
    /**
     * Queries for entities connected with <em>connectionType</em>
     * to the entity whose ID is <em>connectingEntityId</em>. Use the 
     * <em>ql</em> parameter to specify a query string for further 
     * filtering.
     * 
     * @param connectingEntityType The type of the first entity.
     * @param connectingEntityId The ID of the first entity.
     * @param connectionType The type of connection between the entities.
     * @param ql The string (if any) that should follow the "ql"
     * in the request URL
     * @return
     */
    public Query queryEntityConnections(String connectingEntityType,
            String connectingEntityId, String connectionType, String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", ql);        
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, params, null,
                organizationId, applicationId, connectingEntityType, connectingEntityId,
                connectionType);
        return q;
    }

	/**
     * Queries for entities connected with <em>connectionType</em>
     * to the entity whose ID is <em>connectingEntityId</em>. Use the 
     * <em>ql</em> parameter to specify a query string for further 
     * filtering. Executes asynchronously in background and
	 * the callbacks are called in the UI thread.
	 * 
     * @param connectingEntityType The type of the first entity.
     * @param connectingEntityId The ID of the first entity.
     * @param connectionType The type of connection between the entities.
     * @param ql The string (if any) that should follow the "ql"
     * in the request URL
	 * @param callback A callback for the async response.
	 */
	public void queryEntityConnectionsAsync(String connectingEntityType,
			String connectingEntityId, String connectionType, String ql,
			QueryResultsCallback callback) {
		Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", ql);        
		queryEntitiesRequestAsync(callback, HTTP_METHOD_GET, params, null,
				getOrganizationId(), getApplicationId(), connectingEntityType, connectingEntityId,
				connectionType);
	}
	
    protected String makeLocationQL(float distance, double latitude,
            double longitude, String ql) {
        String within = String.format("within %d of %d , %d", distance,
                latitude, longitude);
        ql = ql == null ? within : within + " and " + ql;
        return ql;
    }

    /**
     * Queries for entities connected with <em>connectionType</em>
     * to the entity whose ID is <em>connectingEntityId</em> and
     * within distance of a specific point.
     * 
     * @param connectingEntityType The type of the first entity.
     * @param connectingEntityId The ID of the first entity.
     * @param connectionType The type of connection between the entities.
     * @param distance The distance in meters from the specified latitude and 
     * longitude.
     * @param latitude The latitude of the point from which to 
     * measure distance.
     * @param longitude The longitude of the ponit from which to
     * measure distance.
     * @return An instance with the query results.
     */
    public Query queryEntityConnectionsWithinLocation(
            String connectingEntityType, String connectingEntityId,
            String connectionType, float distance, float latitude,
            float longitude, String ql) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", makeLocationQL(distance, latitude, longitude, ql));
        Query q = queryEntitiesRequest(HTTP_METHOD_GET, params, null, organizationId,
                applicationId, connectingEntityType, connectingEntityId,
                connectionType);
        return q;
    }

	/**
     * Queries for entities connected with <em>connectionType</em>
     * to the entity whose ID is <em>connectingEntityId</em> and
     * within distance of a specific point. Executes asynchronously in 
     * background and the callbacks are called in the UI thread.
	 * 
     * @param connectingEntityType The type of the first entity
     * @param connectingEntityId The ID of the first entity
     * @param connectionType The type of connection between the entities
     * @param distance The distance in meters from the specified latitude and 
     * longitude
     * @param location a Location object
     * @param ql an optional query to add to the request
	 * @param callback A callback for the async response
	 */
	public void queryEntityConnectionsWithinLocationAsync(
			String connectingEntityType, String connectingEntityId,
			String connectionType, float distance, Location location,
			String ql, QueryResultsCallback callback) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ql", makeLocationQL(distance, location.getLatitude(), location.getLongitude(), ql));
		params.put("ql", ql);
		queryEntitiesRequestAsync(callback, HTTP_METHOD_GET, params, null,
				getOrganizationId(), getApplicationId(), connectingEntityType, connectingEntityId,
				connectionType);
	}
	
    /****************** MESSAGE QUEUES ***********************/
    /****************** MESSAGE QUEUES ***********************/

    private String normalizeQueuePath(String path) {
        return arrayToDelimitedString(
                tokenizeToStringArray(path, "/", true, true), "/");
    }

    public ApiResponse postMessage(String path, Map<String, Object> message) {
        return apiRequest(HTTP_METHOD_POST, null, message, organizationId,  applicationId,
                "queues", normalizeQueuePath(path));
    }

    public ApiResponse postMessage(String path,
            List<Map<String, Object>> messages) {
        return apiRequest(HTTP_METHOD_POST, null, messages,  organizationId, applicationId,
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
        return apiRequest(HTTP_METHOD_GET, params, null,  organizationId, applicationId,
                "queues", normalizeQueuePath(path));
    }

    public ApiResponse addSubscriber(String publisherQueue,
            String subscriberQueue) {
        return apiRequest(HTTP_METHOD_POST, null, null, organizationId,  applicationId, "queues",
                normalizeQueuePath(publisherQueue), "subscribers",
                normalizeQueuePath(subscriberQueue));
    }

    public ApiResponse removeSubscriber(String publisherQueue,
            String subscriberQueue) {
        return apiRequest(HTTP_METHOD_DELETE, null, null, organizationId,  applicationId,
                "queues", normalizeQueuePath(publisherQueue), "subscribers",
                normalizeQueuePath(subscriberQueue));
    }
    
    private class QueueQuery implements Query {
        final String httpMethod;
        final Map<String, Object> params;
        final Object data;
        final String queuePath;
        final ApiResponse response;

        private QueueQuery(ApiResponse response, String httpMethod,
                Map<String, Object> params, Object data, String queuePath) {
            this.response = response;
            this.httpMethod = httpMethod;
            this.params = params;
            this.data = data;
            this.queuePath = normalizeQueuePath(queuePath);
        }

        private QueueQuery(ApiResponse response, QueueQuery q) {
            this.response = response;
            httpMethod = q.httpMethod;
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
                ApiResponse nextResponse = apiRequest(httpMethod, nextParams, data,
                        queuePath);
                return new QueueQuery(nextResponse, this);
            }
            return null;
        }

    }

    public Query queryQueuesRequest(String httpMethod,
            Map<String, Object> params, Object data, String queuePath) {
        ApiResponse response = apiRequest(httpMethod, params, data, queuePath);
        return new QueueQuery(response, httpMethod, params, data, queuePath);
    }
    
    
    /****************** COLLECTION MANAGEMENT ***********************/
    /****************** COLLECTION MANAGEMENT ***********************/

    /**
     * Gets a collection of type <em>type</em>.
     * 
     * @param type The entity type to return in the collection.
     * @return A collection of the specified type.
     */
    public Collection getCollection(String type)
    {
        return getCollection(type,null);
    }

    /**
     * Gets a collection of type <em>type</em>, with entities 
     * filtered using a query string created from keys and values
     * in <em>qs</em>.
     * 
     * @param type The entity type to return in the collection.
     * @param qs A query string specifying how to filter the entities 
     * included in the collection.
     * @return A collection of the specified type.
     */
    public Collection getCollection(String type,Map<String,Object> qs)
    {
        return new Collection(this,type,qs);
    }

    /**
     * Asynchronously gets a collection of type <em>type</em>, with entities 
     * filtered using a query string created from keys and values
     * in <em>qs</em>.
     * 
     * @param type The entity type to return in the collection.
     * @param qs A query string specifying how to filter the entities 
     * included in the collection.
     * @param callback A callback instance to use for the response.
     */
    public void getCollectionAsync(final String type, final Map<String,Object> qs,
		final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return getCollection(type, qs).fetch();
            }
        }).execute();
    }
    

    /****************** DEVICE ENTITY MANAGEMENT ***********************/
    /****************** DEVICE ENTITY MANAGEMENT ***********************/
    
    /**
     * Creates or updates a device entity using the provided deviceId, and saves the device model, 
     * device platform, and OS version in the entity. 
     * If an entity does not exist for the device, it is created with a UUID equal to deviceId.
     * 
     * @param  deviceId  the device entity's UUID
     * @param  properties  additional properties to save in the device entity.      
     * @return  a Device object if success. Models the Usergrid device entity.
     */
    public ApiResponse registerDevice(UUID deviceId, Map<String, Object> properties) {
        assertValidApplicationId();
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.put("refreshed", System.currentTimeMillis());
        
        // add device meta-data
        properties.put("deviceModel", Build.MODEL);
        properties.put("devicePlatform", "android");
        properties.put("deviceOSVersion", Build.VERSION.RELEASE);
        
        ApiResponse response = apiRequest(HTTP_METHOD_PUT, null, properties,
                organizationId, applicationId, "devices", deviceId.toString());
        return response;
    }

    /**
     * Creates or updates a device entity using the provided deviceId, and saves the device model, 
     * device platform, and OS version in the entity. 
     * If an entity does not exist for the device, it is created with a UUID equal to deviceId.
     * Executes asynchronously in background and the callbacks are called in the UI thread.
     * 
     * @param  deviceId  the device entity's UUID
     * @param  properties  additional properties to save in the device entity     
     * @param  callback  a DeviceRegistrationCallback to handle the async response
     */
    public void registerDeviceAsync(final UUID deviceId,
            final Map<String, Object> properties,
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return registerDevice(deviceId, properties);
            }
        }).execute();
    }


    /****************** EVENT ENTITY MANAGEMENT ***********************/
    /****************** EVENT ENTITY MANAGEMENT ***********************/

    /**
     * Creates an Event entity from properties specified in <em>mapEvent</em>.
     * If no timestamp property value is provided, one is assigned by the server.
     * 
     * @param mapEvent A Map instance whose keys are property names and values 
     * are property values.
     * @return An API response from the server.
     */
	public ApiResponse createEvent(Map<String,Object> mapEvent)
	{
		if (mapEvent != null) {
			boolean needTypeSet = true;

			if (mapEvent.containsKey("type")) {
				Object typeValue = mapEvent.get("type");
				if (typeValue != null) {
					if (typeValue instanceof String) {
						String typeValueString = (String) typeValue;
						if (typeValueString.equals("event") || typeValueString.equals("events")) {
							needTypeSet = false;
						}
					}
				}
			}
			
			if (needTypeSet) {
				mapEvent.put("type", "events");
			}
			
			if (!mapEvent.containsKey("timestamp")) {
				// let the server assign the timestamp
				mapEvent.put("timestamp", "0");
			}
		}
		
		return createEntity(mapEvent);
	}
	
	/**
     * Creates an Event entity from properties specified in <em>mapEvent</em> and
     * using <em>timestamp</em> as the event's timestamp property value.
     * 
     * @param mapEvent A Map instance whose keys are property names and values 
     * are property values.
     * @param timestamp An instance with the event timestamp value.
     * @return An API response from the server.
	 */
	public ApiResponse createEvent(Map<String,Object> mapEvent, Date timestamp)
	{
		if (mapEvent == null) {
			mapEvent = new HashMap<String,Object>();
		}

		populateTimestamp(timestamp, mapEvent);
		return createEvent(mapEvent);
	}
	
	/**
	 * Creates an Event entity from properties specified in <em>mapEvent</em>, adding a
	 * counters property from values specified in <em>counterIncrement</em>.
	 * 
	 * @param mapEvent The map containing property values for the new entity.
	 * @param counterIncrement An instance with a counter name and increment value.
     * @return An API response from the server.
	 */
	public ApiResponse createEvent(Map<String,Object> mapEvent, CounterIncrement counterIncrement)
	{
		if (mapEvent == null) {
			mapEvent = new HashMap<String,Object>();
		}
		
		populateTimestamp(null, mapEvent);
		populateCounter(counterIncrement, mapEvent);
		return createEvent(mapEvent);
	}
	
	/**
	 * Asynchronously creates an Event entity from properties specified in <em>mapEvent</em>, 
	 * adding a counters property from values specified in <em>counterIncrement</em>.
	 * 
	 * @param mapEvent The map containing property values for the new entity.
	 * @param counterIncrement An instance with a counter name and increment value.
	 * @param callback The response callback.
	 */
	public void createEventAsync(final Map<String,Object> mapEvent, final CounterIncrement counterIncrement, final ApiResponseCallback callback)
	{
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return createEvent(mapEvent,counterIncrement);
			}
		}).execute();
	}
	
	/**
     * Creates an Event entity from properties specified in <em>mapEvent</em> and
     * using <em>timestamp</em> as the event's timestamp property value. This adds
     * a counters property from values specified in <em>counterIncrement</em>
     * 
     * @param mapEvent A Map instance whose keys are property names and values 
     * are property values.
     * @param timestamp An instance with the event timestamp value.
	 * @param counterIncrement An instance with a counter name and increment value.
     * @return An API response from the server.
	 */
	public ApiResponse createEvent(Map<String,Object> mapEvent, Date timestamp, CounterIncrement counterIncrement)
	{
		if (mapEvent == null) {
			mapEvent = new HashMap<String,Object>();
		}

		populateTimestamp(timestamp, mapEvent);
		populateCounter(counterIncrement, mapEvent);
		return createEvent(mapEvent);
	}
	
	/**
     * Creates an Event entity from properties specified in <em>mapEvent</em> and
     * using <em>timestamp</em> as the event's timestamp property value. This adds
     * counters properties from values specified in <em>counterIncrements</em>
     * 
     * @param mapEvent A Map instance whose keys are property names and values 
     * are property values.
     * @param timestamp An instance with the event timestamp value.
	 * @param counterIncrements A list of instances with a counter name and increment value.
     * @return An API response from the server.
	 */
	public ApiResponse createEvent(Map<String,Object> mapEvent, Date timestamp, List<CounterIncrement> counterIncrements)
	{
		if (mapEvent == null) {
			mapEvent = new HashMap<String,Object>();
		}

		populateTimestamp(timestamp, mapEvent);
		
		if (counterIncrements != null) {
			for (CounterIncrement counterIncrement : counterIncrements) {
				populateCounter(counterIncrement, mapEvent);
			}
		}
		
		return createEvent(mapEvent);
	}

	/**
	 * Adds the value of <em>timestamp</em> as the value of a timestamp 
	 * key in <em>mapEvent</em>.
	 * 
	 * @param timestamp The value that should be used for the timestamp key.
	 * @param mapEvent The map to which the timestamp key should be added.
	 */
    protected void populateTimestamp(Date timestamp, Map<String,Object> mapEvent)
    {
        if (timestamp != null) {
            mapEvent.put("timestamp", "" + timestamp.getTime());
        } else {
            // let the server assign the timestamp
            mapEvent.put("timestamp", "0");
        }
    }

    /**
     * Adds the counter name and increment value from <em>counterIncrement</em> as the Map
     * value of a "counters" key in <em>mapEvent</em>. If <em>counterIncrement</em> has no
     * counter name, no key/value is added.
     * 
     * @param counterIncrement An instance containing a counter name and increment value.
     * @param mapEvent The map to which the counters key/value should be added.
     */
    protected void populateCounter(CounterIncrement counterIncrement, Map<String,Object> mapEvent)
    {
        if ((counterIncrement != null) && (mapEvent != null)) {
            String counterName = counterIncrement.getCounterName();
            if ((counterName != null) && (counterName.length() > 0)) {
                Map<String, Object> mapCounters = null;
                
                Object existingCounters = mapEvent.get("counters");
                
                if (existingCounters != null) {
                    if (existingCounters instanceof Map) {
                        try {
                            mapCounters = (Map<String,Object>) existingCounters;
                        } catch (Throwable t) {
                            mapCounters = null;
                        }
                    }
                }
                
                if (null == mapCounters) {
                    mapCounters = new HashMap<String,Object>();
                }
                
                mapCounters.put(counterName, new Long(counterIncrement.getCounterIncrementValue()));
                mapEvent.put("counters", mapCounters);
            }
        }
    }

    /**
     * Requests one or more counters
     *
     * @param  counterArray  an ArrayList of counter names to be retrieved
     * @return  an ApiResponse object
    */
    public ApiResponse getCounters(ArrayList<String> counterArray)
    {   
        String counters = null;

        if (counterArray != null) {                      
            counters = "?counter=" + counterArray.get(0);
            for (int i = 1; i < counterArray.size(); i++) {
                counters += "&counter=";
                counters += counterArray.get(i);
            }
        }

        return apiRequest(HTTP_METHOD_GET, // method
                            null, // params
                            null, // data
                            organizationId,
                            applicationId,
                            "counters",
                            counters);
    }

    /**
     * Asynchronously requests one or more counters
     *
     * @param  type  the entity type to be retrieved
     * @param  counterArray  an ArrayList of counter names to be retrieved
     * @param  callback an ApiResponseCallback to handle the async response
    */
    public void getCountersAsync(final ArrayList<String> counterArray,
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return getCounters(counterArray);
            }
        }).execute();
    }

    /**
     * Requests one or more counters for a given time inteval and resolution, e.g. day, hour, etc.
     *
     * @param  counterArray  an ArrayList of counter names to be retrieved
     * @param  startTime  a Date object specifying the start time to retrieve counter data for
     * @param  endTime  a Date object specifying the end time to retrieve counter data for
     * @param  resolution  the resolution of the result set. Results can be returned for the following:
     *      all, minute, five_minutes, half_hour, hour, day, six_day, week, month
     * @param  callback an ApiResponseCallback to handle the async response
     * @return an ApiResponse object
    */
    public ApiResponse getCountersForInterval(ArrayList<String> counterArray, 
            Date startTime, Date endTime, String resolution)
    {       
        String counters = null;

        if (counterArray != null) {         
            counters = "?counter=" + counterArray.get(0);
            for (int i = 1; i < counterArray.size(); i++) {
                counters += "&counter=";
                counters += counterArray.get(i);
            }    

            if (startTime != null) {
                counters += "&start_time=";
                counters += startTime.getTime();                               
            }

            if (endTime != null) {
                counters += "&end_time=";
                counters += endTime.getTime();
            }

            counters += "&resolution=";
            counters += resolution;
        }

        return apiRequest(HTTP_METHOD_GET, // method
                            null, // params
                            null, // data
                            organizationId,
                            applicationId,
                            "counters",
                            counters);
    }

    /**
     * Asynchronously requests one or more counters for a given time inteval and resolution, e.g. day, hour, etc.
     *
     * @param  counterArray  an ArrayList of counter names to be retrieved
     * @param  startTime  a Date object specifying the start time to retrieve counter data for
     * @param  endTime  a Date object specifying the end time to retrieve counter data for
     * @param  resolution  the resolution of the result set. Results can be returned for the following:
     *      all, minute, five_minutes, half_hour, hour, day, six_day, week, month
     * @param  callback an ApiResponseCallback to handle the async response
    */
    public void getCountersForIntervalAsync(final ArrayList<String> counterArray, final Date startTime, 
            final Date endTime, final String resolution, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return getCountersForInterval(counterArray, startTime, endTime, resolution);
            }
        }).execute();
    }
}


