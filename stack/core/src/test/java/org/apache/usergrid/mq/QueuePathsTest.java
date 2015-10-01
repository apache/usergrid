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
package org.apache.usergrid.mq;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.usergrid.mq.Queue.getQueueParentPaths;
import static org.apache.usergrid.mq.Queue.normalizeQueuePath;
import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;



public class QueuePathsTest {
    private static final Logger LOG = LoggerFactory.getLogger( QueuePathsTest.class );


    @Test
    // TODO - why does this test case not have assertions to test results?
    // tests should not be written like this: what's the point? If it's
    // code coverage this is still bad.
    public void testPaths() throws Exception {
        LOG.info( normalizeQueuePath( "a/b/c" ) );
        LOG.info( normalizeQueuePath( "a/b/c/" ) );
        LOG.info( normalizeQueuePath( "/a/b/c" ) );
        LOG.info( normalizeQueuePath( "/////a/b/c" ) );
        LOG.info( normalizeQueuePath( "/" ) );

        LOG.info( mapToFormattedJsonString( getQueueParentPaths( "/a/b/c" ) ) );
        LOG.info( mapToFormattedJsonString( getQueueParentPaths( "/" ) ) );
    }
}
