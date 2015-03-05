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


import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.*;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;


/** A {@link org.junit.rules.TestRule} that cleans up the Shiro Subject's ThreadState. */
public class ClearShiroSubject extends ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger( ClearShiroSubject.class );



    @Override
    protected void before() throws Throwable {
        super.before();
        clear();
    }

    @Override
    protected void after() {
        super.after();
        clear();
    }

    public void clear(){
        Subject subject = SecurityUtils.getSubject();

        if ( subject == null ) {

            LOG.info( "Shiro Subject was null. No need to clear manually." );
            return;
        }

        new SubjectThreadState( subject ).clear();

        LOG.info( "Shiro Subject was NOT null. Subject has been cleared manually." );
    }
}
