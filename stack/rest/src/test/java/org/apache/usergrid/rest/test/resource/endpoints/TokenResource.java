/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.rest.test.resource.endpoints;


import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 * Classy class class.
 */
public class TokenResource extends NamedResource {
    public TokenResource(final ClientContext context, final UrlResource parent) {
        super("token", context, parent);
    }


    /**
     * Obtains an access token and sets the token for the context to use in later calls
     *
     * @return
     */
    public Token post(QueryParameters params) {
        WebTarget resource = getTarget( false );
        resource = addParametersToResource(resource, params);
        Token token = resource.request()
            .accept( MediaType.APPLICATION_JSON )
            .get(Token.class);

        this.context.setToken(token);
        return token;
    }

    /**
     * Obtains an access token and sets the token for the context to use in later calls
     *
     * @return
     */
    public Token post() {
        Token token = getTarget().request().accept( MediaType.APPLICATION_JSON ).post( Entity.json(null), Token.class );
        this.context.setToken(token);
        return token;
    }

    /**
     * Obtains an access token and sets the token for the context to use in later calls
     *
     * @param token
     * @return
     */
    public Token post(Token token) {
        token = getTarget(false).request()
            .accept(MediaType.APPLICATION_JSON)
            .post( Entity.json( token ), Token.class);
        this.context.setToken(token);
        return token;
    }

    public TokenResource setToken(Token token) {
        this.context.setToken(token);
        return this;
    }

    public TokenResource clearToken() {
        this.context.setToken(null);
        return this;
    }

}
