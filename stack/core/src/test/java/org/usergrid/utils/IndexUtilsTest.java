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

import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;
import org.usergrid.persistence.entities.Activity;
import org.usergrid.persistence.entities.User;

public class IndexUtilsTest {

	private static final Logger logger = LoggerFactory.getLogger(IndexUtilsTest.class);

	@Test
	public void testKeywords() {

		String test = "Dragons, the policeman knew, were supposed to breathe fire and occasionally get themselves slaughtered.";
		List<String> keywords = IndexUtils.keywords(test);

		assertEquals(11, keywords.size());

		for (String keyword : keywords) {
			logger.info(keyword);
		}
	}

	@Test
	public void testKeyValue() throws Exception {

		User user = new User();
		user.setUsername("edanuff");
		user.setEmail("ed@anuff.com");

		Activity activity = Activity.newActivity(Activity.VERB_POST, null,
				"I ate another sammich", null, user, null, "tweet", null, null);

		List<Entry<String, Object>> l = IndexUtils.getKeyValueList(activity,
				false);
		for (Entry<String, Object> e : l) {
			logger.info(e.getKey() + " = " + e.getValue());
		}

		assertEquals(7, l.size());

	}
}
