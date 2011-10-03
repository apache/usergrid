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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class ConversionUtilsTest {

	@Test
	public void testBytes() {
		byte[] bytes = ConversionUtils.bytes(true);
		assertNotNull("bytes() on boolean true returned null", bytes);
		assertTrue("bytes() on boolean true returned wrong size byte array:"
				+ bytes.length, bytes.length == 1);
		assertTrue("bytes() on boolean true returned wrong value:" + bytes[0],
				bytes[0] == 1);

		bytes = ConversionUtils.bytes(false);
		assertNotNull("bytes() on boolean false returned null", bytes);
		assertTrue("bytes() on boolean false returned wrong size byte array:"
				+ bytes.length, bytes.length == 1);
		assertTrue("bytes() on boolean false returned wrong value:" + bytes[0],
				bytes[0] == 0);

		bytes = ConversionUtils.bytes(new UUID(0, 0));
		assertNotNull("bytes() on uuid(0, 0) returned null", bytes);
		assertTrue("bytes() on uuid(0, 0) returned wrong size byte array:"
				+ bytes.length, bytes.length == 16);
		for (byte b : bytes) {
			assertTrue("bytes() on uuid(0, 0) returned wrong value:" + b,
					b == 0);
		}

	}
}
