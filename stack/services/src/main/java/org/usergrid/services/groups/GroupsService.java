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
package org.usergrid.services.groups;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.services.AbstractPathBasedColllectionService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceResults;

public class GroupsService extends AbstractPathBasedColllectionService {

	private static final Logger logger = LoggerFactory.getLogger(GroupsService.class);

  static CharMatcher matcher = CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.anyOf("-./"));

	public GroupsService() {
		super();
		logger.info("/groups");

		// hiddenConnections = new LinkedHashSet<String>();
		// hiddenConnections.add("members");

		// addedCollections = new LinkedHashSet<String>();
		// addedCollections.add("members");

		declareEntityDictionary("rolenames");
	}

	@Override
	public ServiceResults postCollection(ServiceContext context)
			throws Exception {

    String path = (String)context.getProperty("path");

    logger.info("Creating group with path {}", path);

    Preconditions.checkArgument(matcher.matchesAllOf(path),
            "Illegal characters found in group name: " + path);

		return super.postCollection(context);
	}

}
