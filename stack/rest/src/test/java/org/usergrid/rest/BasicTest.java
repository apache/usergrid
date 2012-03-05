package org.usergrid.rest;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

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

	@Test
	public void testToken() {
		JsonNode node = null;

		// test get token for admin user with bad password

		boolean err_thrown = false;
		try {
			node = resource().path("/management/token")
					.queryParam("grant_type", "password")
					.queryParam("username", "test@usergrid.com")
					.queryParam("password", "blahblah")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			assertEquals("Should receive a 400 Bad Request", 400, e
					.getResponse().getStatus());
			err_thrown = true;
		}
		assertTrue("Error should have been thrown", err_thrown);

		// test get token for admin user with correct default password

		String mgmtToken = mgmtToken();
		// test get admin user with token

		node = resource().path("/management/users/test@usergrid.com")
				.queryParam("access_token", mgmtToken)
				.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

		logNode(node);

		assertEquals("Test User",
				node.get("data").get("organizations").get("test-organization")
						.get("users").get("test").get("name").getTextValue());


		// test login user with incorrect password

		err_thrown = false;
		try {
			node = resource().path("/test-app/token")
					.queryParam("grant_type", "password")
					.queryParam("username", "ed@anuff.com")
					.queryParam("password", "blahblah")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			assertEquals("Should receive a 400 Bad Request", 400, e
					.getResponse().getStatus());
			err_thrown = true;
		}
		assertTrue("Error should have been thrown", err_thrown);

		// test login user with incorrect pin

		err_thrown = false;
		try {
			node = resource().path("/test-app/token")
					.queryParam("grant_type", "pin")
					.queryParam("username", "ed@anuff.com")
					.queryParam("pin", "4321")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			assertEquals("Should receive a 400 Bad Request", 400, e
					.getResponse().getStatus());
			err_thrown = true;
		}
		assertTrue("Error should have been thrown", err_thrown);

		// test login user with correct password

		node = resource().path("/test-app/token")
				.queryParam("grant_type", "password")
				.queryParam("username", "ed@anuff.com")
				.queryParam("password", "sesame")
				.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

		logNode(node);

		String user_access_token = node.get("access_token").getTextValue();
		assertTrue(isNotBlank(user_access_token));

		// test get app user collection with insufficient permissions

		err_thrown = false;
		try {
			node = resource().path("/test-app/users")
					.queryParam("access_token", user_access_token)
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			if (e.getResponse().getStatus() != 401) {
				throw e;
			}
			err_thrown = true;
		}
		// assertTrue("Error should have been thrown", err_thrown);

		// test get app user with sufficient permissions

		node = resource().path("/test-app/users/edanuff")
				.queryParam("access_token", user_access_token)
				.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		logNode(node);

		assertEquals(1, node.get("entities").size());

		// test get app user collection with bad token

		err_thrown = false;
		try {
			node = resource().path("/test-app/users")
					.queryParam("access_token", "blahblahblah")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			if (e.getResponse().getStatus() != 401) {
				throw e;
			}
			err_thrown = true;
		}
		assertTrue("Error should have been thrown", err_thrown);

		// test get app user collection with no token

		err_thrown = false;
		try {
			node = resource().path("/test-app/users")
					.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
		} catch (UniformInterfaceException e) {
			assertEquals("Should receive a 401 Unauthorized", 401, e
					.getResponse().getStatus());
			err_thrown = true;
		}
		assertTrue("Error should have been thrown", err_thrown);

		// test login app user with pin

		node = resource().path("/test-app/token")
				.queryParam("grant_type", "pin")
				.queryParam("username", "ed@anuff.com")
				.queryParam("pin", "1234").accept(MediaType.APPLICATION_JSON)
				.get(JsonNode.class);

		logNode(node);

		user_access_token = node.get("access_token").getTextValue();
		assertTrue(isNotBlank(user_access_token));

		// test set app user pin

		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("pin", "5678");
		node = resource().path("/test-app/users/ed@anuff.com/setpin")
				.queryParam("access_token", user_access_token)
				.type("application/x-www-form-urlencoded")
				.post(JsonNode.class, formData);

		node = resource().path("/test-app/token")
				.queryParam("grant_type", "pin")
				.queryParam("username", "ed@anuff.com")
				.queryParam("pin", "5678").accept(MediaType.APPLICATION_JSON)
				.get(JsonNode.class);

		logNode(node);

		user_access_token = node.get("access_token").getTextValue();
		assertTrue(isNotBlank(user_access_token));

		// test user test extension resource

		node = resource().path("/test-app/users/ed@anuff.com/test").get(
				JsonNode.class);
		logNode(node);

		// test create user with guest permissions (no token)

		Map<String,String> payload = hashMap("email", "ed.anuff@gmail.com")
				.map("username", "ed.anuff").map("name", "Ed Anuff")
				.map("password", "sesame").map("pin", "1234");

		node = resource().path("/test-app/users")
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.post(JsonNode.class, payload);

		logNode(node);

		assertEquals("ed.anuff", node.get("entities").get(0).get("username")
				.getTextValue());

		// test create device with guest permissions (no token)

		payload = hashMap("foo", "bar");

		node = resource().path("/test-app/devices/" + UUID.randomUUID())
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.put(JsonNode.class, payload);

		logNode(node);

		// test create entity with guest permissions (no token), should fail

		payload = hashMap("foo", "bar");

		err_thrown = false;
		try {
			node = resource().path("/test-app/items")
					.accept(MediaType.APPLICATION_JSON)
					.type(MediaType.APPLICATION_JSON_TYPE)
					.post(JsonNode.class, payload);
		} catch (UniformInterfaceException e) {
			assertEquals("Should receive a 401 Unauthorized", 401, e
					.getResponse().getStatus());
			err_thrown = true;
		}
		assertTrue("Error should have been thrown", err_thrown);

	}

}
