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
package org.apache.usergrid.utils;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;


public class TestInflector {
    private static final Logger logger = LoggerFactory.getLogger( TestInflector.class );


    @Test
    public void testInflector() {

        testSingularize( "users", "user" );
        testSingularize( "groups", "group" );
        testSingularize( "entities", "entity" );
        testSingularize( "messages", "message" );
        testSingularize( "activities", "activity" );
        testSingularize( "binaries", "binary" );
        testSingularize( "data", "data" );

        testSingularize( "user", "user" );
        testSingularize( "group", "group" );
        testSingularize( "entity", "entity" );
        testSingularize( "message", "message" );
        testSingularize( "activity", "activity" );
        testSingularize( "binary", "binary" );

        testPluralize( "user", "users" );
        testPluralize( "group", "groups" );
        testPluralize( "entity", "entities" );
        testPluralize( "message", "messages" );
        testPluralize( "activity", "activities" );
        testPluralize( "binary", "binaries" );
        testPluralize( "data", "data" );

        testPluralize( "users", "users" );
        testPluralize( "groups", "groups" );
        testPluralize( "entities", "entities" );
        testPluralize( "messages", "messages" );
        testPluralize( "activities", "activities" );
        testPluralize( "binaries", "binaries" );

        testPluralize( "com.usergrid.resources.user", "com.usergrid.resources.users" );
        testSingularize( "com.usergrid.resources.users", "com.usergrid.resources.user" );
    }


    public void testSingularize( String p, String expected ) {
        String s = Inflector.getInstance().singularize( p );
        logger.info( "Inflector says singular form of " + p + " is " + s );
        assertEquals( "singular form of " + p + " not expected value", expected, s );
    }


    public void testPluralize( String s, String expected ) {
        String p = Inflector.getInstance().pluralize( s );
        logger.info( "Inflector says plural form of " + s + " is " + p );
        assertEquals( "plural form of " + s + " not expected value", expected, p );
    }
}
