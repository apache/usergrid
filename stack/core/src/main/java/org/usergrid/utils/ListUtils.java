/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.usergrid.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;

public class ListUtils extends org.apache.commons.collections.ListUtils {

	public static <A> A first(List<A> list) {
		if (list == null) {
			return null;
		}
		if (list.size() == 0) {
			return null;
		}
		return list.get(0);
	}

	public static <A> A last(List<A> list) {
		if (list == null) {
			return null;
		}
		if (list.size() == 0) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	public static <A> Integer firstInteger(List<A> list) {
		A a = first(list);
		if (a == null) {
			return null;
		}
		if (a instanceof Integer) {
			return (Integer) a;
		}
		try {
			return NumberUtils.toInt((String) a);
		} catch (Exception e) {
		}
		return null;
	}

	public static <A> Long firstLong(List<A> list) {
		A a = first(list);
		if (a == null) {
			return null;
		}
		if (a instanceof Long) {
			return (Long) a;
		}
		try {
			return NumberUtils.toLong((String) a);
		} catch (Exception e) {
		}
		return null;
	}

	public static <A> Boolean firstBoolean(List<A> list) {
		A a = first(list);
		if (a == null) {
			return null;
		}
		if (a instanceof Boolean) {
			return (Boolean) a;
		}
		try {
			return Boolean.parseBoolean((String) a);
		} catch (Exception e) {
		}
		return null;
	}

	public static <A> UUID firstUuid(List<A> list) {
		A i = first(list);
		if (i == null) {
			return null;
		}
		if (i instanceof UUID) {
			return (UUID) i;
		}
		try {
			return UUIDUtils.tryGetUUID((String) i);
		} catch (Exception e) {
		}
		return null;
	}

	public static boolean isEmpty(List<?> list) {
		return (list == null) || (list.size() == 0);
	}

	public static <T> List<T> dequeueCopy(List<T> list) {
		if (!isEmpty(list)) {
			list = list.subList(1, list.size());
		}
		return list;
	}

	public static <T> List<T> queueCopy(List<T> list, T item) {
		if (!isEmpty(list)) {
			list = new ArrayList<T>(list);
		} else {
			list = new ArrayList<T>();
		}
		list.add(item);
		return list;
	}

	public static <T> List<T> initCopy(List<T> list) {
		if (!isEmpty(list)) {
			list = new ArrayList<T>(list);
		} else {
			list = new ArrayList<T>();
		}
		return list;
	}

	public static <T> T dequeue(List<T> list) {
		if (!isEmpty(list)) {
			return list.remove(0);
		}
		return null;
	}

	public static <T> List<T> queue(List<T> list, T item) {
		if (list == null) {
			list = new ArrayList<T>();
		}
		list.add(item);
		return list;
	}

	public static <T> List<T> requeue(List<T> list, T item) {
		if (list == null) {
			list = new ArrayList<T>();
		}
		list.add(0, item);
		return list;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<?> flatten(Collection<?> l) {
		boolean hasCollection = false;
		for (Object o : l) {
			if (o instanceof Collection) {
				hasCollection = true;
				break;
			}
		}
		if (!hasCollection && (l instanceof List)) {
			return (List<?>) l;
		}
		List newList = new ArrayList();
		for (Object o : l) {
			if (o instanceof List) {
				newList.addAll(flatten((List) o));
			} else {
				newList.add(o);
			}
		}
		return newList;
	}

	public static boolean anyNull(List<?> l) {
		for (Object o : l) {
			if (o == null) {
				return true;
			}
		}
		return false;
	}

	public static boolean anyNull(Object... objects) {
		for (Object o : objects) {
			if (o == null) {
				return true;
			}
		}
		return false;
	}

}
