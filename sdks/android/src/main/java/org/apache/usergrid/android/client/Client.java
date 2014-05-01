package org.apache.usergrid.android.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

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
import android.util.Log;

/**
 * The Client class for accessing the Usergrid API. Start by instantiating this
 * class though the appropriate constructor.
 * 
 */
public class Client extends org.usergrid.java.client.Client {


  /**
   * Default tag applied to all logging messages sent from an instance of Client
   */
  public static final String LOGGING_TAG = "UsergridClient";	

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

	private String organizationId;
  private String applicationId;
  private String clientId;
  private String clientSecret;

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
		this.organizationId = organizationId;
		this.applicationId = applicationId;		
	}

  /**
   * Logs a trace-level logging message with tag 'UsergridClient'
   *
   * @param   logMessage  the message to log
   */
  public void logTrace(String logMessage) {
  	if(logMessage != null) {
  		Log.v(LOGGING_TAG,logMessage);
  	}
  }
  
  /**
   * Logs a debug-level logging message with tag 'UsergridClient'
   *
   * @param   logMessage  the message to log
   */
  public void logDebug(String logMessage) {
  	if(logMessage != null) {
  		Log.d(LOGGING_TAG,logMessage);
  	}
  }
  
  /**
   * Logs an info-level logging message with tag 'UsergridClient'
   *
   * @param   logMessage  the message to log
   */
  public void logInfo(String logMessage) {
  	if(logMessage != null) {
  		Log.i(LOGGING_TAG,logMessage);
  	}
  }
  
  /**
   * Logs a warn-level logging message with tag 'UsergridClient'
   *
   * @param   logMessage  the message to log
   */
  public void logWarn(String logMessage) {
  	if(logMessage != null) {
  		Log.w(LOGGING_TAG,logMessage);
  	}
  }
  
  /**
   * Logs an error-level logging message with tag 'UsergridClient'
   *
   * @param   logMessage  the message to log
   */
  public void logError(String logMessage) {
  	if(logMessage != null) {
  		Log.e(LOGGING_TAG,logMessage);
  	}
  }

  /**
   * Logs a debug-level logging message with tag 'UsergridClient'
   *
   * @param   logMessage  the message to log
   */
  public void writeLog(String logMessage) {
    //TODO: do we support different log levels in this class?
    Log.d(LOGGING_TAG, logMessage);      
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
     * Log out a user and destroy the access token currently stored in DataClient 
     * on the server and in the DataClient.
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
     * Log out a user and destroy the access token currently stored in DataClient 
     * on the server and in the DataClient.
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
     * from the DataClient instance, if it matches the token provided.
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
     * from the DataClient instance, if it matches the token provided.
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
     * The token stored in DataClient will also be destroyed.
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
     * The token stored in DataClient will also be destroyed.
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
				getOrganizationId(), getApplicationId(), "users");
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
				getOrganizationId(), getApplicationId(), "users");
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
				getOrganizationId(), getApplicationId(), connectingEntityType, connectingEntityId,
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
				getOrganizationId(), getApplicationId(), connectingEntityType, connectingEntityId,
				connectionType);
	}

	
	
}
