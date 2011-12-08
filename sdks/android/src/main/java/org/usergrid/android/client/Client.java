package org.usergrid.android.client;

import static org.usergrid.android.client.utils.JsonUtils.parse;
import static org.usergrid.android.client.utils.ObjectUtils.isEmpty;
import static org.usergrid.android.client.utils.UrlUtils.addQueryParams;
import static org.usergrid.android.client.utils.UrlUtils.encodeParams;
import static org.usergrid.android.client.utils.UrlUtils.path;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.node.JsonNodeFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.usergrid.android.client.entities.Device;
import org.usergrid.android.client.entities.Entity;
import org.usergrid.android.client.entities.Group;
import org.usergrid.android.client.entities.User;
import org.usergrid.android.client.response.ApiResponse;
import org.usergrid.android.client.utils.DeviceUuidFactory;

import android.content.Context;
import android.util.Log;

/**
 * @author edanuff
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
	 * Default constructor
	 */
	public Client() {
		init();
	}

	/**
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

	public String getCurrentOrganization() {
		return currentOrganization;
	}

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

	public ApiResponse createEntity(Entity entity) {
		assertValidApplicationId();
		if (isEmpty(entity.getType())) {
			throw new IllegalArgumentException("Missing entity type");
		}
		ApiResponse response = apiRequest(HttpMethod.POST, null, entity,
				applicationId, entity.getType());
		return response;
	}

	public ApiResponse createEntity(Map<String, Object> properties) {
		assertValidApplicationId();
		if (isEmpty(properties.get("type"))) {
			throw new IllegalArgumentException("Missing entity type");
		}
		ApiResponse response = apiRequest(HttpMethod.POST, null, properties,
				applicationId, properties.get("type").toString());
		return response;
	}

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

	public Query queryRequest(HttpMethod method, Map<String, Object> params,
			Object data, String... segments) {
		ApiResponse response = apiRequest(method, params, data, segments);
		return new Query(response, method, params, data, segments);
	}

	public Query queryUsers() {
		Query q = queryRequest(HttpMethod.GET, null, null, applicationId,
				"users");
		return q;
	}

	public Query queryUsers(String ql) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ql", ql);
		Query q = queryRequest(HttpMethod.GET, params, null, applicationId,
				"users");
		return q;
	}

	public Query queryUsersForGroup(String groupId) {
		Query q = queryRequest(HttpMethod.GET, null, null, applicationId,
				"groups", groupId);
		return q;
	}

	public ApiResponse addUserToGroup(String userId, String groupId) {
		return apiRequest(HttpMethod.POST, null, null, applicationId, "groups",
				groupId, "users", userId);
	}

	public ApiResponse createGroup(String groupPath) {
		return createGroup(groupPath, null);
	}

	public ApiResponse createGroup(String groupPath, String groupTitle) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("path", groupPath);
		if (groupTitle != null) {
			data.put("title", groupTitle);
		}
		return apiRequest(HttpMethod.POST, null, data, applicationId, "groups");
	}

	public class Query {
		final HttpMethod method;
		final Map<String, Object> params;
		final Object data;
		final String[] segments;
		final String prevCursor;
		final ApiResponse response;

		private Query(ApiResponse response, HttpMethod method,
				Map<String, Object> params, Object data, String[] segments) {
			this.response = response;
			this.method = method;
			this.params = params;
			this.data = data;
			this.segments = segments;
			prevCursor = null;
		}

		private Query(ApiResponse response, Query q) {
			this.response = response;
			method = q.method;
			params = q.params;
			data = q.data;
			segments = q.segments;
			prevCursor = (q.response != null) ? q.response.getCursor() : null;
		}

		public ApiResponse getResponse() {
			return response;
		}

		public boolean more() {
			if ((response != null) && (response.getCursor() != null)
					&& (response.getCursor().length() > 0)) {
				return true;
			}
			return false;
		}

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
				return new Query(nextResponse, this);
			}
			return null;
		}

	}

}
