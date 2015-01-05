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
package org.apache.usergrid.cassandra;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AnotherCassandraResourceIT {
    private static final Logger logger = LoggerFactory.getLogger( AnotherCassandraResourceIT.class );
    private SpringResource springResource = SpringResource.getInstance();


    @Test
    public void testItAgain() throws Exception {
        String testBean = springResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean: {}", testBean );
    }


    @Test
    public void testItAgainAndAgain() throws Exception {
        String testBean = springResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
    }


    @Test
    public void testItAgainAndAgain2() throws Exception {
        String testBean = springResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
    }


    @Test
    public void testItAgainAndAgain3() throws Exception {
        String testBean = springResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
    }


    @Test
    public void testItAgainAndAgain4() throws Exception {
        String testBean = springResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
    }


    @Test
    public void testItAgainAndAgain5() throws Exception {
        String testBean = springResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
    }


    @Test
    public void testItAgainAndAgain6() throws Exception {
        String testBean = springResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
    }
}
