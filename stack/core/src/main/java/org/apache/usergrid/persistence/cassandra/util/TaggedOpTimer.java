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


import me.prettyprint.cassandra.connection.HOpTimer;


/**
 * Trace the timed execution of a 'tag' over the course of a number of operations. Facilitates integration with
 * Dapper-style trace logging infrastructure.
 *
 * @author zznate
 */
public class TaggedOpTimer implements HOpTimer {

    private TraceTagManager traceTagManager;


    public TaggedOpTimer( TraceTagManager traceTagManager ) {
        this.traceTagManager = traceTagManager;
    }


    @Override
    public Object start( String tagName ) {
        // look for our threadLocal. if not present, return this.
        return traceTagManager.timerInstance();
    }


    @Override
    public void stop( Object timedOpTag, String opTagName, boolean success ) {
        if ( timedOpTag instanceof TimedOpTag ) {
            TimedOpTag t = ( ( TimedOpTag ) timedOpTag );
            t.stopAndApply( opTagName, success );
            traceTagManager.addTimer( t );
        }
    }
}
