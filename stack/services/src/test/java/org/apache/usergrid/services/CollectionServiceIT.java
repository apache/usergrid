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
package org.apache.usergrid.services;


import org.junit.Assert;
import org.junit.Test;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.apache.usergrid.persistence.exceptions.UnexpectedEntityTypeException;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.services.exceptions.ServiceResourceNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;



public class CollectionServiceIT extends AbstractServiceIT {
    public static final String CST_TEST_GROUP = "cst-test-group";


    @Test
    public void testUsersCollectionWithGroupIdName() throws Exception {
        app.put( "path", "cst-test-group/cst-test-group" );
        app.put( "title", "Collection Test group" );

        Entity group = app.testRequest( ServiceAction.POST, 1, "groups" ).getEntity();
        assertNotNull( group );

        app.testRequest( ServiceAction.GET, 1, "groups", CST_TEST_GROUP, CST_TEST_GROUP );

        app.testRequest( ServiceAction.GET, 1, "groups" );

        app.put( "username", "edanuff" );
        app.put( "email", "ed@anuff.com" );

        Entity user = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
        assertNotNull( user );

        try {
            // try GET on users with group id
            app.testRequest( ServiceAction.GET, 0, "users", group.getUuid() );
            Assert.fail();
        }
        catch ( UnexpectedEntityTypeException uee ) {
            // ok
        }
        catch ( EntityNotFoundException enfe ) {
            // ok
        }

        try {
            // try GET on users with group name
            app.testRequest( ServiceAction.GET, 0, "users", CST_TEST_GROUP );
            Assert.fail();
        }
        catch ( ServiceResourceNotFoundException srnfe ) {
            // ok
        }

        app.put( "group-size", "10" );

        try {
            // try POST on users with group id
            app.testRequest( ServiceAction.POST, 0, "users", group.getUuid() );
            Assert.fail();
        }
        catch ( UnexpectedEntityTypeException uee ) {
            // ok
        }
        catch ( ServiceResourceNotFoundException srnfe ) {
            // ok
        }

        try {
            // try POST on users with group name
            app.testRequest( ServiceAction.POST, 0, "users", CST_TEST_GROUP );
            Assert.fail();
        }
        catch ( ServiceResourceNotFoundException srnfe ) {
            // ok
        }

        try {
            // try PUT on users with group id
            app.testRequest( ServiceAction.PUT, 0, "users", group.getUuid() );
            Assert.fail();
        }
        catch ( UnexpectedEntityTypeException uee ) {
            // ok
        }
        catch ( RequiredPropertyNotFoundException rpnfe ) {
            // ok
        }

        try {
            // try PUT on users with group name
            app.testRequest( ServiceAction.PUT, 0, "users", CST_TEST_GROUP );
            Assert.fail();
        }
        catch ( RequiredPropertyNotFoundException srnfe ) {
            // ok
        }

        try {
            // try DELETE on users with group id
            app.testRequest( ServiceAction.DELETE, 0, "users", group.getUuid() );
            Assert.fail();
        }
        catch ( UnexpectedEntityTypeException uee ) {
            // ok
        }
        catch ( ServiceResourceNotFoundException srnfe ) {
            // ok
        }

        try {
            // try DELETE on users with group name
            app.testRequest( ServiceAction.DELETE, 0, "users", CST_TEST_GROUP );
            Assert.fail();
        }
        catch ( ServiceResourceNotFoundException srnfe ) {
            // ok
        }
    }


    @Test
    public void testGenericEntityCollectionWithIdName() throws Exception {
        app.put( "name", "Tom" );

        Entity cat = app.testRequest( ServiceAction.POST, 1, "cats" ).getEntity();
        assertNotNull( cat );

        app.testRequest( ServiceAction.GET, 1, "cats", "Tom" );

        app.clear();
        app.put( "name", "Danny" );

        Entity dog = app.testRequest( ServiceAction.POST, 1, "dogs" ).getEntity();
        assertNotNull( dog );

        try {
            // try GET on cats with dog id
            app.testRequest( ServiceAction.GET, 0, "cats", dog.getUuid() );
            Assert.fail();
        }
        catch ( Exception uee ) {
            // ok
        }

        try {
            // try GET on cats with dog name
            app.testRequest( ServiceAction.GET, 0, "cats", "Danny" );
            Assert.fail();
        }
        catch ( EntityNotFoundException enfe ) {
            // ok
        }

        app.put( "color", "black" );

        try {
            // try POST on cats with dogs id
            app.testRequest( ServiceAction.POST, 0, "cats", dog.getUuid() );
            Assert.fail();
        }
        catch ( Exception uee ) {
            // ok
        }

        try {
            // try POST on cats with dogs name
            app.testRequest( ServiceAction.POST, 0, "cats", "Danny" );
            Assert.fail();
        }
        catch ( ServiceResourceNotFoundException srnfe ) {
            // ok
        }

        try {
            // try PUT on users with dogs id
            app.testRequest( ServiceAction.PUT, 0, "cats", dog.getUuid() );
            Assert.fail();
        }
        catch ( UnexpectedEntityTypeException uee ) {
            // ok
        }

        try {
            // try DELETE on cats with dogs id
            app.testRequest( ServiceAction.DELETE, 0, "cats", dog.getUuid() );
            Assert.fail();
        }
        catch ( Exception uee ) {
            // ok
        }

        app.refreshIndex();
        try {
            // try DELETE on cats with dogs name
            app.testRequest( ServiceAction.DELETE, 0, "cats", "Danny" );
            Assert.fail();
        }
        catch ( ServiceResourceNotFoundException srnfe ) {
            // ok
        }

        // TODO: This test cannot be supported with Core Persistence
        // try PUT on cats with a new UUID
        final String catsUuid = "99999990-600c-11e2-b414-14109fd49581";
        ServiceResults results = app.testRequest( ServiceAction.PUT, 1, "cats", catsUuid );
        Entity entity = results.getEntity();
        //Assert.assertEquals( entity.getUuid().toString(), catsUuid );

        // try PUT on cats with a name w/o name in properties
        results = app.testRequest( ServiceAction.PUT, 1, "cats", "Danny" );
        entity = results.getEntity();
        Assert.assertEquals( entity.getName(), "danny" );

        // try PUT on cats with a name in properties w/ difference capitalization
        app.put( "name", "Danny2" );
        results = app.testRequest( ServiceAction.PUT, 1, "cats", "Danny2" );
        entity = results.getEntity();
        Assert.assertEquals( entity.getName(), "Danny2" );

        // try PUT on cats with a completely different name in properties
        app.put( "name", "Jimmy" );
        results = app.testRequest( ServiceAction.PUT, 1, "cats", "Danny3" );
        entity = results.getEntity();
        Assert.assertEquals( entity.getName(), "danny3" );
    }


    @Test
    public void testEmptyCollection() throws Exception {
        // Generic collection first call
        Entity cat = app.testRequest( ServiceAction.POST, 0, "cats" ).getEntity();
        assertNull( cat );

        CollectionInfo info = Schema.getDefaultSchema().getCollection( TYPE_APPLICATION, "cats" );

        assertNotNull( info );

        assertEquals( "cats", info.getName() );

        // call second time
        cat = app.testRequest( ServiceAction.POST, 0, "cats" ).getEntity();
        assertNull( cat );

        // users core collections - username required
        try {
            app.testRequest( ServiceAction.POST, 0, "users" );
            Assert.fail();
        }
        catch ( RequiredPropertyNotFoundException rpnfe ) {
            //ok
        }

        // groups core collections - path required
        try {

            app.testRequest( ServiceAction.POST, 0, "groups" );
            Assert.fail();
        }
        catch ( IllegalArgumentException iae ) {
            //ok
        }

        // roles core collections - role name required
        try {
            app.testRequest( ServiceAction.POST, 0, "roles" );
            Assert.fail();
        }
        catch ( RequiredPropertyNotFoundException ex ) {
            //ok
        }

        // events core collections - timestamp required
        try {
            app.testRequest( ServiceAction.POST, 0, "events" );
        }
        catch ( RequiredPropertyNotFoundException rpnfe ) {
            //ok
        }
    }
}
