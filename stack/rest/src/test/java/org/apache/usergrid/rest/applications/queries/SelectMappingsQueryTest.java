/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest.applications.queries;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class SelectMappingsQueryTest extends QueryTestBase {
    private static final Logger logger = LoggerFactory.getLogger(OrderByTest.class);


    /**
     * Select field mappings may include nested entity fields.
     */
    @Test
    public void testNestedSelectFieldNames() throws Exception {

        String collectionName = "basketballs";
        generateTestEntities(20, collectionName);

        QueryParameters params = new QueryParameters()
            .setQuery("select actor.displayName,sometestprop where sometestprop = 'testprop'");
        Collection coll = this.app().collection(collectionName).get(params);
        assertEquals( 10, coll.getNumOfEntities() );

        Iterator<Entity> iter = coll.iterator();
        while ( iter.hasNext() ) {

            Entity entity = iter.next();
            assertEquals( 5, entity.getDynamicProperties().size() );

            assertNotNull( entity.getDynamicProperties().get("uuid") );
            assertNotNull( entity.getDynamicProperties().get("type") );
            assertNotNull( entity.getDynamicProperties().get("metadata") );
            assertNotNull( entity.getDynamicProperties().get("sometestprop") );

            Map<String, Object> actor = (Map<String, Object>)entity.getDynamicProperties().get("actor");
            assertNotNull( actor );
            assertNotNull( actor.get("displayName") );

        }
    }


    /**
     * When entity posted with two duplicate names with different cases, last one wins.
     */
    @Test
    public void testMixedCaseDupField() throws Exception {

        String collectionName = "baseballs";

        String value = RandomStringUtils.randomAlphabetic( 20 );
        String otherValue = RandomStringUtils.randomAlphabetic( 20 );

        // create entity with testProp=value
        Entity entity = new Entity()
            .withProp( "testProp", value )
            .withProp( "TESTPROP", otherValue);
        app().collection( collectionName ).post( entity );
        waitForQueueDrainAndRefreshIndex();

        // testProp and TESTPROP should now have otherValue

        QueryParameters params = new QueryParameters()
            .setQuery( "select * where testProp='" + otherValue + "'" );
        Collection coll = this.app().collection(collectionName).get( params );
        assertEquals( 1, coll.getNumOfEntities() );

        params = new QueryParameters()
            .setQuery( "select * where TESTPROP='" + otherValue + "'" );
        coll = app().collection(collectionName).get( params );
        assertEquals( 1, coll.getNumOfEntities() );
    }

    @Test
    public void testStringWithSingleQuote() throws Exception {

        String collectionName = "footballs";

        String value = "test'value";
        String escapedValue = "test\\'value";

        // create entity with testProp=value
        Entity entity = new Entity()
            .withProp( "testprop", value );
        app().collection( collectionName ).post( entity );
        waitForQueueDrainAndRefreshIndex();

        // testProp and TESTPROP should now have otherValue

        QueryParameters params = new QueryParameters()
            .setQuery( "select * where testprop='" + escapedValue + "'" );
        Collection things = this.app().collection(collectionName).get( params );
        assertEquals( 1, things.getNumOfEntities() );
    }

    @Test
    public void testStringWithPlus() throws Exception {

        String collectionName = "volleyballs";
        String value = "ed+test@usergrid.com";

        // create entity with value containing a plus symbol
        Entity entity = new Entity()
            .withProp( "testprop", value );
        app().collection( collectionName ).post( entity );
        waitForQueueDrainAndRefreshIndex();

        // now query this without encoding the plus symbol
        QueryParameters params = new QueryParameters()
            .setQuery( "select * where testprop='" + value + "'" );
        Collection coll = this.app().collection(collectionName).get( params );
        assertEquals( 1, coll.getNumOfEntities() );

        // again query with the plus symbol url encoded
        String escapedValue = "ed%2Btest@usergrid.com";
        params = new QueryParameters()
            .setQuery( "select * where testprop='" + escapedValue + "'" );
        coll = this.app().collection(collectionName).get( params );
        assertEquals( 1, coll.getNumOfEntities() );

    }


    /**
     * Field named testProp can be over-written by field named TESTPROP.
     */
    @Test
    public void testFieldOverride1() throws Exception {

        String collectionName = "pickleballs";

        // create entity with testProp=value
        String value = RandomStringUtils.randomAlphabetic( 20 );
        Entity entity = new Entity().withProp( "testProp", value );
        app().collection( collectionName ).post( entity );
        waitForQueueDrainAndRefreshIndex();

        // override with TESTPROP=newValue
        String newValue = RandomStringUtils.randomAlphabetic( 20 );
        entity = new Entity().withProp( "TESTPROP", newValue );
        app().collection( collectionName ).post( entity );
        waitForQueueDrainAndRefreshIndex();

        // testProp and TESTPROP should new be queryable by new value

        QueryParameters params = new QueryParameters()
            .setQuery( "select * where testProp='" + newValue + "'" );
        Collection coll = this.app().collection(collectionName).get( params );
        assertEquals( 1, coll.getNumOfEntities() );

        params = new QueryParameters()
            .setQuery( "select * where TESTPROP='" + newValue + "'" );
        coll = app().collection(collectionName).get( params );
        assertEquals( 1, coll.getNumOfEntities() );
    }

    /**
     * Field named testProp can be over-written by field named TESTPROP.
     */
    @Test
    public void testFieldOverride2() throws Exception {

        String collectionName = "tennisballs";

        // create entity with TESTPROP=value
        String value = RandomStringUtils.randomAlphabetic( 20 );
        Entity entity = new Entity().withProp( "TESTPROP", value );
        app().collection( collectionName ).post( entity );
        waitForQueueDrainAndRefreshIndex();

        // override with testProp=newValue
        String newValue = RandomStringUtils.randomAlphabetic( 20 );
        entity = new Entity().withProp( "testProp", newValue );
        app().collection( collectionName ).post( entity );
        waitForQueueDrainAndRefreshIndex();

        // testProp and TESTPROP should new be queryable by new value

        QueryParameters params = new QueryParameters()
            .setQuery( "select * where testProp='" + newValue + "'" );
        Collection coll = this.app().collection(collectionName).get( params );
        assertEquals( 1, coll.getNumOfEntities() );

        params = new QueryParameters()
            .setQuery( "select * where TESTPROP='" + newValue + "'" );
        coll = app().collection(collectionName).get( params );
        assertEquals( 1, coll.getNumOfEntities() );
    }

}
