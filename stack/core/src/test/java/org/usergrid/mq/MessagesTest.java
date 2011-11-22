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
package org.usergrid.mq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;
import org.usergrid.persistence.AbstractPersistenceTest;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.utils.JsonUtils;

public class MessagesTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory.getLogger(MessagesTest.class);

	public MessagesTest() {
		super();
	}

	@Test
	public void testMessages() throws Exception {
		logger.info("MessagesTest.testMessages");

		UUID applicationId = createApplication("testMessages");
		assertNotNull(applicationId);

		EntityManager em = getEntityManagerFactory().getEntityManager(
				applicationId);
		assertNotNull(em);

		logger.info("Creating message #1");

		Message message = new Message();
		message.setStringProperty("foo", "bar");
		logger.info(JsonUtils.mapToFormattedJsonString(message));

		logger.info("Posting message #1 to queue /foo/bar");

		QueueManager qm = geQueueManagerFactory()
				.getQueueManager(applicationId);
		qm.postToQueue("/foo/bar", message);

		logger.info("Getting message #1");

		message = qm.getMessage(message.getUuid());
		logger.info(JsonUtils.mapToFormattedJsonString(message));

		logger.info("Getting message from /foo/bar, should be message #1");

		QueueResults messages = qm.getFromQueue("/foo/bar", null);
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(1, messages.size());

		logger.info("Getting message from /foo/bar, should empty");

		messages = qm.getFromQueue("/foo/bar", null);
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(0, messages.size());

		message = new Message();
		message.setStringProperty("name", "alpha");
		qm.postToQueue("/foo/bar", message);

		message = new Message();
		message.setStringProperty("name", "bravo");
		qm.postToQueue("/foo/bar", message);

		messages = qm.getFromQueue("/foo/bar", null);
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(1, messages.size());

		messages = qm.getFromQueue("/foo/bar", null);
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(1, messages.size());

		messages = qm.getFromQueue("/foo/bar", null);
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(0, messages.size());

		messages = qm.getFromQueue("/foo/bar",
				new QueueQuery().withPosition(QueuePosition.END)
						.withPreviousCount(3));
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(3, messages.size());

		Map<String, Long> counters = qm.getQueueCounters("/");
		logger.info(JsonUtils.mapToFormattedJsonString(counters));
		assertEquals(1, counters.size());
		assertNotNull(counters.get("/foo/bar/"));
		assertEquals(new Long(3), counters.get("/foo/bar/"));
	}

	@Test
	public void testSubscriberSearch() throws Exception {

		UUID applicationId = createApplication("testSubscriberSearch");
		assertNotNull(applicationId);

		EntityManager em = getEntityManagerFactory().getEntityManager(
				applicationId);
		assertNotNull(em);

		QueueManager qm = geQueueManagerFactory()
				.getQueueManager(applicationId);

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("foo", "alpha");
		Queue q = qm.updateQueue("/foo/1/", properties);
		logger.info(JsonUtils.mapToFormattedJsonString(q));

		q = qm.getQueue("/foo/1/");
		logger.info(JsonUtils.mapToFormattedJsonString(q));
		assertEquals("alpha", q.getStringProperty("foo"));

		properties = new HashMap<String, Object>();
		properties.put("foo", "bravo");
		q = qm.updateQueue("/foo/2/", properties);
		logger.info(JsonUtils.mapToFormattedJsonString(q));

		properties = new HashMap<String, Object>();
		properties.put("foo", "charlie");
		q = qm.updateQueue("/foo/3/", properties);
		logger.info(JsonUtils.mapToFormattedJsonString(q));

		qm.subscribeToQueue("/pubtest/", "/foo/1/");
		qm.subscribeToQueue("/pubtest/", "/foo/2/");
		qm.subscribeToQueue("/pubtest/", "/foo/3/");

		QueueSet results = qm.searchSubscribers("/pubtest/",
				Query.findForProperty("foo", "bravo"));
		logger.info(JsonUtils.mapToFormattedJsonString(results));
		assertEquals(1, results.size());

		properties = new HashMap<String, Object>();
		properties.put("foo", "delta");
		q = qm.updateQueue("/foo/2/", properties);
		logger.info(JsonUtils.mapToFormattedJsonString(q));

		results = qm.searchSubscribers("/pubtest/",
				Query.findForProperty("foo", "bravo"));
		logger.info(JsonUtils.mapToFormattedJsonString(results));
		assertEquals(0, results.size());

		results = qm.searchSubscribers("/pubtest/",
				Query.findForProperty("foo", "delta"));
		logger.info(JsonUtils.mapToFormattedJsonString(results));
		assertEquals(1, results.size());

		qm.unsubscribeFromQueue("/pubtest/", "/foo/2/");

		results = qm.searchSubscribers("/pubtest/",
				Query.findForProperty("foo", "delta"));
		logger.info(JsonUtils.mapToFormattedJsonString(results));
		assertEquals(0, results.size());
	}

	@Test
	public void testConsumer() throws Exception {

		UUID applicationId = createApplication("testConsumer");
		assertNotNull(applicationId);

		EntityManager em = getEntityManagerFactory().getEntityManager(
				applicationId);
		assertNotNull(em);

		logger.info("Creating messages");

		QueueManager qm = geQueueManagerFactory()
				.getQueueManager(applicationId);
		Message message = null;

		for (int i = 0; i < 10; i++) {
			message = new Message();
			message.setStringProperty("foo", "bar" + i);

			logger.info("Posting message #" + i + " to queue /foo/bar: "
					+ message.getUuid());

			qm.postToQueue("/foo/bar", message);
		}

		for (int i = 0; i < 11; i++) {
			QueueResults messages = qm.getFromQueue("/foo/bar",
					new QueueQuery().withConsumer("consumer1"));
			logger.info(JsonUtils.mapToFormattedJsonString(messages));
			if (i < 10) {
				assertEquals(1, messages.size());
				assertEquals("bar" + i, messages.getMessages().get(0)
						.getStringProperty("foo"));
			} else {
				assertEquals(0, messages.size());
			}
		}

		for (int i = 0; i < 11; i++) {
			QueueResults messages = qm.getFromQueue("/foo/bar",
					new QueueQuery().withConsumer("consumer2"));
			logger.info(JsonUtils.mapToFormattedJsonString(messages));
			if (i < 10) {
				assertEquals(1, messages.size());
				assertEquals("bar" + i, messages.getMessages().get(0)
						.getStringProperty("foo"));
			} else {
				assertEquals(0, messages.size());
			}
		}
	}

}
