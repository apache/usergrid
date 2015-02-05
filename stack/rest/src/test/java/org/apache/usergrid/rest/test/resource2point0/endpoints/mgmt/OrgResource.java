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

package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;

import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.User;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.sun.jersey.api.client.WebResource;

import com.sun.jersey.api.representation.Form;


//TODO: add error checking to each of the rest calls.
/**
 * Manages the Management/ORG endpoint.
 */
public class OrgResource  extends NamedResource {

    public OrgResource( final ClientContext context, final UrlResource parent ) {
        super( "orgs", context, parent );
    }


    public OrganizationResource organization (final String orgname){
        return new OrganizationResource( orgname,context,this );
    }

    /**
     * This post is for the POST params case, where the entire call is made using queryParameters
     * @return
     */
    public Organization post(Form form){
        //Seems like an apiresponse can't handle what gets returned from the from urlended media type

        ApiResponse response = getResource().type( MediaType.APPLICATION_FORM_URLENCODED )
                                       .accept( MediaType.APPLICATION_JSON ).post( ApiResponse.class, form );


        Organization organization = new Organization(response);
        organization.setOwner( response );
        return organization;
    }

    /**
     * This post is for the POST params case, where the entire call is made using queryParameters
     * @return
     */
    public Organization post(QueryParameters parameters){
        //Seems like an apiresponse can't handle what gets returned from the from urlended media type
        WebResource resource = addParametersToResource( getResource(), parameters);

            ApiResponse response = resource.type( MediaType.APPLICATION_FORM_URLENCODED )
                                        .accept( MediaType.APPLICATION_JSON ).post( ApiResponse.class );


        Organization org = new Organization(response);
        org.setOwner( response );
        return org;
    }

    public Organization post(Organization organization){
        ApiResponse response = getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                     .post( ApiResponse.class,organization );

        Organization org = new Organization(response);
        org.setOwner( response );

        return org;
    }

    public Organization put(Organization organization){
        ApiResponse response = getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                .put(ApiResponse.class, organization);

        Organization org = new Organization(response);
        org.setOwner( response );

        return org;

    }

    public Organization get(){
        throw new UnsupportedOperationException("service doesn't exist");
    }

    public void delete(){
        ApiResponse response = getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                .delete(ApiResponse.class);

    }
    public CredentialsResource credentials(){
        return new CredentialsResource(  context ,this );
    }

}
