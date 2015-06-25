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
package org.apache.usergrid.system;


import java.util.Date;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;
import org.apache.usergrid.utils.MapUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** @author zznate */
public class UsergridSystemMonitorIT {

    private UsergridSystemMonitor usergridSystemMonitor;


    @Before
    public void setupLocal() {
        usergridSystemMonitor = ConcurrentProcessSingleton.getInstance().getSpringResource().getBean( UsergridSystemMonitor.class );
    }


    @Test
    public void testVersionNumber() {
        assertEquals( "0.1", usergridSystemMonitor.getBuildNumber() );
    }


    @Test
    public void testIsCassandraAlive() {
        assertTrue( usergridSystemMonitor.getIsCassandraAlive() );
    }


    @Test
    public void verifyLogDump() {
        String str = UsergridSystemMonitor.formatMessage( 1600L, MapUtils.hashMap( "message", "hello" ) );

        assertTrue( StringUtils.contains( str, "hello" ) );

        usergridSystemMonitor.maybeLogPayload( 16000L, "foo", "bar", "message", "some text" );
        usergridSystemMonitor.maybeLogPayload( 16000L, new Date() );
    }
}
