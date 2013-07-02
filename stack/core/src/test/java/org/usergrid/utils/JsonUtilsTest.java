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
package org.usergrid.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

public class JsonUtilsTest {

	private static final Logger logger = LoggerFactory.getLogger(JsonUtilsTest.class);

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
		assertEquals(java.util.LinkedHashMap.class, o.getClass());

	}

}
