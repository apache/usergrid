package org.usergrid.rest;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class BasicTest extends AbstractRestTest {

	private static Logger log = LoggerFactory.getLogger(BasicTest.class);

	public BasicTest() throws Exception {
		super();
	}

	public void tryTest() {
		WebResource webResource = resource();
		String json = webResource.path("/test/hello")
				.accept(MediaType.APPLICATION_JSON).get(String.class);
		assertTrue(isNotBlank(json));

		log.info(json);
	}

	public static void logNode(JsonNode node) {
		log.info(mapToFormattedJsonString(node));
	}

	@Test
	public void testToken() {
		JsonNode node = null;

		try {
			node = resource().path("/management/token")
					.queryParam("grant_type", "password")
					.queryParam("username", "test@usergrid.com")
					.queryParam("password", "blahblah")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			assertEquals("Should receive a 400 Bad Request", 400, e
					.getResponse().getStatus());
		}

		node = resource().path("/management/token")
				.queryParam("grant_type", "password")
				.queryParam("username", "test@usergrid.com")
				.queryParam("password", "test")
				.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

		logNode(node);

		String access_token = node.get("access_token").getTextValue();
		assertTrue(isNotBlank(access_token));

		node = resource().path("/management/users/test@usergrid.com")
				.queryParam("access_token", access_token)
				.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

		logNode(node);

		assertEquals("Test User",
				node.get("data").get("organizations").get("test-organization")
						.get("users").get("test").get("name").getTextValue());

		Map<String, String> payload = hashMap("email", "ed@anuff.com")
				.map("username", "edanuff").map("name", "Ed Anuff")
				.map("password", "sesame");

		node = resource().path("/test-app/users")
				.queryParam("access_token", access_token)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.post(JsonNode.class, payload);

		logNode(node);

		assertEquals("edanuff", node.get("entities").get(0).get("username")
				.getTextValue());

		try {
			node = resource().path("/test-app/token")
					.queryParam("grant_type", "password")
					.queryParam("username", "ed@anuff.com")
					.queryParam("password", "blahblah")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			assertEquals("Should receive a 400 Bad Request", 400, e
					.getResponse().getStatus());
		}

		node = resource().path("/test-app/token")
				.queryParam("grant_type", "password")
				.queryParam("username", "ed@anuff.com")
				.queryParam("password", "sesame")
				.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

		logNode(node);

		String user_access_token = node.get("access_token").getTextValue();
		assertTrue(isNotBlank(user_access_token));

		node = resource().path("/test-app/users")
				.queryParam("access_token", user_access_token)
				.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

		logNode(node);

		assertEquals(1, node.get("entities").size());

		try {
			node = resource().path("/test-app/users")
					.queryParam("access_token", "blahblahblah")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			if (e.getResponse().getStatus() != 401) {
				throw e;
			}
		}

		try {
			node = resource().path("/test-app/users")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			assertEquals("Should receive a 401 Unauthorized", 401, e
					.getResponse().getStatus());
		}
	}

}
