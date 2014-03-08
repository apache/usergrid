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


import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.utils.JsonUtils;

import static org.junit.Assert.assertEquals;


public class ServiceInfoTest {

    private static final Logger logger = LoggerFactory.getLogger( ServiceInfoTest.class );


    @Test
    public void testServiceInfo() throws Exception {

        testServiceInfo( "/users", "users.UsersService" );
        testServiceInfo( "/users/*/messages", "users.messages.MessagesService" );
        testServiceInfo( "/users/*/messages/*/likes", "users.messages.likes.LikesService" );
        testServiceInfo( "/users/*/groups:group", "users.groups.GroupsService" );
        testServiceInfo( "/users/*/likes", "users.likes.LikesService" );
        testServiceInfo( "/users/*/likes:bar/*/wines", "users.likes.wines.BarWinesService" );
        testServiceInfo( "/restaurants", "restaurants.RestaurantsService" );
        testServiceInfo( "/blogpack.posts/", "blogpack.PostsService" );
        testServiceInfo( "/blogpack.posts/*/comments", "blogpack.posts.comments.CommentsService" );
        testServiceInfo( "/blogpack.posts/*/comments:blogpost.comment", "blogpack.posts.comments.CommentsService" );
    }


    @Test
    public void testFallback() throws Exception {

        dumpFallback( "/users/*/friends/*/recommendations:food" );
        dumpFallback( "/users/*/friends/*/recommendations" );
        dumpFallback( "/users/*/friends" );
        dumpFallback( "/users/*/likes:bar/*/wines" );
    }


    @Test
    public void testTypes() throws Exception {

        dumpType( "/users/*/friends/*/recommendations:food", "food" );
        dumpType( "/users/*/messages", "entity" );
        dumpType( "/users/*/messages:cow", "cow" );
        dumpType( "/users/*/friends:user/*/messages:cow", "cow" );
        dumpType( "/users/*/friends", "entity" );
        dumpType( "/users/*/likes:bar/*/wines", "entity" );
    }


    public void dumpFallback( String start ) {
        List<String> patterns = ServiceInfo.getPatterns( start );
        logger.info( JsonUtils.mapToFormattedJsonString( patterns ) );
    }


    public void dumpType( String start, String expectedType ) {
        String type = ServiceInfo.determineType( start );
        logger.info( start + " = " + type );
        assertEquals( expectedType, type );
    }


    public void testServiceInfo( String s, String... classes ) {
        ServiceInfo info = ServiceInfo.getServiceInfo( s );
        try {
            if ( info != null ) {
                logger.info( JsonUtils.mapToFormattedJsonString( info ) );
            }
            else {
                logger.info( "info = " + info );
            }
        }
        catch ( Throwable t ) {
            logger.error( "Error logging object " + info.toString() );
        }
        int i = 0;
        for ( String pattern : info.getPatterns() ) {
            String className = ServiceInfo.getClassName( pattern );
            logger.info( pattern + " = " + className );
            if ( ( classes != null ) && ( i < classes.length ) ) {
                assertEquals( classes[i], className );
            }
            i++;
        }
    }
}
