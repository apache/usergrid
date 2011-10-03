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
package org.usergrid.persistence;

import java.util.Map;
import java.util.TreeMap;

public class EntityUtils {

	public static Map<String, Object> getSchemaProperties(String entityType,
			Map<String, Object> properties) {

		Map<String, Object> sys_props = new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER);

		for (String propName : properties.keySet()) {
			if (Schema.getDefaultSchema().hasProperty(entityType, propName)) {
				Object propValue = properties.get(propName);
				sys_props.put(propName, propValue);
			}
		}

		return sys_props;

	}

	public static Map<String, Object> getDynamicProperties(String entityType,
			Map<String, Object> properties) {

		Map<String, Object> sys_props = new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER);

		for (String propName : properties.keySet()) {
			if (!Schema.getDefaultSchema().hasProperty(entityType, propName)) {
				Object propValue = properties.get(propName);
				sys_props.put(propName, propValue);
			}
		}

		return sys_props;

	}

}
