package org.usergrid.android.client;

import static java.net.URLEncoder.encode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;

import android.util.Log;

public class Utils {

	private static final String TAG = "UsergridClientUtils";

	static ObjectMapper mapper = new ObjectMapper();

	public static URL url(String s) {
		try {
			return new URL(s);
		} catch (MalformedURLException e) {
			Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
		}
		return null;
	}

	public static URL url(URL u, String s) {
		try {
			return new URL(u, s);
		} catch (MalformedURLException e) {
			Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
		}
		return null;
	}

	public static String path(Object... segments) {
		String path = "";
		boolean first = true;
		for (Object segment : segments) {
			if (segment instanceof Object[]) {
				segment = path((Object[]) segment);
			}
			if (!isEmpty(segment)) {
				if (first) {
					path = segment.toString();
					first = false;
				} else {
					if (!path.endsWith("/")) {
						path += "/";
					}
					path += segment.toString();
				}
			}
		}
		return path;
	}

	public static boolean isEmpty(Object s) {
		if (s == null) {
			return true;
		}
		if ((s instanceof String) && (((String) s).trim().length() == 0)) {
			return true;
		}
		if (s instanceof Map) {
			return ((Map<?, ?>) s).isEmpty();
		}
		return false;
	}

	public static <T> Map<String, T> newMapWithoutKeys(Map<String, T> map,
			List<String> keys) {
		Map<String, T> newMap = null;
		if (map != null) {
			newMap = new HashMap<String, T>(map);
		} else {
			newMap = new HashMap<String, T>();
		}
		for (String key : keys) {
			newMap.remove(key);
		}
		return newMap;
	}

	@SuppressWarnings("rawtypes")
	public static String encodeParams(Map<String, Object> params) {
		if (params == null) {
			return "";
		}
		boolean first = true;
		StringBuilder results = new StringBuilder();
		for (Entry<String, Object> entry : params.entrySet()) {
			if (entry.getValue() instanceof List) {
				for (Object o : (List) entry.getValue()) {
					if (!isEmpty(o)) {
						if (!first) {
							results.append('&');
						}
						first = false;
						results.append(entry.getKey());
						results.append("=");
						try {
							results.append(encode(o.toString(), "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
						}
					}
				}
			} else if (!isEmpty(entry.getValue())) {
				if (!first) {
					results.append('&');
				}
				first = false;
				results.append(entry.getKey());
				results.append("=");
				try {
					results.append(encode(entry.getValue().toString(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
				}
			}
		}
		Log.i(TAG, results.toString());
		return results.toString();
	}

	public static String addQueryParams(String url, Map<String, Object> params) {
		if (params == null) {
			return url;
		}
		if (!url.contains("?")) {
			url += "?";
		}
		url += encodeParams(params);
		return url;
	}

	public static String getStringProperty(Map<String, JsonNode> properties,
			String name) {
		JsonNode value = properties.get(name);
		if (value != null) {
			return value.asText();
		}
		return null;
	}

	public static void setStringProperty(Map<String, JsonNode> properties,
			String name, String value) {
		if (value == null) {
			properties.remove(name);
		} else {
			properties.put(name, JsonNodeFactory.instance.textNode(value));
		}
	}

	public static Boolean getBooleanProperty(Map<String, JsonNode> properties,
			String name) {
		JsonNode value = properties.get(name);
		if (value != null) {
			return value.asBoolean();
		}
		return false;
	}

	public static void setBooleanProperty(Map<String, JsonNode> properties,
			String name, Boolean value) {
		if (value == null) {
			properties.remove(name);
		} else {
			properties.put(name, JsonNodeFactory.instance.booleanNode(value));
		}
	}

	public static UUID getUUIDProperty(Map<String, JsonNode> properties,
			String name) {
		JsonNode value = properties.get(name);
		if (value != null) {
			UUID uuid = null;
			try {
				uuid = UUID.fromString(value.asText());
			} catch (Exception e) {
			}
			return uuid;
		}
		return null;
	}

	public static void setUUIDProperty(Map<String, JsonNode> properties,
			String name, UUID value) {
		if (value == null) {
			properties.remove(name);
		} else {
			properties.put(name,
					JsonNodeFactory.instance.textNode(value.toString()));
		}
	}

	public static String toJsonString(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
			Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
		} catch (JsonMappingException e) {
			Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
		} catch (IOException e) {
			Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
		}
		return "{}";
	}

	public static <T> T parse(String json, Class<T> c) {
		try {
			return mapper.readValue(json, c);
		} catch (JsonParseException e) {
			Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
		} catch (JsonMappingException e) {
			Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
		} catch (IOException e) {
			Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
		}
		return null;
	}

}
