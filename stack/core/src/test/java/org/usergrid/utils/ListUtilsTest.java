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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

public class ListUtilsTest {

	private static final Logger logger = LoggerFactory.getLogger(ListUtilsTest.class);

	@Test
	public void testListUtils() {

		List<?> l = Arrays.asList(1, 2, 3);

		l = Arrays.asList(l, 4, 5, 6);
		l = Arrays.asList(l, 7, 8, 9);

		l = ListUtils.flatten(l);
		assertEquals(9, l.size());
		logger.info(JsonUtils.mapToFormattedJsonString(l));

		l = Arrays.asList(Arrays.asList(1, 2, 3), 4, 5, 6,
				Arrays.asList(7, 8, 9));

		l = ListUtils.flatten(l);
		assertEquals(9, l.size());
		logger.info(JsonUtils.mapToFormattedJsonString(l));

	}
}
