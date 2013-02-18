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
package org.usergrid.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.persistence.Schema.TYPE_APPLICATION;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.usergrid.persistence.exceptions.UnexpectedEntityTypeException;
import org.usergrid.persistence.schema.CollectionInfo;
import org.usergrid.services.exceptions.ServiceResourceNotFoundException;

public class CollectionServiceTest extends AbstractServiceTest {

    private static final Logger logger = LoggerFactory
			.getLogger(CollectionServiceTest.class);
    private static final String TEST_ORGANIZATION = "testOrganizationCST";
    private static final String TEST_APPLICATION = "testCollectionCST";
    public static final String CST_TEST_GROUP = "cst-test-group";

    @Test
	public void testUsersCollectionWithGroupIdName() throws Exception {

		UUID applicationId = createApplication(TEST_ORGANIZATION,
                TEST_APPLICATION);

		ServiceManager sm = smf.getServiceManager(applicationId);


		Map<String, Object> properties = new LinkedHashMap<String, Object>();

        properties.put("path", "cst-test-group/cst-test-group");
		properties.put("title", "Collection Test group");

		Entity group = testRequest(sm, ServiceAction.POST, 1, properties,
				"groups").getEntity();
		assertNotNull(group);

		testRequest(sm, ServiceAction.GET, 1, null, "groups", CST_TEST_GROUP,
                CST_TEST_GROUP);

		testRequest(sm, ServiceAction.GET, 1, null, "groups");

		properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = testRequest(sm, ServiceAction.POST, 1, properties,
				"users").getEntity();
		assertNotNull(user);

		try {
			// try GET on users with group id
			testRequest(sm, ServiceAction.GET, 0, null, "users",
					group.getUuid());
			Assert.fail();
		} catch (UnexpectedEntityTypeException uee) {
			// ok
		}

		try {
			// try GET on users with group name
			testRequest(sm, ServiceAction.GET, 0, null, "users", CST_TEST_GROUP);
			Assert.fail();
		} catch (ServiceResourceNotFoundException srnfe) {
			// ok
		}

		properties = new LinkedHashMap<String, Object>();
		properties.put("group-size", "10");

		try {
			// try POST on users with group id
			testRequest(sm, ServiceAction.POST, 0, properties, "users",
					group.getUuid());
			Assert.fail();
		} catch (UnexpectedEntityTypeException uee) {
			// ok
		}

		try {
			// try POST on users with group name
			testRequest(sm, ServiceAction.POST, 0, properties, "users",
                    CST_TEST_GROUP);
			Assert.fail();
		} catch (ServiceResourceNotFoundException srnfe) {
			// ok
		}

		try {
			// try PUT on users with group id
			testRequest(sm, ServiceAction.PUT, 0, properties, "users",
					group.getUuid());
			Assert.fail();
		} catch (UnexpectedEntityTypeException uee) {
			// ok
		}

		try {
			// try PUT on users with group name
			testRequest(sm, ServiceAction.PUT, 0, properties, "users",
                    CST_TEST_GROUP);
			Assert.fail();
		} catch (RequiredPropertyNotFoundException srnfe) {
			// ok
		}

		try {
			// try DELETE on users with group id
			testRequest(sm, ServiceAction.DELETE, 0, null, "users",
					group.getUuid());
			Assert.fail();
		} catch (UnexpectedEntityTypeException uee) {
			// ok
		}

		try {
			// try DELETE on users with group name
			testRequest(sm, ServiceAction.DELETE, 0, null, "users",
                    CST_TEST_GROUP);
			Assert.fail();
		} catch (ServiceResourceNotFoundException srnfe) {
			// ok
		}

	}

	@Test
	public void testGenericEntityCollectionWithIdName() throws Exception {

		UUID applicationId = createApplication(TEST_ORGANIZATION,
				"testCatsDogs");

		ServiceManager sm = smf.getServiceManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "Tom");

		Entity cat = testRequest(sm, ServiceAction.POST, 1, properties, "cats")
				.getEntity();
		assertNotNull(cat);

		testRequest(sm, ServiceAction.GET, 1, null, "cats", "Tom");

		properties = new LinkedHashMap<String, Object>();
		properties.put("name", "Danny");

		Entity dog = testRequest(sm, ServiceAction.POST, 1, properties, "dogs")
				.getEntity();
		assertNotNull(dog);

		try {
			// try GET on cats with dog id
			testRequest(sm, ServiceAction.GET, 0, null, "cats", dog.getUuid());
			Assert.fail();
		} catch (UnexpectedEntityTypeException uee) {
			// ok
		}

		try {
			// try GET on cats with dog name
			testRequest(sm, ServiceAction.GET, 0, null, "cats", "Danny");
			Assert.fail();
		} catch (ServiceResourceNotFoundException srnfe) {
			// ok
		}

		properties = new LinkedHashMap<String, Object>();
		properties.put("color", "black");

		try {
			// try POST on cats with dogs id
			testRequest(sm, ServiceAction.POST, 0, properties, "cats",
					dog.getUuid());
			Assert.fail();
		} catch (UnexpectedEntityTypeException uee) {
			// ok
		}

		try {
			// try POST on cats with dogs name
			testRequest(sm, ServiceAction.POST, 0, properties, "cats", "Danny");
			Assert.fail();
		} catch (ServiceResourceNotFoundException srnfe) {
			// ok
		}

		try {
			// try PUT on users with dogs id
			testRequest(sm, ServiceAction.PUT, 0, properties, "cats",
					dog.getUuid());
			Assert.fail();
		} catch (UnexpectedEntityTypeException uee) {
			// ok
		}

		try {
			// try DELETE on cats with dogs id
			testRequest(sm, ServiceAction.DELETE, 0, null, "cats",
					dog.getUuid());
			Assert.fail();
		} catch (UnexpectedEntityTypeException uee) {
			// ok
		}

		try {
			// try DELETE on cats with dogs name
			testRequest(sm, ServiceAction.DELETE, 0, null, "cats", "Danny");
			Assert.fail();
		} catch (ServiceResourceNotFoundException srnfe) {
			// ok
		}

    // try PUT on cats with a new UUID
    ServiceResults results = testRequest(sm, ServiceAction.PUT, 1, properties, "cats", "99999990-600c-11e2-b414-14109fd49581");
    Entity entity = results.getEntity();
    Assert.assertEquals(entity.getUuid().toString(), "99999990-600c-11e2-b414-14109fd49581");

    // try PUT on cats with a name w/o name in properties
    properties.remove("name");
    results = testRequest(sm, ServiceAction.PUT, 1, properties, "cats", "Danny");
    entity = results.getEntity();
    Assert.assertEquals(entity.getName(), "danny");

    // try PUT on cats with a name in properties w/ difference capitalization
    properties.put("name", "Danny2");
    results = testRequest(sm, ServiceAction.PUT, 1, properties, "cats", "Danny2");
    entity = results.getEntity();
    Assert.assertEquals(entity.getName(), "Danny2");

    // try PUT on cats with a completely different name in properties
    properties.put("name", "Jimmy");
    results = testRequest(sm, ServiceAction.PUT, 1, properties, "cats", "Danny3");
    entity = results.getEntity();
    Assert.assertEquals(entity.getName(), "danny3");
  }

	@Test
	public void testEmptyCollection() throws Exception {
		UUID applicationId = createApplication("testOrganization",
				"testEmptyCollections");

		ServiceManager sm = smf.getServiceManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();

		// Generic collection first call
		Entity cat = testRequest(sm, ServiceAction.POST, 0, properties, "cats")
				.getEntity();
		assertNull(cat);

		CollectionInfo info = Schema.getDefaultSchema().getCollection(
				TYPE_APPLICATION, "cats");

		assertNotNull(info);

		assertEquals("cats", info.getName());

		// call second time
		cat = testRequest(sm, ServiceAction.POST, 0, properties, "cats")
				.getEntity();
		assertNull(cat);

		// users core collections - username required
		try {

			  testRequest(sm, ServiceAction.POST, 0, properties, "users");
			Assert.fail();
		} catch (RequiredPropertyNotFoundException rpnfe) {
			//ok
		}

		// groups core collections - path required
		try {

			  testRequest(sm, ServiceAction.POST, 0, properties, "groups");
			Assert.fail();
		} catch (IllegalArgumentException iae) {
			//ok
		}

		// roles core collections - role name required
		try {

			  testRequest(sm, ServiceAction.POST, 0, properties, "roles");
			Assert.fail();
		} catch (IllegalArgumentException iae) {
			//ok
		}

		// events core collections - timestamp required
		try {
			testRequest(sm, ServiceAction.POST, 0, properties, "events");
		} catch(RequiredPropertyNotFoundException rpnfe) {
			//ok
		}
	}

}
