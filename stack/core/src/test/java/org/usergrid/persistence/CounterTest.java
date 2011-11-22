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
package org.usergrid.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;
import org.usergrid.persistence.entities.Event;
import org.usergrid.utils.JsonUtils;

public class CounterTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory.getLogger(CounterTest.class);

	public CounterTest() {
		super();
	}

	@Test
	public void testCounters() throws Exception {
		logger.info("CounterTest.testCounters");

		UUID applicationId = createApplication("testCounters");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		long ts = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
		UUID user1 = UUID.randomUUID();
		UUID user2 = UUID.randomUUID();
		// UUID groupId = UUID.randomUUID();

		Event event = null;

		for (int i = 0; i < 1000; i++) {
			event = new Event();
			event.setTimestamp(ts + (i * 60 * 1000));
			event.addCounter("visits", 1);
			event.setUser(user1);
			em.create(event);

			event = new Event();
			event.setTimestamp(ts + (i * 60 * 1000));
			event.addCounter("visits", 1);
			event.setUser(user2);
			em.create(event);
		}

		Results r = em.getAggregateCounters(null, null, null, "visits",
				CounterResolution.SIX_HOUR, ts, System.currentTimeMillis(),
				false);
		logger.info(JsonUtils.mapToJsonString(r.getCounters()));

		r = em.getAggregateCounters(user1, null, null, "visits",
				CounterResolution.SIX_HOUR, ts, System.currentTimeMillis(),
				false);
		logger.info(JsonUtils.mapToJsonString(r.getCounters()));

		r = em.getAggregateCounters(user1, null, null, "visits",
				CounterResolution.SIX_HOUR, ts, System.currentTimeMillis(),
				true);
		logger.info(JsonUtils.mapToJsonString(r.getCounters()));

		r = em.getAggregateCounters(user1, null, null, "visits",
				CounterResolution.ALL, ts, System.currentTimeMillis(), false);
		logger.info(JsonUtils.mapToJsonString(r.getCounters()));

		for (int i = 0; i < 10; i++) {
			event = new Event();
			event.setTimestamp(ts + (i * 60 * 60 * 1000));
			event.addCounter("clicks", 1);
			em.create(event);
		}

		r = em.getAggregateCounters(null, null, null, "clicks",
				CounterResolution.HALF_HOUR, ts, System.currentTimeMillis(),
				true);
		logger.info(JsonUtils.mapToJsonString(r.getCounters()));

		Query query = new Query();
		query.addCounterFilter("clicks:*:*:*");
		query.addCounterFilter("visits:*:*:*");
		query.setStartTime(ts);
		query.setFinishTime(System.currentTimeMillis());
		query.setResolution(CounterResolution.SIX_HOUR);
		query.setPad(true);
		r = em.getAggregateCounters(query);
		logger.info(JsonUtils.mapToJsonString(r.getCounters()));

		logger.info(JsonUtils.mapToJsonString(em.getCounterNames()));

		Map<String, Long> counts = em.getApplicationCounters();
		logger.info(JsonUtils.mapToJsonString(counts));

		assertEquals(new Long(10), counts.get("clicks"));
		assertEquals(new Long(2000), counts.get("visits"));
		assertEquals(new Long(2010),
				counts.get("application.collection.events"));
	}

}
