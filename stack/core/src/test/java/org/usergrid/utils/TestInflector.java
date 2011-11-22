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

import static org.junit.Assert.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

public class TestInflector {
	private static final Logger logger = LoggerFactory.getLogger(TestInflector.class);

	@Test
	public void testInflector() {

		testSingularize("users", "user");
		testSingularize("groups", "group");
		testSingularize("entities", "entity");
		testSingularize("messages", "message");
		testSingularize("activities", "activity");
		testSingularize("binaries", "binary");
		testSingularize("data", "data");

		testSingularize("user", "user");
		testSingularize("group", "group");
		testSingularize("entity", "entity");
		testSingularize("message", "message");
		testSingularize("activity", "activity");
		testSingularize("binary", "binary");

		testPluralize("user", "users");
		testPluralize("group", "groups");
		testPluralize("entity", "entities");
		testPluralize("message", "messages");
		testPluralize("activity", "activities");
		testPluralize("binary", "binaries");
		testPluralize("data", "data");

		testPluralize("users", "users");
		testPluralize("groups", "groups");
		testPluralize("entities", "entities");
		testPluralize("messages", "messages");
		testPluralize("activities", "activities");
		testPluralize("binaries", "binaries");

		testPluralize("com.usergrid.resources.user",
				"com.usergrid.resources.users");
		testSingularize("com.usergrid.resources.users",
				"com.usergrid.resources.user");
	}

	public void testSingularize(String p, String expected) {
		String s = Inflector.getInstance().singularize(p);
		logger.info("Inflector says singular form of " + p + " is " + s);
		assertEquals("singular form of " + p + " not expected value", expected,
				s);
	}

	public void testPluralize(String s, String expected) {
		String p = Inflector.getInstance().pluralize(s);
		logger.info("Inflector says plural form of " + s + " is " + p);
		assertEquals("plural form of " + s + " not expected value", expected, p);
	}

}
