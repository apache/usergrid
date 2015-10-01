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
package org.apache.usergrid.batch;


import org.junit.Test;

import com.google.common.base.CharMatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** @author zznate */
public class AppArgsTest {

    @Test
    public void verifyDefaults() {
        AppArgs aa = AppArgs.parseArgs( new String[] { "" } );
        assertEquals( "127.0.0.1", aa.getHost() );
        assertEquals( 9160, aa.getPort() );
    }


    @Test
    public void verifyArgs() {
        AppArgs aa =
                AppArgs.parseArgs( new String[] { "-host", "127.0.0.2", "-appContext", "classpath:/appContext.xml" } );
        assertEquals( "127.0.0.2", aa.getHost() );
        assertNotNull( aa.getAppContext() );
    }


    @Test
    public void verifyContextSwitch() {
        AppArgs appArgs = AppArgs.parseArgs( new String[] { "-appContext", "classpath:/appContext.xml" } );
        assertEquals( "/appContext.xml", getIndex( appArgs.getAppContext() ) );
        appArgs = AppArgs.parseArgs( new String[] { "-appContext", "/appContext.xml" } );
        assertEquals( "/appContext.xml", getIndex( appArgs.getAppContext() ) );
    }


    private String getIndex( String path ) {
        int index = CharMatcher.is( ':' ).indexIn( path );
        if ( index > 0 ) {
            return path.substring( ++index );
        }
        else {
            return path;
        }
    }
}
