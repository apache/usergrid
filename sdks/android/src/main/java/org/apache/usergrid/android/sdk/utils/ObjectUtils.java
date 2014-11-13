package org.apache.usergrid.android.sdk.utils;

import java.util.Map;

public class ObjectUtils {

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

}
