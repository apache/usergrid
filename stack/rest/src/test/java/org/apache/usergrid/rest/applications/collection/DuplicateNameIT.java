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




import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.junit.Test;

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

        fail("This test is incorrectly written and should not use direct serialization to test duplicate names");

//        CustomCollection things = context.application().customCollection( "things" );
//
//        Map<String, String> entity = MapUtils.hashMap( "name", "enzo" );
//
//        try {
//            things.create( entity );
//        } catch (IOException ex) {
//            logger.error("Cannot create entity", ex);
//        }
//
//        refreshIndex( context.getAppUuid() );
//
//        Injector injector = Guice.createInjector( new TestGuiceModule( null ) );
//        SerializationFig sfig = injector.getInstance( SerializationFig.class );
//
//        // wait for any temporary unique value records to timeout
//        try { Thread.sleep( sfig.getTimeout() * 1100 ); } catch (InterruptedException ignored) {}
//
//        try {
//            things.create( entity );
//            fail("Should not have created duplicate entity");
//
//        } catch (Exception ex) {
//            // good
//        }
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
        } catch (UniformInterfaceException uie) {
            //Check for an exception
            assertEquals(400, uie.getResponse().getStatus());
        }
    }
}
