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
package org.usergrid.websocket;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class WebSocketKeyStore {
	private static final short[] DATA = new short[] {};

	private WebSocketKeyStore() {
		throw new AssertionError();
	}

	public static InputStream asInputStream() {
		byte[] data = new byte[DATA.length];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) DATA[i];
		}
		return new ByteArrayInputStream(data);
	}

	public static char[] getCertificatePassword() {
		return "jwebsocket".toCharArray();
	}

	public static char[] getKeyStorePassword() {
		return "jwebsocket".toCharArray();
	}
}
