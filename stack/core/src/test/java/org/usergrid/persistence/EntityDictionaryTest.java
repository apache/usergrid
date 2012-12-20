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
package org.usergrid.persistence;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.entities.Application;
import org.usergrid.utils.JsonUtils;

public class EntityDictionaryTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(EntityDictionaryTest.class);

	public EntityDictionaryTest() {
		super();
	}
	

	@Test
	public void testApplicationDictionaries() throws Exception {

		Application.OAuthProvider provider = new Application.OAuthProvider();
		provider.setClientId("123456789012.apps.googleusercontent.com");
		provider.setClientSecret("abcdefghijklmnopqrstuvwx");
		provider.setRedirectUris("https://www.example.com/oauth2callback");
		provider.setJavaScriptOrigins("https://www.example.com");
		provider.setAuthorizationEndpointUrl("https://accounts.google.com/o/oauth2/auth");
		provider.setAccessTokenEndpointUrl("https://accounts.google.com/o/oauth2/token");
		provider.setVersion("2.0");

		logger.info("EntityDictionaryTest.testApplicationDictionaries");

		UUID applicationId = createApplication("testOrganization","testApplicationDictionaries");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		em.addToDictionary(em.getApplicationRef(), "oauthproviders", "google",
				provider);

		Object o = em.getDictionaryElementValue(em.getApplicationRef(),
				"oauthproviders", "google");
		logger.info(JsonUtils.mapToFormattedJsonString(o));
	}

	@Test
	public void testUserDictionaries() throws Exception {

		logger.info("EntityDictionaryTest.testUserDictionaries");

		UUID applicationId = createApplication("testOrganization","testUserDictionaries");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = em.create("user", properties);
		assertNotNull(user);

		// test plaintext

		CredentialsInfo credentials = new CredentialsInfo();
		credentials.setSecret("test");
		credentials.setEncrypted(false);
		credentials.setRecoverable(true);
    credentials.setCryptoChain(new String[]{"plaintext"});

		em.addToDictionary(user, "credentials", "plaintext", credentials);

		Object o = em.getDictionaryElementValue(user, "credentials",
				"plaintext");
		logger.info(JsonUtils.mapToFormattedJsonString(o));

		assertEquals(CredentialsInfo.class, o.getClass());
		
		CredentialsInfo returned = (CredentialsInfo) o;
		
		assertEquals(credentials.getSecret(), returned.getSecret());
		assertEquals(credentials.getEncrypted(), returned.getEncrypted());
		assertEquals(credentials.getRecoverable(), returned.getRecoverable());
		assertArrayEquals(credentials.getCryptoChain(), returned.getCryptoChain());

		// test encrypted but recoverable

		credentials = new CredentialsInfo();
		credentials.setEncrypted(true);
		credentials.setRecoverable(false);
		credentials.setSecret("salt");
		credentials.setCryptoChain(new String[]{"sha-1"});

		em.addToDictionary(user, "credentials", "encrypted", credentials);

		o = em.getDictionaryElementValue(user, "credentials", "encrypted");
		logger.info(JsonUtils.mapToFormattedJsonString(o));

		assertEquals(CredentialsInfo.class, o.getClass());
		returned = (CredentialsInfo) o;

		
	  assertEquals(credentials.getSecret(), returned.getSecret());
    assertEquals(credentials.getEncrypted(), returned.getEncrypted());
    assertEquals(credentials.getRecoverable(), returned.getRecoverable());
    assertArrayEquals(credentials.getCryptoChain(), returned.getCryptoChain());



	}

}
