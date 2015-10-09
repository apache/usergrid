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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.java.client.exception.ClientException;
import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Organization;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.state.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;


//TODO: add error checking to each of the REST calls.
/**
 * Manages the Management/ORG endpoint.
 */
public class OrgResource  extends NamedResource {
    private static final Logger logger = LoggerFactory.getLogger(OrgResource.class);

    public OrgResource( final ClientContext context, final UrlResource parent ) {
        super( "organizations", context, parent );
    }


    public OrganizationResource org( final String orgname ){
        return new OrganizationResource( orgname,context,this );
    }

    /**
     * This post is for the POST params case, where the entire call is made using queryParameters.
     */
    public Organization post(Form form){

        // Seems like an apiresponse can't handle what gets returned from the from urlended media type

        ApiResponse response = getTarget().request()
            .accept(MediaType.APPLICATION_JSON)
            .post( Entity.form( form ), ApiResponse.class);

        Organization organization = new Organization(response);
        organization.setOwner( response );
        return organization;
    }

    /**
     * This post is for the POST params case, where the entire call is made using queryParameters.
     */
    public Organization post(QueryParameters parameters){

        // Seems like an ApiResponse can't handle what gets returned from the from URL encoded media type
        WebTarget resource = addParametersToResource( getTarget(), parameters);

        // use string type so we can log actual response from server
        String responseString = resource.request()
            .accept( MediaType.APPLICATION_JSON )
            .post( Entity.json( null ), String.class );

        logger.debug("Response from post: " + responseString);

        ObjectMapper mapper = new ObjectMapper();
        ApiResponse response;
        try {
            response = mapper.readValue( new StringReader(responseString), ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        Organization org = new Organization(response);
        org.setOwner( response );

        return org;
    }

    public Organization post(Organization organization){

        ApiResponse apiResponse = getTarget( false ).request()
            .accept( MediaType.APPLICATION_JSON )
            .post( Entity.json( organization ), ApiResponse.class );


        Organization org = new Organization(apiResponse);
        org.setOwner( apiResponse );

        return org;
    }

    public Organization post(Organization organization, Token token) {

        ApiResponse apiResponse = getTarget( true, token ).request()
            .accept( MediaType.APPLICATION_JSON )
            .post( Entity.json( organization ), ApiResponse.class );


        Organization org = new Organization(apiResponse);
        org.setOwner( apiResponse );

        return org;
    }

    public Organization put(Organization organization){

        // use string type so we can log actual response from server
        String responseString = getTarget().request()
            .accept( MediaType.APPLICATION_JSON )
            .post( Entity.json( organization ), String.class );

        logger.debug("Response from put: " + responseString);

        ObjectMapper mapper = new ObjectMapper();
        ApiResponse response;
        try {
            response = mapper.readValue( new StringReader(responseString), ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        Organization org = new Organization(response);
        org.setOwner( response );

        return org;

    }

    public Organization get(){
        throw new UnsupportedOperationException("service doesn't exist");
    }

    public CredentialsResource credentials(){
        return new CredentialsResource(  context ,this );
    }

}
