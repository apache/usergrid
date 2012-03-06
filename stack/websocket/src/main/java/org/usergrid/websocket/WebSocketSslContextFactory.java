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

import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class WebSocketSslContextFactory {

	private static final String PROTOCOL = "TLS";
	private static final SSLContext SERVER_CONTEXT;

	static {
		String algorithm = Security
				.getProperty("ssl.KeyManagerFactory.algorithm");
		if (algorithm == null) {
			algorithm = "SunX509";
		}

		SSLContext serverContext = null;
		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(WebSocketKeyStore.asInputStream(),
					WebSocketKeyStore.getKeyStorePassword());

			// Set up key manager factory to use our key store
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
			kmf.init(ks, WebSocketKeyStore.getCertificatePassword());

			// Initialize the SSLContext to work with our key managers.
			serverContext = SSLContext.getInstance(PROTOCOL);
			serverContext.init(kmf.getKeyManagers(), null, null);
		} catch (Exception e) {
			throw new Error("Failed to initialize the server-side SSLContext",
					e);
		}

		SERVER_CONTEXT = serverContext;
	}

	public static SSLContext getServerContext() {
		return SERVER_CONTEXT;
	}

}
