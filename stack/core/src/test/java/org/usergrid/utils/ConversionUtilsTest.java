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
