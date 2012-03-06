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

import static org.usergrid.utils.ClassUtils.cast;
import static org.usergrid.utils.ClassUtils.isBasicType;
import static org.usergrid.utils.JsonUtils.quoteString;
import static org.usergrid.utils.JsonUtils.toJsonNode;

import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public class IndexUtils {

	private static final Logger logger = LoggerFactory.getLogger(IndexUtils.class);

	static Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);

	private static void buildKeyValueSet(Object node,
			Map<String, List<Object>> keyValues, String path,
			boolean fulltextIndex, Object... history) {

		if (node == null) {
			return;
		}

		if (node instanceof Collection) {
			Object[] new_history = Arrays.copyOf(history, history.length + 1);
			new_history[history.length] = node;
			@SuppressWarnings("unchecked")
			Collection<Object> c = (Collection<Object>) node;
			for (Object o : c) {
				buildKeyValueSet(o, keyValues, path, fulltextIndex, new_history);
			}
		} else if (node instanceof Map) {
			Object[] new_history = Arrays.copyOf(history, history.length + 1);
			new_history[history.length] = node;
			@SuppressWarnings("unchecked")
			Map<Object, Object> m = (Map<Object, Object>) node;
			for (Entry<Object, Object> e : m.entrySet()) {
				String new_path = path;
				String key = e.getKey().toString();
				key = quoteString(key);
				if (key.indexOf('\\') == -1) {
					new_path = (path != null ? path + "." : "") + key;
				} else {
					new_path = (path != null ? path : "") + "[\"" + key + "\"]";
				}
				buildKeyValueSet(e.getValue(), keyValues, new_path,
						fulltextIndex, new_history);
			}
		} else if (node instanceof ArrayNode) {
			Object[] new_history = Arrays.copyOf(history, history.length + 1);
			new_history[history.length] = node;
			ArrayNode c = (ArrayNode) node;
			for (JsonNode o : c) {
				buildKeyValueSet(o, keyValues, path, fulltextIndex, new_history);
			}
		} else if (node instanceof ObjectNode) {
			Object[] new_history = Arrays.copyOf(history, history.length + 1);
			new_history[history.length] = node;
			ObjectNode c = (ObjectNode) node;
			Iterator<Entry<String, JsonNode>> i = c.getFields();
			while (i.hasNext()) {
				Entry<String, JsonNode> e = i.next();
				String new_path = path;
				String key = e.getKey();
				key = quoteString(key);
				if (key.indexOf('\\') == -1) {
					new_path = (path != null ? path + "." : "") + key;
				} else {
					new_path = (path != null ? path : "") + "[\"" + key + "\"]";
				}
				buildKeyValueSet(e.getValue(), keyValues, new_path,
						fulltextIndex, new_history);
			}
		} else if (!isBasicType(node.getClass())
				&& (!(node instanceof JsonNode))) {
			buildKeyValueSet(toJsonNode(node), keyValues, path, fulltextIndex,
					history);
		} else {

			if (node instanceof JsonNode) {
				if (((JsonNode) node).isTextual()) {
					node = ((JsonNode) node).getTextValue();
					UUID uuid = UUIDUtils.tryGetUUID((String) node);
					if (uuid != null) {
						node = uuid;
					}
				} else if (((JsonNode) node).isNumber()) {
					node = ((JsonNode) node).getNumberValue();
				} else {
					return;
				}
			}

			if (path == null) {
				path = "";
			}
			List<Object> l = keyValues.get(path);
			if (l == null) {
				l = new ArrayList<Object>();
				keyValues.put(path, l);
			}

			l.add(node);

			if ((node instanceof String) && fulltextIndex) {
				String keywords_path = (path.length() > 0) ? path + ".keywords"
						: "keywords";
				List<Object> keywords = cast(keywords((String) node));

				if (keywords.size() > 0) {
					keyValues.put(keywords_path, keywords);
				}
			}
		}

	}

	public static Map<String, List<Object>> getKeyValues(String path,
			Object obj, boolean fulltextIndex) {
		Map<String, List<Object>> keys = new LinkedHashMap<String, List<Object>>();
		buildKeyValueSet(obj, keys, path, fulltextIndex);
		return keys;
	}

	public static List<Map.Entry<String, Object>> getKeyValueList(String path,
			Object obj, boolean fulltextIndex) {
		Map<String, List<Object>> map = getKeyValues(path, obj, fulltextIndex);
		List<Map.Entry<String, Object>> list = new ArrayList<Map.Entry<String, Object>>();
		for (Entry<String, List<Object>> entry : map.entrySet()) {
			for (Object value : entry.getValue()) {
				list.add(new AbstractMap.SimpleEntry<String, Object>(entry
						.getKey(), value));
			}
		}
		;
		return list;
	}

	public static Map<String, List<Object>> getKeyValues(Object obj,
			boolean fulltextIndex) {
		return getKeyValues(null, obj, fulltextIndex);
	}

	public static List<Entry<String, Object>> getKeyValueList(Object obj,
			boolean fulltextIndex) {
		return getKeyValueList(null, obj, fulltextIndex);
	}

	public static List<String> keywords(String source) {
		TokenStream ts = analyzer.tokenStream("keywords", new StringReader(
				source));
		List<String> keywords = new ArrayList<String>();
		try {
			while (ts.incrementToken()) {
				keywords.add(ts.getAttribute(TermAttribute.class).term());
			}
		} catch (IOException e) {
			logger.error("Error getting keywords ", e);
		}
		return keywords;
	}

	public static String keywordText(String source) {
		TokenStream ts = analyzer.tokenStream("keywords", new StringReader(
				source));
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		try {
			while (ts.incrementToken()) {
				if (!first) {
					builder.append(' ');
				}
				first = false;
				builder.append(ts.getAttribute(TermAttribute.class).term());
			}
		} catch (IOException e) {
			logger.error("Error getting keywords ", e);
		}
		return builder.toString();
	}
}
