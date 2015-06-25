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
package org.apache.usergrid.security.tokens;


import java.util.Map;

import org.apache.usergrid.security.AuthPrincipalInfo;


public interface TokenService {

    /**
     * Create the token with the given duration.  A duration value of 0 equals the default value specified in the
     * properties It is not possible to specify a duration greater than the maximum system allowed duration.
     */
    public String createToken( TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                               Map<String, Object> state, long duration ) throws Exception;

    public void importToken( String token, TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                               Map<String, Object> state, long duration ) throws Exception;

    /** Get the token info for the string version of this token */
    public TokenInfo getTokenInfo( String token ) throws Exception;

    /** Get the max token age in milliseconds */
    public long getMaxTokenAge( String token );


    /** Get the max token age in seconds */
    public long getMaxTokenAgeInSeconds( String token );

    /**
     * Expire the token.  If the token does not exist, this operation will not throw an error.  Implementations should
     * always delete this token, regardless of state.
     */
    public void revokeToken( String token );

    /**
     * Remove all tokens currently issued for the given AuthPrincipal.  Removes the specified type of token for the
     * given principal uuid and application uuid
     */
    public void removeTokens( AuthPrincipalInfo principal ) throws Exception;
}
