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

import static org.usergrid.utils.ConversionUtils.bytes;

import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

public class CodecUtils {

	public static Base64 base64 = new Base64(true);

	public static String base64(byte[] bytes) {
		return base64.encodeToString(bytes);
	}

	public static String base64(UUID uuid) {
		return base64.encodeToString(bytes(uuid));
	}

	public static UUID decodeBase64UUID(String base64String) {
		byte[] bytes = base64.decode(base64String);
		return UUID.nameUUIDFromBytes(bytes);
	}

	public static String base64(String str) {
		return Base64.encodeBase64URLSafeString(bytes(str));
	}
}
