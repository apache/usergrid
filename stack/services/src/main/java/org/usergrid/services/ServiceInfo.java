/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
package org.usergrid.services;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.apache.commons.lang.StringUtils.split;
import static org.usergrid.utils.InflectionUtils.pluralize;
import static org.usergrid.utils.InflectionUtils.singularize;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterLast;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeLast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.usergrid.persistence.Schema;

public class ServiceInfo {

	private final String name;
	private final boolean rootService;
	private final String rootType;
	private final String containerType;
	private final String collectionName;
	private final String itemType;
	private final List<String> patterns;
	private final List<String> collections;

	public ServiceInfo(String name, boolean rootService, String rootType,
			String containerType, String collectionName, String itemType,
			List<String> patterns, List<String> collections) {
		this.name = name;
		this.rootService = rootService;
		this.rootType = rootType;
		this.containerType = containerType;
		this.collectionName = collectionName;
		this.itemType = itemType;
		this.patterns = patterns;
		this.collections = collections;
	}

	public static String normalizeServicePattern(String s) {
		if (s == null) {
			return null;
		}
		s = s.trim().toLowerCase();

		s = removeEnd(s, "/");
		s = removeEnd(s, "/*");

		if (!s.startsWith("/")) {
			s = "/" + s;
		}

		return s;
	}

	public static List<String> getPatterns(String servicePattern) {

		String[] collections = split(servicePattern, "/*/");
		return getPatterns(servicePattern, collections);
	}

	public static List<String> getPatterns(String servicePattern,
			String[] collections) {

		if (collections == null) {
			collections = split(servicePattern, "/*/");
		}

		List<String> patterns = new ArrayList<String>();
		patterns.add(servicePattern);
		if (servicePattern.indexOf(':') >= 0) {
			patterns.add(removeTypeSpecifiers(collections));
		}

		String s = getFallbackPattern(collections, 0, collections.length - 1);
		while (s != null) {
			patterns.add(s);
			s = getFallbackPattern(s);
		}

		return patterns;

	}

	private static String removeTypeSpecifiers(String[] collections) {
		String s = "";
		boolean first = true;

		for (String collection : collections) {
			if (!first) {
				s += "/*";
			}
			first = false;
			s += "/" + stringOrSubstringBeforeFirst(collection, ':');
		}
		return s;
	}

	private static String getFallbackPattern(String servicePattern) {
		String[] collections = split(servicePattern, "/*/");
		return getFallbackPattern(collections, 0, collections.length - 1);
	}

	private static String getFallbackPattern(String[] collections, int first,
			int last) {

		if (last < first) {
			return null;
		}

		if ((last - first) == 1) {
			if (!collections[first].startsWith("entities")) {
				return "/entities:" + singularize(collections[first]) + "/*/"
						+ collections[first + 1];
			}
			return null;
		}

		if ((last - first) == 0) {
			if (!collections[first].startsWith("entities")) {
				return "/entities:" + singularize(collections[first]);
			}
			return null;
		}

		int i = last - 1;
		while (i >= first) {
			if (collections[i].indexOf(':') > -1) {
				break;
			}
			i--;
		}

		if (i >= first) {
			String type = stringOrSubstringAfterLast(collections[i], ':');
			String fallback = "/" + pluralize(type);
			i++;
			while (i <= last) {
				fallback += "/*/" + collections[i];
				i++;
			}
			return fallback;
		}

		String eType = determineType(collections, first, last - 1);
		if (eType != "entity") {
			return "/entities:" + eType + "/*/" + collections[last];
		}

		return "/entities/*/" + collections[last];

	}

	public static String determineType(String servicePattern) {

		String[] collections = split(servicePattern, '/');
		return determineType(collections, 0, collections.length - 1);
	}

	private static String determineType(String[] collections, int first,
			int last) {

		if (last < first) {
			return null;
		}

		if (first == last) {
			return singularize(stringOrSubstringAfterLast(collections[0], ':'));
		}

		int i = first + 1;
		String containerType = singularize(collections[first]);

		while (i <= last) {
			String collectionName = stringOrSubstringBeforeFirst(
					collections[i], ':');
			String nextType = Schema.getDefaultSchema().getCollectionType(
					containerType, collectionName);
			if (nextType == null) {
				if (collections[i].indexOf(':') >= 0) {
					nextType = stringOrSubstringAfterLast(collections[i], ':');
				} else if ((i < last) && (collections[last].indexOf(':') >= 0)) {
					nextType = stringOrSubstringAfterLast(collections[last],
							':');
				} else {
					return "entity";
				}
			}
			containerType = nextType;
			i++;
		}
		return containerType;
	}

	public static String getClassName(String servicePattern) {
		servicePattern = normalizeServicePattern(servicePattern);

		String[] collections = split(servicePattern, "/*/");

		if (collections[0].startsWith("entities")) {
			if (collections.length == 1) {
				return "generic.RootCollectionService";
			}
			if (collections[0].indexOf(':') < 0) {
				return "generic.GenericConnectionsService";
			}
			String container = stringOrSubstringAfterLast(collections[0], ':');
			String collectionName = stringOrSubstringBeforeFirst(
					collections[1], ':');
			if (Schema.getDefaultSchema().hasCollection(container,
					collectionName)) {
				return "generic.GenericCollectionService";
			}
			return "generic.GenericConnectionsService";
		}

		String packages = "";

		String types = "";

		if (collections.length == 1) {
			packages = stringOrSubstringBeforeLast(
					stringOrSubstringBeforeFirst(collections[0], ':'), '.')
					+ ".";
		} else {
			for (int i = 0; i < collections.length; i++) {
				if (i == 0) {
					packages = stringOrSubstringBeforeFirst(collections[i], ':')
							+ ".";
				} else {
					packages += stringOrSubstringBeforeLast(
							stringOrSubstringBeforeFirst(collections[i], ':'),
							'.')
							+ ".";
				}
				if ((i < (collections.length - 1))
						&& (collections[i].indexOf(':') >= 0)) {
					types += capitalize(stringOrSubstringAfterLast(
							stringOrSubstringAfterLast(collections[i], ':'),
							'.'));
				}
			}
		}

		return packages
				+ types
				+ capitalize(stringOrSubstringAfterLast(
						stringOrSubstringBeforeFirst(
								collections[collections.length - 1], ':'), '.'))
				+ "Service";

	}

	private static final Map<String, ServiceInfo> serviceInfoCache = new LinkedHashMap<String, ServiceInfo>();

	public static ServiceInfo getServiceInfo(String servicePattern) {
		if (servicePattern == null) {
			return null;
		}

		servicePattern = normalizeServicePattern(servicePattern);

		ServiceInfo info = serviceInfoCache.get(servicePattern);

		if (info != null) {
			return info;
		}

		String[] collections = split(servicePattern, "/*/");

		if (collections.length == 0) {
			return null;
		}

		String collectionName = stringOrSubstringBeforeFirst(
				collections[collections.length - 1], ':');

		if (collectionName == null) {
			throw new NullPointerException("Collection name is null");
		}

		String ownerType = "entity";

		String rootType = determineType(collections, 0, 0);

		if (collections.length == 1) {
			ownerType = "application";
		}

		if (collections.length > 1) {
			ownerType = determineType(collections, 0, collections.length - 2);
		}

		String itemType = determineType(collections, 0, collections.length - 1);

		List<String> patterns = getPatterns(servicePattern, collections);
		info = new ServiceInfo(servicePattern, collections.length == 1,
				rootType, ownerType, collectionName, itemType, patterns,
				Arrays.asList(collections));

		serviceInfoCache.put(servicePattern, info);

		return info;
	}

	public String getClassName() {
		return getClassName(name);
	}

	public String getName() {
		return name;
	}

	public boolean isRootService() {
		return rootService;
	}

	public String getRootType() {
		return rootType;
	}

	public boolean isGenericRootType() {
		return ("entity".equals(rootType)) || ("entities".equals(rootType));
	}

	public String getContainerType() {
		return containerType;
	}

	public boolean isContainerType() {
		return ("entity".equals(containerType))
				|| ("entities".equals(containerType));
	}

	public String getCollectionName() {
		return collectionName;
	}

	public String getItemType() {
		return itemType;
	}

	public boolean isGenericItemType() {
		return "entity".equals(itemType);
	}

	public List<String> getPatterns() {
		return patterns;
	}

	public List<String> getCollections() {
		return collections;
	}

}
