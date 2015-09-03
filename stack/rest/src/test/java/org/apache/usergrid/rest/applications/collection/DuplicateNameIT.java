/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.rest.applications.collection;

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.junit.Test;

import javax.ws.rs.ClientErrorException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class DuplicateNameIT extends AbstractRestIT {

    /**
     * Test to ensure that an error is returned when
     * attempting to POST multiple entities to the
     * same collection with the same name
     */
    @Test
    public void duplicateNamePrevention() {

        String collectionName = "things";
        Entity entity = new Entity();
        entity.put("name", "enzo");
        //Create an entity named "enzo" in the "things" collection
        entity = this.app().collection(collectionName).post(entity);
        refreshIndex();
        try {
            // Try to create a second entity in "things" with the name "enzo".
            this.app().collection(collectionName).post(entity);
            // fail if the POST did not return an exception
            fail("Should not have created duplicate entity");
        } catch (ClientErrorException uie) {
            //Check for an exception
            assertEquals(400, uie.getResponse().getStatus());
        }
    }
}
