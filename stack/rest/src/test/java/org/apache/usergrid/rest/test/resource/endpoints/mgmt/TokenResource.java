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
package org.apache.usergrid.rest.test.resource.endpoints.mgmt;


import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;


/**
 * Called by the ManagementResource. This contains anything token related that comes back to the ManagementResource.
 */
public class TokenResource extends NamedResource {
    public TokenResource(final ClientContext context, final UrlResource parent) {
        super("token", context, parent);
    }

    public Token get(String username, String password){
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "grant_type", "password" );
        queryParameters.addParam( "username", username );
        queryParameters.addParam( "password", password );
        return get(queryParameters);

    }
    /**
     * Obtains an access token and sets the token for the context to use in later calls
     *
     * @return
     */
    public Token get(QueryParameters params) {
        WebTarget resource = getTarget( false );
        resource = addParametersToResource(resource, params);
        Token token = resource.request().accept(MediaType.APPLICATION_JSON).get(Token.class);

        this.context.setToken(token);
        return token;
    }


    /**
     * Convinece method to set the token needed for each call.
     * @param token
     * @return
     */
    public TokenResource setToken(Token token) {
        this.context.setToken(token);
        return this;
    }
}
