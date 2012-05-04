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
package org.usergrid.rest.applications.users;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.java.client.Client.Query;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.entities.User;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.utils.UUIDUtils;

/**
 * @author tnine
 */
public class GroupResourceTest extends AbstractRestTest {
    private static Logger log = LoggerFactory
            .getLogger(GroupResourceTest.class);

    private static final String GROUP = "testGroup";

    private static final String USER = "edanuff";

    private static boolean groupCreated = false;

    public GroupResourceTest() throws Exception {

    }

    @Before
    public void setupGroup() {
        if (groupCreated) {
            return;
        }

        client.createGroup(GROUP);

        groupCreated = true;
    }

    @Test
    public void postGroupActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        UUID id = UUIDUtils.newTimeUUID();

        String groupPath = "groupPath" + id;
        String groupTitle = "groupTitle " + id;
        String groupName = "groupName" + id;

        ApiResponse response = client.createGroup(groupPath, groupTitle,
                groupName);

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        UUID newId = response.getEntities().get(0).getUuid();

        Query results = client.queryGroups(String
                .format("name='%s'", groupName));

        response = results.getResponse();

        UUID entityId = response.getEntities().get(0).getUuid();

        assertEquals(newId, entityId);

        results = client.queryGroups(String.format("title='%s'", groupTitle));

        response = results.getResponse();

        entityId = response.getEntities().get(0).getUuid();

        assertEquals(newId, entityId);
        
        results = client.queryGroups(String.format("title contains '%s'", id));

        response = results.getResponse();

        entityId = response.getEntities().get(0).getUuid();

        assertEquals(newId, entityId);

        results = client.queryGroups(String.format("path='%s'", groupPath));

        response = results.getResponse();

        entityId = response.getEntities().get(0).getUuid();

        assertEquals(newId, entityId);

    }

}
