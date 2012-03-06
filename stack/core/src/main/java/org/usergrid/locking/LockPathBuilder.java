/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.locking;

import static org.usergrid.persistence.Schema.defaultCollectionName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.usergrid.persistence.EntityRef;

/**
 * Helper class that contains the logic to build a lock path
 */
public class LockPathBuilder {

	private static final String SLASH = "/";
	private static final String AT = "@";

	public static String buildPath(UUID applicationId, UUID entityId) {
		StringBuilder builder = new StringBuilder();
		builder.append(SLASH);
		builder.append(applicationId.toString());
		builder.append(SLASH);
		builder.append(entityId.toString());
		return builder.toString();
	}

	public static String buildPath(EntityRef application, EntityRef entity) {
		StringBuilder builder = new StringBuilder();
		builder.append(SLASH);
		builder.append(application.getUuid().toString());
		builder.append(SLASH);
		builder.append(entity.getUuid().toString());
		return builder.toString();
	}

	public static List<String> buildPath(EntityRef application,
			EntityRef[] entities) {
		List<String> paths = new ArrayList<String>(entities.length);

		for (EntityRef ref : entities) {
			paths.add(buildPath(application, ref));
		}

		// Sort the paths
		Collections.sort(paths);

		return paths;
	}

	public static List<String> buildPath(UUID applicationId, UUID[] entityIDs) {
		List<String> paths = new ArrayList<String>(entityIDs.length);

		for (UUID aUUID : entityIDs) {
			paths.add(buildPath(applicationId, aUUID));
		}

		// Sort the paths
		Collections.sort(paths);

		return paths;
	}

	public static String buildPath(UUID applicationId, String path) {
		StringBuilder builder = new StringBuilder();
		builder.append(SLASH);
		builder.append(applicationId.toString());
		builder.append(SLASH);
		builder.append(path);
		return builder.toString();
	}

	public static List<String> buildPath(UUID applicationId, String[] pathsParam) {
		List<String> paths = new ArrayList<String>(pathsParam.length);

		for (String aPath : paths) {
			paths.add(buildPath(applicationId, aPath));
		}

		// Sort the paths
		Collections.sort(paths);

		return paths;
	}

	public static String buildPropertyPath(UUID applicationId,
			String entityType, String propertyName) {
		StringBuilder builder = new StringBuilder();
		builder.append(SLASH);
		builder.append(applicationId.toString());
		builder.append(SLASH);
		builder.append(defaultCollectionName(entityType));
		builder.append(SLASH);
		builder.append(AT);
		builder.append(propertyName);
		return builder.toString();
	}

	public static List<String> buildPropertyPaths(UUID applicationId,
			String entityType, String... propertyNames) {
		List<String> paths = new ArrayList<String>(propertyNames.length);
		for (String propertyName : propertyNames) {
			paths.add(buildPropertyPath(applicationId, entityType, propertyName));
		}
		Collections.sort(paths);
		return paths;

	}

}
