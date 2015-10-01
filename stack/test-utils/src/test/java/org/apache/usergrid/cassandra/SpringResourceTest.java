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

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertSame;


/** This tests the CassandraResource. */
public class SpringResourceTest {
    public static final Logger LOG = LoggerFactory.getLogger( SpringResourceTest.class );




    /**
     * Fires up two Cassandra instances on the same machine.
     *
     * @throws Exception if this don't work
     */
    @Test
    public void testDoubleTrouble() throws Throwable {
        SpringResource c1 = SpringResource.getInstance();
        LOG.info( "Starting up first Spring instance: {}", c1 );

        LOG.debug( "Waiting for the new instance to come online." );

        SchemaManager c1SchemaManager = c1.getBean( SchemaManager.class );

        SpringResource c2 = SpringResource.getInstance();
        LOG.debug( "Starting up second Spring instance: {}", c2 );

        SchemaManager c2SchemaManager = c2.getBean( SchemaManager.class );

        LOG.debug( "Waiting a few seconds for second instance to be ready before shutting down." );

        assertSame("Instances should be from the same spring context", c1SchemaManager, c2SchemaManager);

    }
}
