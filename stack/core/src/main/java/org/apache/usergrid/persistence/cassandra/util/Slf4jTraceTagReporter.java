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


import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple reporter which dumps to class logger at info level.
 * <p/>
 * You can configure a logger with the name "TraceTagReporter" explicitly which, if not in a logging context, then the
 * class level logger will be used.
 *
 * @author zznate
 */
public class Slf4jTraceTagReporter implements TraceTagReporter {
    private Logger logger;


    public Slf4jTraceTagReporter() {
        logger = LoggerFactory.getLogger( "TraceTagReporter" );
        if ( logger == null ) {
            logger = LoggerFactory.getLogger( Slf4jTraceTagReporter.class );
        }
    }


    @Override
    public void report( TraceTag traceTag ) {
        logger.info( "TraceTag: {}", traceTag.getTraceName() );
        for ( TimedOpTag timedOpTag : traceTag ) {
            logger.info( "----opId: {} opName: {} startTime: {} elapsed: {}",
                    timedOpTag.getOpTag(), timedOpTag.getTagName(), new Date( timedOpTag.getStart() ),
                    timedOpTag.getElapsed()
            );
        }
        logger.info( "------" );
    }


    @Override
    public void reportUnattached( TimedOpTag timedOpTag ) {
        logger.info( "--[unattached]-- {}", timedOpTag );
    }
}
