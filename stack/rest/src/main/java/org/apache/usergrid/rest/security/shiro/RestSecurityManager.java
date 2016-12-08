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
package org.apache.usergrid.rest.security.shiro;


import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.rest.security.shiro.session.HttpRequestSessionManager;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;


public class RestSecurityManager extends DefaultWebSecurityManager {

    private static final Logger logger = LoggerFactory.getLogger( RestSecurityManager.class );


    public RestSecurityManager() {
        setSessionManager( new HttpRequestSessionManager() );
    }


    public RestSecurityManager( Realm singleRealm ) {
        super( singleRealm );
        setSessionManager( new HttpRequestSessionManager() );
    }


    public RestSecurityManager( Collection<Realm> realms ) {
        super( realms );
        setSessionManager( new HttpRequestSessionManager() );
    }


    @Override
    public void setSessionManager( SessionManager sessionManager ) {
        if ( !( sessionManager instanceof HttpRequestSessionManager ) ) {
            logger.info( "Replacing {} with HttpRequestSessionManager", sessionManager );
            sessionManager = new HttpRequestSessionManager();
        }
        super.setSessionManager( sessionManager );
    }
}
