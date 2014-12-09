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
package org.apache.usergrid.rest.test.resource2point0;


import java.net.URI;

import org.junit.ClassRule;

import org.apache.usergrid.rest.ITSetup;
import org.apache.usergrid.rest.RestITSuite;

import javax.ws.rs.core.Application;


//import com.sun.jersey.api.json.JSONConfiguration;
//import com.sun.jersey.test.framework.WebAppDescriptor;


public class AbstractRestIT {

    @ClassRule
    public static ITSetup setup = new ITSetup( RestITSuite.cassandraResource );



}
