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
package org.apache.usergrid.android.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.apache.usergrid.android.client.callbacks.ApiResponseCallback;
import org.apache.usergrid.android.client.callbacks.ClientAsyncTask;
import org.apache.usergrid.android.client.callbacks.DeviceRegistrationCallback;
import org.apache.usergrid.android.client.callbacks.GroupsRetrievedCallback;
import org.apache.usergrid.android.client.callbacks.QueryResultsCallback;
import org.usergrid.java.client.entities.Device;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.entities.Group;
import org.usergrid.java.client.entities.User;
import org.usergrid.java.client.response.ApiResponse;
import org.apache.usergrid.android.client.utils.DeviceUuidFactory;

import android.content.Context;
import android.location.Location;

/**
 * The Client class for accessing the Usergrid API. Start by instantiating this
 * class though the appropriate constructor.
 * 
 */
public class Client extends org.usergrid.java.client.Client {

	private static final String TAG = "UsergridClient";

	public static boolean FORCE_PUBLIC_API = false;

	// Public API
	public static String PUBLIC_API_URL = "http://api.usergrid.com";

	// Local API of standalone server
	public static String LOCAL_STANDALONE_API_URL = "http://localhost:8080";

	// Local API of Tomcat server in Eclipse
	public static String LOCAL_TOMCAT_API_URL = "http://localhost:8080/ROOT";

	// Local API
	public static String LOCAL_API_URL = LOCAL_STANDALONE_API_URL;

	
	static RestTemplate restTemplate = new RestTemplate(true);

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
		super(organizationId, applicationId);
	}


	/**
	 * Log the user in and get a valid access token. Executes asynchronously in
	 * background and the callbacks are called in the UI thread.
	 * 
	 * @param email
	 * @param password
	 * @param callback
	 */
	public void authorizeAppUserAsync(final String email,
			final String password, final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return authorizeAppUser(email, password);
			}
		}).execute();
	}

	

	/**
	 * Log the user in with their numeric pin-code and get a valid access token.
	 * Executes asynchronously in background and the callbacks are called in the
	 * UI thread.
	 * 
	 * @param email
	 * @param pin
	 * @param callback
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
	 * Log the user in with their numeric pin-code and get a valid access token.
	 * Executes asynchronously in background and the callbacks are called in the
	 * UI thread.
	 * 
	 * @param email
	 * @param pin
	 * @param callback
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
	 * Log the app in with it's client id and client secret key. Not recommended
	 * for production apps. Executes asynchronously in background and the
	 * callbacks are called in the UI thread.
	 * 
	 * @param clientId
	 * @param clientSecret
	 * @param callback
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

	
	/**
	 * Registers a device using the device's unique device ID. Executes
	 * asynchronously in background and the callbacks are called in the UI
	 * thread.
	 * 
	 * @param context
	 * @param properties
	 * @param callback
	 */
	public void registerDeviceAsync(final Context context,
			final Map<String, Object> properties,
			final DeviceRegistrationCallback callback) {
		(new ClientAsyncTask<Device>(callback) {
			@Override
			public Device doTask() {
			    UUID deviceId = new DeviceUuidFactory(context).getDeviceUuid();
		        
				return registerDevice(deviceId, properties);
			}
		}).execute();
	}


  /**
   * Registers a device using the device's unique device ID. Executes
   * asynchronously in background and the callbacks are called in the UI
   * thread.
   *
   * @param context
   * @param properties
   * @param callback
   */
  public void registerDeviceForPushAsync(final Context context,
                                         final String notifier,
                                         final String token,
                                         final Map<String, Object> properties,
                                         final DeviceRegistrationCallback callback) {
    (new ClientAsyncTask<Device>(callback) {
      @Override
      public Device doTask() {
        UUID deviceId = new DeviceUuidFactory(context).getDeviceUuid();

        return registerDeviceForPush(deviceId, notifier, token, properties);
      }
    }).execute();
  }


  /**
	 * Create a new entity on the server. Executes asynchronously in background
	 * and the callbacks are called in the UI thread.
	 * 
	 * @param entity
	 * @param callback
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
	 * @param properties
	 * @param callback
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
	 * Creates a user. Executes asynchronously in background and the callbacks
	 * are called in the UI thread.
	 * 
	 * @param username
	 * @param name
	 * @param email
	 * @param password
	 * @param callback
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
	 * Get the groups for the user. Executes asynchronously in background and
	 * the callbacks are called in the UI thread.
	 * 
	 * @param userId
	 * @param callback
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
	 * Get a user's activity feed. Returned as a query to ease paging. Executes
	 * asynchronously in background and the callbacks are called in the UI
	 * thread.
	 * 
	 * 
	 * @param userId
	 * @param callback
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
	 * Get a group's activity feed. Returned as a query to ease paging. Executes
	 * asynchronously in background and the callbacks are called in the UI
	 * thread.
	 * 
	 * 
	 * @param userId
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


	/**
	 * Perform a query request and return a query object. The Query object
	 * provides a simple way of dealing with result sets that need to be
	 * iterated or paged through. Executes asynchronously in background and the
	 * callbacks are called in the UI thread.
	 * 
	 * @param callback
	 * @param method
	 * @param params
	 * @param data
	 * @param segments
	 */
	public void queryEntitiesRequestAsync(final QueryResultsCallback callback,
			final HttpMethod method, final Map<String, Object> params,
			final Object data, final String... segments) {
		(new ClientAsyncTask<Query>(callback) {
			@Override
			public Query doTask() {
				return queryEntitiesRequest(method, params, data, segments);
			}
		}).execute();
	}


	/**
	 * Perform a query of the users collection. Executes asynchronously in
	 * background and the callbacks are called in the UI thread.
	 * 
	 * @param callback
	 */
	public void queryUsersAsync(QueryResultsCallback callback) {
		queryEntitiesRequestAsync(callback, HttpMethod.GET, null, null,
				getApplicationId(), "users");
	}

	/**
	 * Perform a query of the users collection using the provided query command.
	 * For example: "name contains 'ed'". Executes asynchronously in background
	 * and the callbacks are called in the UI thread.
	 * 
	 * @param ql
	 * @param callback
	 */
	public void queryUsersAsync(String ql, QueryResultsCallback callback) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ql", ql);
		queryEntitiesRequestAsync(callback, HttpMethod.GET, params, null,
				getApplicationId(), "users");
	}

	
	/**
	 * Queries the users for the specified group. Executes asynchronously in
	 * background and the callbacks are called in the UI thread.
	 * 
	 * @param groupId
	 * @param callback
	 */
	public void queryUsersForGroupAsync(String groupId,
			QueryResultsCallback callback) {
		queryEntitiesRequestAsync(callback, HttpMethod.GET, null, null,
				getApplicationId(), "groups", groupId, "users");
	}

	/**
	 * Adds a user to the specified groups. Executes asynchronously in
	 * background and the callbacks are called in the UI thread.
	 * 
	 * @param userId
	 * @param groupId
	 * @param callback
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
	 * Creates a group with the specified group path. Group paths can be slash
	 * ("/") delimited like file paths for hierarchical group relationships.
	 * Executes asynchronously in background and the callbacks are called in the
	 * UI thread.
	 * 
	 * @param groupPath
	 * @param callback
	 */
	public void createGroupAsync(String groupPath,
			final ApiResponseCallback callback) {
		createGroupAsync(groupPath, null);
	}

	

	/**
	 * Creates a group with the specified group path and group title. Group
	 * paths can be slash ("/") deliminted like file paths for hierarchical
	 * group relationships. Executes asynchronously in background and the
	 * callbacks are called in the UI thread.
	 * 
	 * @param groupPath
	 * @param groupTitle
	 * @param callback
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
	 * Connect two entities together. Executes asynchronously in background and
	 * the callbacks are called in the UI thread.
	 * 
	 * @param connectingEntityType
	 * @param connectingEntityId
	 * @param connectionType
	 * @param connectedEntityId
	 * @param callback
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
	 * Disconnect two entities. Executes asynchronously in background and the
	 * callbacks are called in the UI thread.
	 * 
	 * @param connectingEntityType
	 * @param connectingEntityId
	 * @param connectionType
	 * @param connectedEntityId
	 * @param callback
	 */
	public void disconnectEntitiesAsync(final String connectingEntityType,
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
	 * Query the connected entities. Executes asynchronously in background and
	 * the callbacks are called in the UI thread.
	 * 
	 * @param connectingEntityType
	 * @param connectingEntityId
	 * @param connectionType
	 * @param ql
	 * @param callback
	 */
	public void queryEntityConnectionsAsync(String connectingEntityType,
			String connectingEntityId, String connectionType, String ql,
			QueryResultsCallback callback) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ql", ql);
		queryEntitiesRequestAsync(callback, HttpMethod.GET, params, null,
				getApplicationId(), connectingEntityType, connectingEntityId,
				connectionType);
	}


	/**
	 * Query the connected entities within distance of a specific point. .
	 * Executes asynchronously in background and the callbacks are called in the
	 * UI thread.
	 * 
	 * @param connectingEntityType
	 * @param connectingEntityId
	 * @param connectionType
	 * @param distance
	 * @param latitude
	 * @param longitude
	 * @param callback
	 */
	public void queryEntityConnectionsWithinLocationAsync(
			String connectingEntityType, String connectingEntityId,
			String connectionType, float distance, Location location,
			String ql, QueryResultsCallback callback) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ql", makeLocationQL(distance, location.getLatitude(), location.getLongitude(), ql));
		params.put("ql", ql);
		queryEntitiesRequestAsync(callback, HttpMethod.GET, params, null,
				getApplicationId(), connectingEntityType, connectingEntityId,
				connectionType);
	}

	
	
}
