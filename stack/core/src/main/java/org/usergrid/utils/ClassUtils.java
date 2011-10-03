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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClassUtils extends org.apache.commons.lang.ClassUtils {

	@SuppressWarnings("unchecked")
	public static <A> Class<A> tryGetClass(Class<A> baseCls, String className) {

		try {
			Class<A> cls = (Class<A>) Class.forName(className);
			if (cls != null) {
				return cls;
			}
		} catch (ClassCastException e) {
			// logger.warn("Unable to convert to a typed entity (" + entityType
			// + "): " + e.getMessage());
		} catch (ClassNotFoundException e) {
			// logger.warn("Unable to convert to a typed entity (" + entityType
			// + "): " + e.getMessage());
		}

		return null;

	}

	@SuppressWarnings("unchecked")
	public static <A, B> B cast(A a) {
		return (B) a;
	}

	@SuppressWarnings("unchecked")
	private static final Set<Class<?>> WRAPPER_TYPES = new HashSet<Class<?>>(
			Arrays.asList(Boolean.class, Byte.class, Character.class,
					Double.class, Float.class, Integer.class, Long.class,
					Short.class, Void.class));

	public static boolean isWrapperType(Class<?> clazz) {
		return WRAPPER_TYPES.contains(clazz);
	}

	public static boolean isWrapperType2(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		return clazz.equals(Boolean.class) || clazz.equals(Integer.class)
				|| clazz.equals(Character.class) || clazz.equals(Byte.class)
				|| clazz.equals(Short.class) || clazz.equals(Double.class)
				|| clazz.equals(Long.class) || clazz.equals(Float.class);
	}

	public static boolean isPrimitiveType(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		return clazz.isPrimitive() || isWrapperType(clazz);
	}

	public static boolean isBasicType(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		return (String.class.isAssignableFrom(clazz)) || isPrimitiveType(clazz);
	}

}
