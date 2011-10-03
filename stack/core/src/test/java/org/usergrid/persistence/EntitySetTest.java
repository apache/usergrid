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

import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.usergrid.persistence.entities.Application;
import org.usergrid.utils.JsonUtils;

public class EntitySetTest extends AbstractPersistenceTest {

	private static final Logger logger = Logger.getLogger(EntitySetTest.class);

	public EntitySetTest() {
		super();
	}

	@Test
	public void testApplicationSets() throws Exception {

		Application.OAuthProvider provider = new Application.OAuthProvider();
		provider.setClientId("123456789012.apps.googleusercontent.com");
		provider.setClientSecret("abcdefghijklmnopqrstuvwx");
		provider.setRedirectUris("https://www.example.com/oauth2callback");
		provider.setJavaScriptOrigins("https://www.example.com");
		provider.setAuthorizationEndpointUrl("https://accounts.google.com/o/oauth2/auth");
		provider.setAccessTokenEndpointUrl("https://accounts.google.com/o/oauth2/token");
		provider.setVersion("2.0");

		logger.info("EntitySetTest.testApplicationSets");

		UUID applicationId = createApplication("testEntityManagerTest");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		em.addToDictionary(em.getApplicationRef(), "oauthproviders", "google",
				provider);

		Object o = em.getDictionaryElementValue(em.getApplicationRef(),
				"oauthproviders", "google");
		logger.info(JsonUtils.mapToFormattedJsonString(o));
	}
}
