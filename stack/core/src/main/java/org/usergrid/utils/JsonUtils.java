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

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.io.JsonStringEncoder;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.smile.SmileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author edanuff
 * 
 */
public class JsonUtils {

	private static final Logger logger = LoggerFactory
			.getLogger(JsonUtils.class);

	static ObjectMapper mapper = new ObjectMapper();

	static SmileFactory smile = new SmileFactory();

	/**
	 * @param obj
	 * @return
	 */
	public static String mapToJsonString(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		return "{}";

	}

	public static String mapToFormattedJsonString(Object obj) {
		try {
			ObjectMapper m = new ObjectMapper();
			m.getSerializationConfig().set(Feature.INDENT_OUTPUT, true);
			return m.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		return "{}";

	}

	public static String schemaToFormattedJsonString(JsonSchema schema) {
		return mapToFormattedJsonString(schema.getSchemaNode());
	}

	public static JsonSchema getJsonSchema(Class<?> cls) {
		JsonSchema jsonSchema = null;
		try {
			jsonSchema = mapper.generateJsonSchema(cls);
		} catch (JsonMappingException e) {
		}
		return jsonSchema;
	}

	public static Object parse(String json) {
		try {
			return mapper.readValue(json, Object.class);
		} catch (JsonParseException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		return null;
	}

	public static JsonNode getJsonSchemaNode(Class<?> cls) {
		JsonNode schemaRootNode = null;
		JsonSchema jsonSchema = getJsonSchema(cls);
		if (jsonSchema != null) {
			schemaRootNode = jsonSchema.getSchemaNode();
		}
		return schemaRootNode;
	}

	public static String quoteString(String s) {
		JsonStringEncoder encoder = new JsonStringEncoder();
		return new String(encoder.quoteAsUTF8(s));
	}

	public static ByteBuffer toByteBuffer(Object obj) {
		if (obj == null) {
			return null;
		}
		ObjectMapper mapper = new ObjectMapper(smile);
		byte[] bytes = null;
		try {
			bytes = mapper.writeValueAsBytes(obj);
		} catch (Exception e) {
			logger.error("Error getting SMILE bytes", e);
		}
		if (bytes != null) {
			return ByteBuffer.wrap(bytes);
		}
		return null;
	}

	public static Object fromByteBuffer(ByteBuffer byteBuffer) {
		return fromByteBuffer(byteBuffer, Object.class);
	}

	public static Object fromByteBuffer(ByteBuffer byteBuffer, Class<?> clazz) {
		if ((byteBuffer == null) || !byteBuffer.hasRemaining()) {
			return null;
		}
		if (clazz == null) {
			clazz = Object.class;
		}
		ObjectMapper mapper = new ObjectMapper(smile);
		Object obj = null;
		try {
			obj = mapper.readValue(byteBuffer.array(), byteBuffer.arrayOffset()
					+ byteBuffer.position(), byteBuffer.remaining(), clazz);
		} catch (Exception e) {
			logger.error("Error parsing SMILE bytes", e);
		}
		return obj;
	}

	public static JsonNode nodeFromByteBuffer(ByteBuffer byteBuffer) {
		if ((byteBuffer == null) || !byteBuffer.hasRemaining()) {
			return null;
		}
		ObjectMapper mapper = new ObjectMapper(smile);
		JsonNode obj = null;
		try {
			obj = mapper.readValue(byteBuffer.array(), byteBuffer.arrayOffset()
					+ byteBuffer.position(), byteBuffer.remaining(),
					JsonNode.class);
		} catch (Exception e) {
			logger.error("Error parsing SMILE bytes", e);
		}
		return obj;
	}

	public static JsonNode toJsonNode(Object obj) {
		if (obj == null) {
			return null;
		}
		JsonNode node = mapper.convertValue(obj, JsonNode.class);
		return node;
	}

	public static Map<String, Object> toJsonMap(Object obj) {
		if (obj == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = mapper.convertValue(obj, Map.class);
		return map;
	}

	private static UUID tryConvertToUUID(Object o) {
		if (o instanceof String) {
			String s = (String) o;
			if (s.length() == 36) {
				try {
					UUID uuid = UUID.fromString(s);
					return uuid;
				} catch (IllegalArgumentException e) {
				}
			}
		}
		return null;
	}

	public static Object normalizeJsonTree(Object obj) {
		if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> m = (Map<Object, Object>) obj;
			for (Object k : m.keySet()) {
				Object o = m.get(k);
				UUID uuid = tryConvertToUUID(o);
				if (uuid != null) {
					m.put(k, uuid);
				} else if (o instanceof Integer) {
					m.put(k, ((Integer) o).longValue());
				} else if (o instanceof BigInteger) {
					m.put(k, ((BigInteger) o).longValue());
				}
			}
		} else if (obj instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> l = (List<Object>) obj;
			for (int i = 0; i < l.size(); i++) {
				Object o = l.get(i);
				UUID uuid = tryConvertToUUID(o);
				if (uuid != null) {
					l.set(i, uuid);
				} else if ((o instanceof Map) || (o instanceof List)) {
					normalizeJsonTree(o);
				} else if (o instanceof Integer) {
					l.set(i, ((Integer) o).longValue());
				} else if (o instanceof BigInteger) {
					l.set(i, ((BigInteger) o).longValue());
				}
			}
		} else if (obj instanceof String) {
			UUID uuid = tryConvertToUUID(obj);
			if (uuid != null) {
				return uuid;
			}
		} else if (obj instanceof Integer) {
			return ((Integer) obj).longValue();
		} else if (obj instanceof BigInteger) {
			return ((BigInteger) obj).longValue();
		} else if (obj instanceof JsonNode) {
			return mapper.convertValue(obj, Object.class);
		}
		return obj;
	}

	public static Object select(Object obj, String path) {
		return select(obj, path, false);
	}

	public static Object select(Object obj, String path, boolean buildResultTree) {

		if (obj == null) {
			return null;
		}

		if (org.apache.commons.lang.StringUtils.isBlank(path)) {
			return obj;
		}

		String segment = stringOrSubstringBeforeFirst(path, '.');
		String remaining = substringAfter(path, ".");

		if (obj instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) obj;
			Object child = map.get(segment);
			Object result = select(child, remaining, buildResultTree);
			if (result != null) {
				if (buildResultTree) {
					Map<Object, Object> results = new LinkedHashMap<Object, Object>();
					results.put(segment, result);
					return results;
				} else {
					return result;
				}
			}
			return null;
		}
		if (obj instanceof List) {
			List<Object> results = new ArrayList<Object>();
			List<?> list = (List<?>) obj;
			for (Object i : list) {
				Object result = select(i, path, buildResultTree);
				if (result != null) {
					results.add(result);
				}
			}
			if (!results.isEmpty()) {
				return results;
			}
			return null;
		}

		return obj;
	}

	public static Object loadFromResourceFile(String file) {
		Object json = null;
		try {
			URL resource = JsonUtils.class.getResource(file);
			json = mapper.readValue(resource, Object.class);
		} catch (Exception e) {
			logger.error("Error loading JSON", e);
		}
		return json;
	}

	public static Object loadFromFilesystem(String filename) {
		Object json = null;
		try {
			File file = new File(filename);
			json = mapper.readValue(file, Object.class);
		} catch (Exception e) {
			logger.error("Error loading JSON", e);
		}
		return json;
	}

	public static Object loadFromUrl(String urlStr) {
		Object json = null;
		try {
			URL url = new URL(urlStr);
			json = mapper.readValue(url, Object.class);
		} catch (Exception e) {
			logger.error("Error loading JSON", e);
		}
		return json;
	}

	public static Object loadFromUrl(URL url) {
		Object json = null;
		try {
			json = mapper.readValue(url, Object.class);
		} catch (Exception e) {
			logger.error("Error loading JSON", e);
		}
		return json;
	}

	public static boolean isSmile(ByteBuffer buffer) {
		buffer = buffer.duplicate();
		if (buffer.get() != 0x3A) {
			return false;
		}
		if (buffer.get() != 0x29) {
			return false;
		}
		if (buffer.get() != 0x0A) {
			return false;
		}
		return true;
	}
}
