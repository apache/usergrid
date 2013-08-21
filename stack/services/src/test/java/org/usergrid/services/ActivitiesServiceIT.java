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


import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.entities.Activity;

import java.util.LinkedHashMap;
import java.util.Map;


@Concurrent()
public class ActivitiesServiceIT extends AbstractServiceIT
{
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger( ActivitiesServiceIT.class );

	@Test
	public void testActivites() throws Exception
    {
        app.add("username", "edanuff");
        app.add("email", "ed@anuff.com");


        Map<String,Object> properties = new LinkedHashMap<String, Object>();

		Entity userA = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
		assertNotNull( userA );

		app.add("username", "djacobs");
        app.add("email", "djacobs@gmail.com");

		Entity userB = app.testRequest(ServiceAction.POST, 1, "users").getEntity();
		assertNotNull(userB);

		app.add("username", "natpo");
        app.add("email", "npodrazik@gmail.com");

		Entity userC = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
		assertNotNull(userC);

		app.testRequest(ServiceAction.POST, 1, "users", userB.getUuid(), "connections", "following", userA.getUuid() );

		app.testRequest(ServiceAction.POST, 1, "users", userC.getUuid(), "connections", "following", userA.getUuid() );

        Activity activity = Activity.newActivity( Activity.VERB_POST, null, "I ate a sammich",
                null, userA, null, "tweet", null, null );
        properties.putAll( activity.getProperties() );
		app.putAll( properties );

		Entity activityEntity = app.testRequest( ServiceAction.POST, 1, "users",
                userA.getUuid(), "activities" ).getEntity();
		assertNotNull( activityEntity );

		app.testRequest( ServiceAction.GET, 1, "users", userA.getUuid(), "activities" );

		app.testRequest( ServiceAction.GET, 1, "activities" );

		app.testRequest( ServiceAction.GET, 1, "users", userB.getUuid(), "feed" );

		app.testRequest( ServiceAction.GET, 1, "users", userC.getUuid(), "feed" );

        activity = Activity.newActivity(Activity.VERB_POST, null, "I ate another sammich",
                null, userA, null, "tweet", null, null );
        properties.putAll( activity.getProperties() );
        app.putAll( properties );

		activityEntity = app.testRequest(ServiceAction.POST, 1, "users", userA.getUuid(), "activities").getEntity();
		assertNotNull( activityEntity );

        activity = Activity.newActivity( Activity.VERB_POST, null, "I ate a cookie", null, userA, null,
                "tweet", null, null );
        properties.putAll( activity.getProperties() );
        app.putAll( properties );

		activityEntity = app.testRequest( ServiceAction.POST, 1, "users", userA.getUuid(), "activities" ).getEntity();
		assertNotNull(activityEntity);

		activity = Activity.newActivity( Activity.VERB_CHECKIN, null, "I'm at the cookie shop", null, userA, null,
                Activity.OBJECT_TYPE_PLACE, "Cookie Shop", null );
        properties.putAll( activity.getProperties() );
        app.putAll( properties );

		activityEntity = app.testRequest( ServiceAction.POST, 1, "users", userA.getUuid(), "activities").getEntity();
		assertNotNull(activityEntity);

		app.testRequest(ServiceAction.GET, 4, "users", userC.getUuid(), "feed");

		app.testRequest(ServiceAction.GET, 2, "users", userC.getUuid(), "feed",
                Query.fromQL("select * where content contains 'cookie'"));

		app.testRequest(ServiceAction.GET, 1, "users", userC.getUuid(), "feed",
                Query.fromQL("select * where verb='post' and content contains 'cookie'"));

		app.add("username", "finn");
		app.add("email", "finn@ooo.com");

		Entity userD = app.testRequest(ServiceAction.POST, 1, "users").getEntity();
		assertNotNull(userD);

		app.testRequest(ServiceAction.POST, 1, "users", userD.getUuid(),
                "connections", "following", userA.getUuid());

		app.testRequest(ServiceAction.GET, 4, "users", userD.getUuid(), "feed");
	}
}
