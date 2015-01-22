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

package org.apache.usergrid.rest.applications;


import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ApplicationDeleteTest  extends AbstractRestIT {
    private static final Logger log = LoggerFactory.getLogger(ApplicationDeleteTest.class);

    @Rule
    public TestContextSetup context = new TestContextSetup( this );

    @Test
    public void testBasicOperation() throws Exception {

        // create a user
        
        // crete an organization 
        
        // create an application
        
        // create a collection with two entities
        
        // test that we can query those entities
        
        // delete the application
        
        // test that we cannot delete the application a second time
        
        // test that we can no longer query for those entities
    }
}
