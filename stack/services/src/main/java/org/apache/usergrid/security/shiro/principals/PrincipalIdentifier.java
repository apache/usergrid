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
package org.apache.usergrid.security.shiro.principals;


import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.security.shiro.credentials.AccessTokenCredentials;


public abstract class PrincipalIdentifier {

    AccessTokenCredentials accessTokenCredentials;


    public UserInfo getUser() {
        return null;
    }


    public boolean isDisabled() {
        return false;
    }


    public boolean isActivated() {
        return true;
    }


    public AccessTokenCredentials getAccessTokenCredentials() {
        return accessTokenCredentials;
    }


    public void setAccessTokenCredentials( AccessTokenCredentials accessTokenCredentials ) {
        this.accessTokenCredentials = accessTokenCredentials;
    }
}
