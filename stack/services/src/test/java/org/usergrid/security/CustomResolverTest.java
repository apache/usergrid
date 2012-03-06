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
package org.usergrid.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.security.shiro.CustomPermission;

public class CustomResolverTest {

	public static final Logger logger = LoggerFactory
			.getLogger(CustomResolverTest.class);

	@Test
	public void testResolver() throws Exception {

		testImplies(true,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo");

		testImplies(
				true,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar");

		testImplies(
				true,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz");

		testImplies(
				false,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/");

		testImplies(
				false,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz");

		testImplies(
				true,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/**",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz");

		testImplies(
				true,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*/boz/*",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz");

		testImplies(
				true,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*/boz/*",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz/biz");

		testImplies(
				false,
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*/boz/*",
				"applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz/biz/box");
	}

	public void testImplies(boolean expected, String s1, String s2) {
		CustomPermission p1 = new CustomPermission(s1);
		CustomPermission p2 = new CustomPermission(s2);
		if (expected) {
			assertTrue(p1.implies(p2));
		} else {
			assertFalse(p1.implies(p2));
		}

	}

}
