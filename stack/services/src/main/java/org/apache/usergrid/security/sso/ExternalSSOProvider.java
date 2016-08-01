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
package org.apache.usergrid.security.sso;

import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.security.tokens.TokenInfo;

import java.util.Map;

/**
 * Created by ayeshadastagiri on 6/22/16.
 */
public interface ExternalSSOProvider {

    /** Authenticate a userId and external token against this provider */
    TokenInfo validateAndReturnTokenInfo(String token, long ttl) throws Exception;

    /** Authenticate a userId and external token against this provider */
    UserInfo validateAndReturnUserInfo(String token, long ttl) throws Exception;

    /** Decode the token, if supported, and return any information encoded with the token */
    Map<String, String> getDecodedTokenDetails(String token) throws Exception;

    Map<String, Object> getAllTokenDetails(String token, String keyUrl) throws Exception;

    String getExternalSSOUrl() throws Exception;

}
