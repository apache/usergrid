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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

public class JsonUtilsTest {

	private static final Logger logger = Logger.getLogger(JsonUtilsTest.class);

	@SuppressWarnings("unchecked")
	@Test
	public void testUnroll() {
		Map<String, Object> json = new LinkedHashMap<String, Object>();

		json.put("name", "edanuff");
		json.put("cat", "fishbone");
		json.put("city", "San Francisco");
		json.put("car", "bmw");
		json.put("stuff", Arrays.asList(1, 2, 3, 4, 5));

		json.put("phones", Arrays.asList(MapUtils.map("a", "b"),
				MapUtils.map("a", "c"),
				MapUtils.map("b", MapUtils.map("d", "e", "d", "f"))));

		dumpJson("obj", json);

		dumpJson("propname", Arrays.asList(1, 2, 3, 4, 5));
		dumpJson("propname", 125);

		System.out.println(JsonUtils.mapToJsonString(json));

		Object result = JsonUtils.select(json, "phones");
		System.out.println(JsonUtils.mapToJsonString(result));

		result = JsonUtils.select(json, "phones.a");
		System.out.println(JsonUtils.mapToJsonString(result));

	}

	public void dumpJson(String path, Object json) {
		List<Map.Entry<String, Object>> list = IndexUtils.getKeyValueList(path,
				json, true);

		for (Map.Entry<String, Object> e : list) {
			logger.info(e.getKey() + " = " + e.getValue());
		}
	}

	@Test
	public void testNormalize() {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("foo", "bar");

		Object o = JsonUtils.normalizeJsonTree(node);
		assertEquals(Map.class, o.getClass());

	}

}
