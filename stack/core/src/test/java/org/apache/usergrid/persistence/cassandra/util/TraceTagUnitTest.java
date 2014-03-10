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
package org.apache.usergrid.persistence.cassandra.util;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** @author zznate */
public class TraceTagUnitTest {

    private TraceTagManager traceTagManager;
    private Slf4jTraceTagReporter traceTagReporter;
    private TaggedOpTimer taggedOpTimer;


    @Before
    public void setup() {
        traceTagManager = new TraceTagManager();
        traceTagReporter = new Slf4jTraceTagReporter();
        taggedOpTimer = new TaggedOpTimer( traceTagManager );
    }


    @Test
    public void createAttachDetach() throws Exception {
        TraceTag traceTag = traceTagManager.create( "testtag1" );
        traceTagManager.attach( traceTag );
        TimedOpTag timedOpTag = ( TimedOpTag ) taggedOpTimer.start( "op-tag-name" );
        Thread.currentThread().sleep( 500 );
        taggedOpTimer.stop( timedOpTag, "op-tag-name", true );
        assertTrue( timedOpTag.getElapsed() >= 500 );
        assertEquals( timedOpTag, traceTag.iterator().next() );
        traceTagManager.detach();
    }
}
