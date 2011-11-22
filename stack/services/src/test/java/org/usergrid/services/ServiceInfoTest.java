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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.utils.JsonUtils;

public class ServiceInfoTest {

	private static final Logger logger = LoggerFactory
			.getLogger(ServiceInfoTest.class);

	@Test
	public void testServiceInfo() throws Exception {

		testServiceInfo("/users", "users.UsersService");
		testServiceInfo("/users/*/messages", "users.messages.MessagesService");
		testServiceInfo("/users/*/messages/*/likes",
				"users.messages.likes.LikesService");
		testServiceInfo("/users/*/groups:group", "users.groups.GroupsService");
		testServiceInfo("/users/*/likes", "users.likes.LikesService");
		testServiceInfo("/users/*/likes:bar/*/wines",
				"users.likes.wines.BarWinesService");
		testServiceInfo("/restaurants", "restaurants.RestaurantsService");
		testServiceInfo("/blogpack.posts/", "blogpack.PostsService");
		testServiceInfo("/blogpack.posts/*/comments",
				"blogpack.posts.comments.CommentsService");
		testServiceInfo("/blogpack.posts/*/comments:blogpost.comment",
				"blogpack.posts.comments.CommentsService");
	}

	@Test
	public void testFallback() throws Exception {

		dumpFallback("/users/*/friends/*/recommendations:food");
		dumpFallback("/users/*/friends/*/recommendations");
		dumpFallback("/users/*/friends");
		dumpFallback("/users/*/likes:bar/*/wines");
	}

	@Test
	public void testTypes() throws Exception {

		dumpType("/users/*/friends/*/recommendations:food", "food");
		dumpType("/users/*/messages", "entity");
		dumpType("/users/*/messages:cow", "cow");
		dumpType("/users/*/friends:user/*/messages:cow", "cow");
		dumpType("/users/*/friends", "entity");
		dumpType("/users/*/likes:bar/*/wines", "entity");
	}

	public void dumpFallback(String start) {
		List<String> patterns = ServiceInfo.getPatterns(start);
		logger.info(JsonUtils.mapToFormattedJsonString(patterns));
	}

	public void dumpType(String start, String expectedType) {
		String type = ServiceInfo.determineType(start);
		logger.info(start + " = " + type);
		assertEquals(expectedType, type);
	}

	public void testServiceInfo(String s, String... classes) {
		ServiceInfo info = ServiceInfo.getServiceInfo(s);
		logger.info(JsonUtils.mapToFormattedJsonString(info));
		int i = 0;
		for (String pattern : info.getPatterns()) {
			String className = ServiceInfo.getClassName(pattern);
			logger.info(pattern + " = " + className);
			if ((classes != null) && (i < classes.length)) {
				assertEquals(classes[i], className);
			}
			i++;
		}
	}
}
