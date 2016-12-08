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


import javax.annotation.Resource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Use Aspects to apply a trace
 *
 * @author zznate
 */
public class TraceTagAspect {
    private static final Logger logger = LoggerFactory.getLogger( TraceTagAspect.class );

    @Resource
    private TraceTagManager traceTagManager;


    public Object applyTrace( ProceedingJoinPoint pjp ) throws Throwable {
        String tagName = pjp.toLongString();

        if (logger.isTraceEnabled()) {
            logger.trace("Applying trace on {}", tagName);
        }

        TimedOpTag timedOpTag = traceTagManager.timerInstance();
        boolean success = true;
        try {
            return pjp.proceed();
        }
        catch ( Exception e ) {
            success = false;
            throw e;
        }
        finally {
            timedOpTag.stopAndApply( tagName, success );
            traceTagManager.addTimer( timedOpTag );

            if (logger.isTraceEnabled()) {
                logger.trace("TimedOpTag added in Aspect on {}", tagName);
            }
        }
    }
}
