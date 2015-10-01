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


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.entities.Activity;

import static org.junit.Assert.assertNotNull;



public class ActivitiesServiceIT extends AbstractServiceIT {
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger( ActivitiesServiceIT.class );


    @Test
    public void testActivites() throws Exception {
        app.put( "username", "edanuff" );
        app.put( "email", "ed@anuff.com" );

        Entity userA = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
        assertNotNull( userA );

        app.put( "username", "djacobs" );
        app.put( "email", "djacobs@gmail.com" );

        Entity userB = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
        assertNotNull( userB );

        app.put( "username", "natpo" );
        app.put( "email", "npodrazik@gmail.com" );

        Entity userC = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
        assertNotNull( userC );

        app.testRequest( ServiceAction.POST, 1, "users", userB.getUuid(), "connections", "following", userA.getUuid() );

        app.testRequest( ServiceAction.POST, 1, "users", userC.getUuid(), "connections", "following", userA.getUuid() );

        app.testRequest( ServiceAction.GET, 0, "users", userA.getUuid(), "activities" );
        app.add( Activity.newActivity( Activity.VERB_POST, null, "I ate a sammich", null, userA, null, "tweet", null,
                null ) );

        Entity activity = app.testRequest( ServiceAction.POST, 1, "users", userA.getUuid(), "activities" ).getEntity();
        assertNotNull( activity );

        app.testRequest( ServiceAction.GET, 1, "users", userA.getUuid(), "activities" );

        app.testRequest( ServiceAction.GET, 1, null, "activities" );

        app.testRequest( ServiceAction.GET, 1, null, "users", userB.getUuid(), "feed" );

        app.testRequest( ServiceAction.GET, 1, null, "users", userC.getUuid(), "feed" );

        app.add( Activity.newActivity( Activity.VERB_POST, null, "I ate another sammich", null, userA, null, "tweet",
                null, null ) );

        activity = app.testRequest( ServiceAction.POST, 1, "users", userA.getUuid(), "activities" ).getEntity();
        assertNotNull( activity );

        app.add( Activity.newActivity( Activity.VERB_POST, null, "I ate a cookie", null, userA, null, "tweet", null,
                null ) );

        activity = app.testRequest( ServiceAction.POST, 1, "users", userA.getUuid(), "activities" ).getEntity();
        assertNotNull( activity );

        app.add( Activity.newActivity( Activity.VERB_CHECKIN, null, "I'm at the cookie shop", null, userA, null,
                Activity.OBJECT_TYPE_PLACE, "Cookie Shop", null ) );

        activity = app.testRequest( ServiceAction.POST, 1, "users", userA.getUuid(), "activities" ).getEntity();
        assertNotNull( activity );

        app.testRequest( ServiceAction.GET, 4, null, "users", userC.getUuid(), "feed" );

        app.testRequest( ServiceAction.GET, 2, null, "users", userC.getUuid(), "feed",
                Query.fromQL( "select * where content contains 'cookie'" ) );

        app.testRequest( ServiceAction.GET, 1, "users", userC.getUuid(), "feed",
                Query.fromQL( "select * where verb='post' and content contains 'cookie'" ) );

        app.put( "username", "finn" );
        app.put( "email", "finn@ooo.com" );

        Entity userD = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
        assertNotNull( userD );

        app.testRequest( ServiceAction.POST, 1, "users", userD.getUuid(), "connections", "following", userA.getUuid() );

        app.testRequest( ServiceAction.GET, 4, null, "users", userD.getUuid(), "feed" );
    }
}
