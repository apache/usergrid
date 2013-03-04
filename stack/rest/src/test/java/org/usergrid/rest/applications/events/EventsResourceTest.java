package org.usergrid.rest.applications.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.AbstractRestTest;

public class EventsResourceTest extends AbstractRestTest {

	private static Logger log = LoggerFactory
			.getLogger(EventsResourceTest.class);

	@Test
	public void testEventPostandGet() {

		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("timestamp", 0);
		payload.put("category", "advertising");
		payload.put("counters", new LinkedHashMap<String, Object>() {
			{
				put("ad_clicks", 5);
			}
		});

		JsonNode node = resource().path("/test-organization/test-app/events")
				.queryParam("access_token", access_token)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.post(JsonNode.class, payload);

		assertNotNull(node.get("entities"));
		String advertising = node.get("entities").get(0).get("uuid").asText();

		payload = new LinkedHashMap<String, Object>();
		payload.put("timestamp", 0);
		payload.put("category", "sales");
		payload.put("counters", new LinkedHashMap<String, Object>() {
			{
				put("ad_sales", 20);
			}
		});

		node = resource().path("/test-organization/test-app/events")
				.queryParam("access_token", access_token)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.post(JsonNode.class, payload);

		assertNotNull(node.get("entities"));
		String sales = node.get("entities").get(0).get("uuid").asText();

		payload = new LinkedHashMap<String, Object>();
		payload.put("timestamp", 0);
		payload.put("category", "marketing");
		payload.put("counters", new LinkedHashMap<String, Object>() {
			{
				put("ad_clicks", 10);
			}
		});

		node = resource().path("/test-organization/test-app/events")
				.queryParam("access_token", access_token)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.post(JsonNode.class, payload);

		assertNotNull(node.get("entities"));
		String marketing = node.get("entities").get(0).get("uuid").asText();

		// subsequent GETs advertising
		for (int i = 0; i < 3; i++) {

			node = resource().path("/test-organization/test-app/events")
					.queryParam("access_token", access_token)
					.accept(MediaType.APPLICATION_JSON)
					.type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

			logNode(node);
			assertEquals("Expected Advertising", advertising, node.get("messages").get(0).get("uuid").asText());
		}

		// check sales event in queue
		node = resource().path("/test-organization/test-app/events/"+sales)
				.queryParam("access_token", access_token)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

		logNode(node);
		assertEquals("Expected Sales", sales,node.get("entities").get(0).get("uuid").asText());

		// check marketing event in queue
		node = resource().path("/test-organization/test-app/events/"+marketing)
				.queryParam("access_token", access_token)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

		logNode(node);
		assertEquals("Expected Marketing", marketing, node.get("entities").get(0).get("uuid").asText());


	}

}
