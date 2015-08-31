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
package org.apache.usergrid.persistence;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.utils.JsonUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



public class EntityTest {
    private static final Logger logger = LoggerFactory.getLogger( EntityTest.class );


    @Test
    // @Ignore( "Fix this then enable EntityTest.testEntityClasses:45 » ConcurrentModification" )
    public void testEntityClasses() throws Exception {
        logger.info( "testEntityClasses" );

        Schema mapper = Schema.getDefaultSchema();

        assertEquals( "group", mapper.getEntityType( Group.class ) );

        assertEquals( User.class, mapper.getEntityClass( "user" ) );

        Entity entity = EntityFactory.newEntity( null, "user" );
        assertEquals( User.class, entity.getClass() );

        User user = ( User ) entity;
        user.setUsername( "testuser" );
        assertEquals( user.getUsername(), user.getProperty( "username" ) );

        user.setProperty( "username", "blahblah" );
        assertEquals( "blahblah", user.getUsername() );

        entity = EntityFactory.newEntity( null, "foobar" );
        assertEquals( DynamicEntity.class, entity.getClass() );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( Schema.PROPERTY_UUID, new UUID( 1, 2 ) );
        properties.put( "foo", "bar" );
        entity.setProperties( properties );

        assertEquals( new UUID( 1, 2 ), entity.getUuid() );
        assertEquals( new UUID( 1, 2 ), entity.getProperty( Schema.PROPERTY_UUID ) );
        assertEquals( "bar", entity.getProperty( "foo" ) );
    }


    @SuppressWarnings("unchecked")
    @Test
    // @Ignore( "Fix this and enable: EntityTest.testJson:83 » ConcurrentModification" )
    public void testJson() throws Exception {

        User user = new User();
        // user.setId(UUIDUtils.newTimeUUID());
        user.setProperty( "foo", "bar" );
        assertEquals( "{\"type\":\"user\",\"foo\":\"bar\"}", JsonUtils.mapToJsonString( user ) );

        String json = "{\"username\":\"edanuff\", \"bar\" : \"baz\" }";
        Map<String, Object> p = ( Map<String, Object> ) JsonUtils.parse( json );
        user = new User();
        user.addProperties( p );
        assertEquals( "edanuff", user.getUsername() );
        assertEquals( "baz", user.getProperty( "bar" ) );

        json = "{\"username\":\"edanuff\", \"foo\" : {\"a\":\"bar\", \"b\" : \"baz\" } }";
        p = ( Map<String, Object> ) JsonUtils.parse( json );
        user = new User();
        user.addProperties( p );
        assertEquals( "edanuff", user.getUsername() );
        assertTrue( Map.class.isAssignableFrom( user.getProperty( "foo" ).getClass() ) );
        assertEquals( "baz", ( ( Map<String, Object> ) user.getProperty( "foo" ) ).get( "b" ) );
    }
}
