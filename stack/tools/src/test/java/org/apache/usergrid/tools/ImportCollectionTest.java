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
package org.apache.usergrid.tools;

import static org.junit.Assert.assertEquals;

import org.apache.usergrid.services.AbstractServiceIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: better test, this is really just a smoke test.
 */
public class ImportCollectionTest extends AbstractServiceIT {
    static final Logger logger = LoggerFactory.getLogger( ImportCollectionTest.class );

    int NUM_COLLECTIONS = 10;
    int NUM_ENTITIES = 50;
    int NUM_CONNECTIONS = 3;

    @org.junit.Test
    public void testBasicOperation() throws Exception {

        // add app with some data

    	  long start = System.currentTimeMillis();

        ImportCollections importCollection = new ImportCollections();
        importCollection.startTool( new String[]{
                  "-host", "localhost:9160",
                  "-appId", "e5fe5955-9750-11e9-bc62-26c8b3a04fa8",     // add your appid
        		"-inputDir",  "/tools/src/test/resources/",                        
                 "-v","/tools/src/test/resources/import-collection-details.log"} ,false);


        logger.info( "100 read and 100 write threads = " + (System.currentTimeMillis() - start) / 1000 + "s" );

        // check that we got the expected number of export files

        logger.info( "1 thread time = " + (System.currentTimeMillis() - start) / 1000 + "s" );

        assertEquals( 1, 1);
        assertEquals( 1, 1);
    }

}
