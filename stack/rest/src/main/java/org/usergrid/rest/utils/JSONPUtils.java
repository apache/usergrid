package org.usergrid.rest.utils;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

public class JSONPUtils {

	static Map<String, Set<String>> javascriptTypes = new HashMap<String, Set<String>>();

	static {
		// application/javascript, application/x-javascript, text/ecmascript,
		// application/ecmascript, text/jscript
		javascriptTypes.put(
				"application",
				new HashSet<String>(Arrays.asList("x-javascript", "ecmascript",
						"javascript")));
		javascriptTypes.put("text",
				new HashSet<String>(Arrays.asList("ecmascript", "jscript")));
	}

	public static boolean isJavascript(MediaType m) {
		if (m == null) {
			return false;
		}

		Set<String> subtypes = javascriptTypes.get(m.getType());
		if (subtypes == null) {
			return false;
		}

		return subtypes.contains(m.getSubtype());
	}

	public static boolean isJavascript(List<MediaType> l) {
		for (MediaType m : l) {
			if (isJavascript(m)) {
				return true;
			}
		}
		return false;
	}

	public static String wrapJSONPResponse(String callback, String jsonResponse) {
		if (isNotBlank(callback)) {
			return callback + "(" + jsonResponse + ")";
		} else {
			return jsonResponse;

		}
	}

	public static String wrapJSONPResponse(MediaType m, String callback,
			String jsonResponse) {
		if (isJavascript(m) && isNotBlank(callback)) {
			String jsResponse = callback + "(" + jsonResponse + ")";
			return jsResponse;
		} else {
			return jsonResponse;

		}
	}

}
