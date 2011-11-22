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
import static org.junit.Assert.assertFalse;
import static org.usergrid.utils.UUIDUtils.getTimestampInMillis;
import static org.usergrid.utils.UUIDUtils.newTimeUUID;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UUIDUtilsTest {

	private static final Logger logger = LoggerFactory
			.getLogger(UUIDUtilsTest.class);

	@Test
	public void testUUIDUtils() {
		UUID uuid = UUIDUtils.newTimeUUID();
		logger.info("" + uuid);
		logger.info("" + uuid.timestamp());
		logger.info("" + UUIDUtils.getTimestampInMillis(uuid));

		logger.info(""
				+ UUIDUtils.getTimestampInMillis(UUIDUtils.newTimeUUID()));
		logger.info("" + System.currentTimeMillis());

		logger.info(""
				+ UUIDUtils.getTimestampInMicros(UUIDUtils.newTimeUUID()));
		logger.info("" + (System.currentTimeMillis() * 1000));

		logger.info("" + UUIDUtils.MIN_TIME_UUID);
		logger.info("" + UUIDUtils.MIN_TIME_UUID.variant());
		logger.info("" + UUIDUtils.MIN_TIME_UUID.version());
		logger.info("" + UUIDUtils.MIN_TIME_UUID.clockSequence());
		logger.info("" + UUIDUtils.MIN_TIME_UUID.timestamp());

		logger.info("" + UUIDUtils.MAX_TIME_UUID);
		logger.info("" + UUIDUtils.MAX_TIME_UUID.variant());
		logger.info("" + UUIDUtils.MAX_TIME_UUID.version());
		logger.info("" + UUIDUtils.MAX_TIME_UUID.clockSequence());
		logger.info("" + UUIDUtils.MAX_TIME_UUID.timestamp());
	}

	@Test
	public void testAppProvidedTimestamp() {
		logger.info("UUIDUtilsTest.testAppProvidedTimestamp");
		long ts = System.currentTimeMillis();
		System.out.println(ts);

		Set<UUID> uuids = new HashSet<UUID>();

		int count = 1000000;

		logger.info("Generating " + count + " UUIDs...");
		for (int i = 0; i < count; i++) {
			UUID uuid = newTimeUUID(ts);

			assertFalse("UUID already generated", uuids.contains(uuid));
			uuids.add(uuid);

			assertEquals("Incorrect UUID timestamp value", ts,
					getTimestampInMillis(uuid));
		}
		logger.info("UUIDs checked");

	}

}
