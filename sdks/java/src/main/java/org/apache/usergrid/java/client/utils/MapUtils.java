package org.apache.usergrid.java.client.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapUtils {

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

}
