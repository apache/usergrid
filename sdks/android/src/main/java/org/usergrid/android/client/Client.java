package org.usergrid.android.client;

import static org.springframework.util.StringUtils.arrayToDelimitedString;
import static org.springframework.util.StringUtils.tokenizeToStringArray;
import static org.usergrid.android.client.utils.JsonUtils.parse;
import static org.usergrid.android.client.utils.ObjectUtils.isEmpty;
import static org.usergrid.android.client.utils.UrlUtils.addQueryParams;
import static org.usergrid.android.client.utils.UrlUtils.encodeParams;
import static org.usergrid.android.client.utils.UrlUtils.path;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.node.JsonNodeFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.usergrid.android.client.callbacks.ApiResponseCallback;
import org.usergrid.android.client.callbacks.ClientAsyncTask;
import org.usergrid.android.client.callbacks.DeviceRegistrationCallback;
import org.usergrid.android.client.callbacks.GroupsRetrievedCallback;
import org.usergrid.android.client.callbacks.QueryResultsCallback;
import org.usergrid.android.client.entities.Activity;
import org.usergrid.android.client.entities.Device;
import org.usergrid.android.client.entities.Entity;
import org.usergrid.android.client.entities.Group;
import org.usergrid.android.client.entities.User;
import org.usergrid.android.client.response.ApiResponse;
import org.usergrid.android.client.utils.DeviceUuidFactory;

import android.content.Context;
import android.location.Location;
import android.util.Log;

/**
 * The Client class for accessing the Usergrid API. Start by instantiating this
 * class though the appropriate constructor.
 * 
 */
public class Client {

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

	private String apiUrl = PUBLIC_API_URL;

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
	public Client(String applicationId) {
		init();
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
			Log.i(TAG, "Authorization: " + auth);
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
		Log.i(TAG, "Client.httpRequest(): url: " + url);
		ResponseEntity<T> responseEntity = restTemplate.exchange(url, method,
				requestEntity, cls);
		Log.i(TAG, "Client.httpRequest(): reponse body: "
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
			Log.i(TAG, "Client.apiRequest(): Response: " + response);
		} catch (HttpClientErrorException e) {
			Log.e(TAG,
					"Client.apiRequest(): HTTP error: "
							+ e.getLocalizedMessage());
			response = parse(e.getResponseBodyAsString(), ApiResponse.class);
			if ((response != null) && !isEmpty(response.getError())) {
				Log.e(TAG,
						"Client.apiRequest(): Response error: "
								+ response.getError());
				if (!isEmpty(response.getException())) {
					Log.e(TAG, "Client.apiRequest(): Response exception: "
							+ response.getException());
				}
			}
		}
		return response;
	}

	void assertValidApplicationId() {
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
		assertValidApplicationId();
		loggedInUser = null;
		accessToken = null;
		currentOrganization = null;
		Map<String, Object> formData = new HashMap<String, Object>();
		formData.put("grant_type", "password");
		formData.put("username", email);
		formData.put("password", password);
		ApiResponse response = apiRequest(HttpMethod.POST, formData, null,
				applicationId, "token");
		if (response == null) {
			return response;
		}
		if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
			loggedInUser = response.getUser();
			accessToken = response.getAccessToken();
			currentOrganization = null;
			Log.i(TAG, "Client.authorizeAppUser(): Access token: "
					+ accessToken);
		} else {
			Log.i(TAG, "Client.authorizeAppUser(): Response: " + response);
		}
		return response;
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
	 * 
	 * @param email
	 * @param pin
	 * @return non-null ApiResponse if request succeeds, check getError() for
	 *         "invalid_grant" to see if access is denied.
	 */
	public ApiResponse authorizeAppUserViaPin(String email, String pin) {
		assertValidApplicationId();
		loggedInUser = null;
		accessToken = null;
		currentOrganization = null;
		Map<String, Object> formData = new HashMap<String, Object>();
		formData.put("grant_type", "pin");
		formData.put("username", email);
		formData.put("pin", pin);
		ApiResponse response = apiRequest(HttpMethod.POST, formData, null,
				applicationId, "token");
		if (response == null) {
			return response;
		}
		if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
			loggedInUser = response.getUser();
			accessToken = response.getAccessToken();
			currentOrganization = null;
			Log.i(TAG, "Client.authorizeAppUser(): Access token: "
					+ accessToken);
		} else {
			Log.i(TAG, "Client.authorizeAppUser(): Response: " + response);
		}
		return response;
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
	 * Log the user in with their Facebook access token retrived via Facebook
	 * OAuth.
	 * 
	 * @param email
	 * @param pin
	 * @return non-null ApiResponse if request succeeds, check getError() for
	 *         "invalid_grant" to see if access is denied.
	 */
	public ApiResponse authorizeAppUserViaFacebook(String fb_access_token) {
		assertValidApplicationId();
		loggedInUser = null;
		accessToken = null;
		currentOrganization = null;
		Map<String, Object> formData = new HashMap<String, Object>();
		formData.put("fb_access_token", fb_access_token);
		ApiResponse response = apiRequest(HttpMethod.POST, formData, null,
				applicationId, "auth", "facebook");
		if (response == null) {
			return response;
		}
		if (!isEmpty(response.getAccessToken()) && (response.getUser() != null)) {
			loggedInUser = response.getUser();
			accessToken = response.getAccessToken();
			currentOrganization = null;
			Log.i(TAG, "Client.authorizeAppUserViaFacebook(): Access token: "
					+ accessToken);
		} else {
			Log.i(TAG, "Client.authorizeAppUserViaFacebook(): Response: "
					+ response);
		}
		return response;
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
	 * for production apps.
	 * 
	 * @param email
	 * @param pin
	 * @return non-null ApiResponse if request succeeds, check getError() for
	 *         "invalid_grant" to see if access is denied.
	 */
	public ApiResponse authorizeAppClient(String clientId, String clientSecret) {
		assertValidApplicationId();
		loggedInUser = null;
		accessToken = null;
		currentOrganization = null;
		Map<String, Object> formData = new HashMap<String, Object>();
		formData.put("grant_type", "client_credentials");
		formData.put("client_id", clientId);
		formData.put("client_secret", clientSecret);
		ApiResponse response = apiRequest(HttpMethod.POST, formData, null,
				applicationId, "token");
		if (response == null) {
			return response;
		}
		if (!isEmpty(response.getAccessToken())) {
			loggedInUser = null;
			accessToken = response.getAccessToken();
			currentOrganization = null;
			Log.i(TAG, "Client.authorizeAppClient(): Access token: "
					+ accessToken);
		} else {
			Log.i(TAG, "Client.authorizeAppClient(): Response: " + response);
		}
		return response;
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
	 * Registers a device using the device's unique device ID.
	 * 
	 * @param context
	 * @param properties
	 * @return a Device object if success
	 */
	public Device registerDevice(Context context, Map<String, Object> properties) {
		assertValidApplicationId();
		UUID deviceId = new DeviceUuidFactory(context).getDeviceUuid();
		if (properties == null) {
			properties = new HashMap<String, Object>();
		}
		properties.put("refreshed", System.currentTimeMillis());
		ApiResponse response = apiRequest(HttpMethod.PUT, null, properties,
				applicationId, "devices", deviceId.toString());
		return response.getFirstEntity(Device.class);
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
				return registerDevice(context, properties);
			}
		}).execute();
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
				applicationId, entity.getType());
		return response;
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
				applicationId, properties.get("type").toString());
		return response;
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
	 * Get the groups for the user.
	 * 
	 * @param userId
	 * @return a map with the group path as the key and the Group entity as the
	 *         value
	 */
	public Map<String, Group> getGroupsForUser(String userId) {
		ApiResponse response = apiRequest(HttpMethod.GET, null, null,
				applicationId, "users", userId, "groups");
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
	 * Get a user's activity feed. Returned as a query to ease paging.
	 * 
	 * @param userId
	 * @return
	 */
	public Query queryActivityFeedForUser(String userId) {
		Query q = queryEntitiesRequest(HttpMethod.GET, null, null,
				applicationId, "users", userId, "feed");
		return q;
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
	public void queryActivityFeedForUserAsync(final String userId,
			final QueryResultsCallback callback) {
		(new ClientAsyncTask<Query>(callback) {
			@Override
			public Query doTask() {
				return queryActivityFeedForUser(userId);
			}
		}).execute();
	}

	/**
	 * Posts an activity to a user. Activity must already be created.
	 * 
	 * @param userId
	 * @param activity
	 * @return
	 */
	public ApiResponse postUserActivity(String userId, Activity activity) {
		return apiRequest(HttpMethod.POST, null, null, applicationId, "users",
				userId, "activities", activity.getUuid().toString());
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
		createEntity(activity);
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
	 * @param userId
	 * @param activity
	 * @return
	 */
	public ApiResponse postGroupActivity(String groupId, Activity activity) {
		return apiRequest(HttpMethod.POST, null, null, applicationId, "groups",
				groupId, "activities", activity.getUuid().toString());
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
	public ApiResponse postGroupActivity(String verb, String title,
			String content, String category, User user, Entity object,
			String objectType, String objectName, String objectContent) {
		Activity activity = Activity.newActivity(verb, title, content,
				category, user, object, objectType, objectName, objectContent);
		createEntity(activity);
		return postGroupActivity(user.getUuid().toString(), activity);
	}

	/**
	 * Creates and posts an activity to a group. Executes asynchronously in
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
	public void postGroupActivityAsync(final String verb, final String title,
			final String content, final String category, final User user,
			final Entity object, final String objectType,
			final String objectName, final String objectContent,
			final ApiResponseCallback callback) {
		(new ClientAsyncTask<ApiResponse>(callback) {
			@Override
			public ApiResponse doTask() {
				return postGroupActivity(verb, title, content, category, user,
						object, objectType, objectName, objectContent);
			}
		}).execute();
	}

	/**
	 * Get a group's activity feed. Returned as a query to ease paging.
	 * 
	 * @param userId
	 * @return
	 */
	public Query queryActivityFeedForGroup(String groupId) {
		Query q = queryEntitiesRequest(HttpMethod.GET, null, null,
				applicationId, "groups", groupId, "feed");
		return q;
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
	 * Perform a query of the users collection.
	 * 
	 * @return
	 */
	public Query queryUsers() {
		Query q = queryEntitiesRequest(HttpMethod.GET, null, null,
				applicationId, "users");
		return q;
	}

	/**
	 * Perform a query of the users collection. Executes asynchronously in
	 * background and the callbacks are called in the UI thread.
	 * 
	 * @param callback
	 */
	public void queryUsersAsync(QueryResultsCallback callback) {
		queryEntitiesRequestAsync(callback, HttpMethod.GET, null, null,
				applicationId, "users");
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
		Query q = queryEntitiesRequest(HttpMethod.GET, params, null,
				applicationId, "users");
		return q;
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
				applicationId, "users");
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
	public Query queryUsersWithinLocation(float distance, Location location,
			String ql) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ql", this.makeLocationQL(distance, location, ql));
		Query q = queryEntitiesRequest(HttpMethod.GET, params, null,
				applicationId, "users");
		return q;
	}

	/**
	 * Perform a query of the users collection within the specified distance of
	 * the specified location and optionally using the provided query command.
	 * For example: "name contains 'ed'". Executes asynchronously in background
	 * and the callbacks are called in the UI thread.
	 * 
	 * @param distance
	 * @param location
	 * @param callback
	 */
	public void queryUsersNearLocation(float distance, Location location,
			String ql, QueryResultsCallback callback) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ql", this.makeLocationQL(distance, location, ql));
		queryEntitiesRequestAsync(callback, HttpMethod.GET, params, null,
				applicationId, "users");
	}

	/**
	 * Queries the users for the specified group.
	 * 
	 * @param groupId
	 * @return
	 */
	public Query queryUsersForGroup(String groupId) {
		Query q = queryEntitiesRequest(HttpMethod.GET, null, null,
				applicationId, "groups", groupId, "users");
		return q;
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
				applicationId, "groups", groupId, "users");
	}

	/**
	 * Adds a user to the specified groups.
	 * 
	 * @param userId
	 * @param groupId
	 * @return
	 */
	public ApiResponse addUserToGroup(String userId, String groupId) {
		return apiRequest(HttpMethod.POST, null, null, applicationId, "groups",
				groupId, "users", userId);
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
	 * 
	 * @param groupPath
	 * @return
	 */
	public ApiResponse createGroup(String groupPath) {
		return createGroup(groupPath, null);
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
	 * paths can be slash ("/") delimited like file paths for hierarchical group
	 * relationships.
	 * 
	 * @param groupPath
	 * @param groupTitle
	 * @return
	 */
	public ApiResponse createGroup(String groupPath, String groupTitle) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("type", "group");
		data.put("path", groupPath);
		if (groupTitle != null) {
			data.put("title", groupTitle);
		}
		return apiRequest(HttpMethod.POST, null, data, applicationId, "groups");
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
		return apiRequest(HttpMethod.POST, null, null, applicationId,
				connectingEntityType, connectingEntityId, connectionType,
				connectedEntityId);
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
		return apiRequest(HttpMethod.DELETE, null, null, applicationId,
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
				return connectEntities(connectingEntityType,
						connectingEntityId, connectionType, connectedEntityId);
			}
		}).execute();
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
				applicationId, connectingEntityType, connectingEntityId,
				connectionType);
		return q;
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
				applicationId, connectingEntityType, connectingEntityId,
				connectionType);
	}

	private String makeLocationQL(float distance, Location location, String ql) {
		ql = (location != null ? "within " + distance + " of "
				+ location.getLatitude() + ", " + location.getLongitude() : "")
				+ (((ql != null) && (ql.trim().length() > 0)) ? " and " + ql
						: "");
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
			String connectionType, float distance, Location location, String ql) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ql", makeLocationQL(distance, location, ql));
		Query q = queryEntitiesRequest(HttpMethod.GET, params, null,
				applicationId, connectingEntityType, connectingEntityId,
				connectionType);
		return q;
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
		params.put("ql", makeLocationQL(distance, location, ql));
		params.put("ql", ql);
		queryEntitiesRequestAsync(callback, HttpMethod.GET, params, null,
				applicationId, connectingEntityType, connectingEntityId,
				connectionType);
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
		return apiRequest(HttpMethod.POST, null, message, applicationId,
				"queues", normalizeQueuePath(path));
	}

	public ApiResponse postMessage(String path,
			List<Map<String, Object>> messages) {
		return apiRequest(HttpMethod.POST, null, messages, applicationId,
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
		return apiRequest(HttpMethod.GET, params, null, applicationId,
				"queues", normalizeQueuePath(path));
	}

	public ApiResponse addSubscriber(String publisherQueue,
			String subscriberQueue) {
		return apiRequest(HttpMethod.POST, null, null, applicationId, "queues",
				normalizeQueuePath(publisherQueue), "subscribers",
				normalizeQueuePath(subscriberQueue));
	}

	public ApiResponse removeSubscriber(String publisherQueue,
			String subscriberQueue) {
		return apiRequest(HttpMethod.DELETE, null, null, applicationId,
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
