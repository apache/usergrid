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
package org.apache.usergrid.rest.management.organizations;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.management.exceptions.ManagementException;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.management.ManagementResource;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;
import static org.apache.usergrid.security.shiro.utils.SubjectUtils.isPermittedAccessToOrganization;


@Component( "org.apache.usergrid.rest.management.organizations.OrganizationsResource" )
@Scope( "prototype" )
@Produces( {
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
} )
public class OrganizationsResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( OrganizationsResource.class );

    public static final String ORGANIZATION_PROPERTIES = "properties";
    public static final String ORGANIZATION_CONFIGURATION = "configuration";

    @Autowired
    private ApplicationCreator applicationCreator;


    public OrganizationsResource() {
    }

    @Path(RootResource.ORGANIZATION_ID_PATH)
    public OrganizationResource getOrganizationById( @Context UriInfo ui,
                                                     @PathParam( "organizationId" ) String organizationIdStr )
            throws Exception {
        OrganizationInfo organization = management.getOrganizationByUuid( UUID.fromString( organizationIdStr ) );
        if ( organization == null ) {
            throw new ManagementException( "Could not find organization for ID: " + organizationIdStr );
        }



        return getSubResource( OrganizationResource.class ).init( organization );
    }

    @Path( "{organizationName}" )
    public OrganizationResource getOrganizationByName( @Context UriInfo ui,
                                                       @PathParam( "organizationName" ) String organizationName )
            throws Exception {
        OrganizationInfo organization = management.getOrganizationByName(organizationName);
        if ( organization == null ) {
            throw new ManagementException( "Could not find organization for name: " + organizationName );
        }

        return getSubResource( OrganizationResource.class ).init(organization);
    }


    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse newOrganization( @Context UriInfo ui, Map<String, Object> json,
                                            @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
            throws Exception {

        logger.debug("newOrganization");

        ApiResponse response = createApiResponse();
        response.setAction( "new organization" );

        if(json==null){
            throw new IllegalArgumentException("missing json post data");
        }
        String organizationName = ( String ) json.remove( "organization" );
        String username = ( String ) json.remove( "username" );
        String name = ( String ) json.remove( "name" );
        String email = ( String ) json.remove( "email" );
        String password = ( String ) json.remove( "password" );
        Map<String, Object> orgProperties = ( Map<String, Object> ) json.remove( ORGANIZATION_PROPERTIES );

        return newOrganization( ui, organizationName, username, name, email, password, json, orgProperties, callback );
    }


    @POST
    @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse newOrganizationFromForm( @Context UriInfo ui,
                                                    @FormParam( "organization" ) String organizationNameForm,
                                                    @QueryParam( "organization" ) String organizationNameQuery,
                                                    @FormParam( "username" ) String usernameForm,
                                                    @QueryParam( "username" ) String usernameQuery,
                                                    @FormParam( "name" ) String nameForm,
                                                    @QueryParam( "name" ) String nameQuery,
                                                    @FormParam( "email" ) String emailForm,
                                                    @QueryParam( "email" ) String emailQuery,
                                                    @FormParam( "password" ) String passwordForm,
                                                    @QueryParam( "password" ) String passwordQuery,
                                                    @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
            throws Exception {

        logger.debug( "New organization: {}", organizationNameForm );

        String organizationName = organizationNameForm != null ? organizationNameForm : organizationNameQuery;
        String username = usernameForm != null ? usernameForm : usernameQuery;
        String name = nameForm != null ? nameForm : nameQuery;
        String email = emailForm != null ? emailForm : emailQuery;
        String password = passwordForm != null ? passwordForm : passwordQuery;

        return newOrganization( ui, organizationName, username, name, email, password, null, null, callback );
    }


    /** Create a new organization */
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    private ApiResponse newOrganization( @Context UriInfo ui, String organizationName, String username, String name,
                                             String email, String password, Map<String, Object> userProperties,
                                             Map<String, Object> orgProperties, String callback ) throws Exception {

        final boolean externalTokensEnabled =
                !StringUtils.isEmpty( properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );

        if ( externalTokensEnabled ) {
            throw new IllegalArgumentException( "Organization / Admin Users must be created via " +
                    properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );
        }

        Preconditions
                .checkArgument( StringUtils.isNotBlank( organizationName ), "The organization parameter was missing" );

        Preconditions.checkArgument(
            StringUtils.isNotBlank( organizationName ), "The organization parameter was missing" );

        logger.debug( "New organization: {}", organizationName );

        ApiResponse response = createApiResponse();
        response.setAction( "new organization" );

        OrganizationOwnerInfo organizationOwner = management
                .createOwnerAndOrganization( organizationName, username, name, email, password, false, false,
                        userProperties, orgProperties );

        if ( organizationOwner == null ) {
            logger.info( "organizationOwner is null, returning. organization: {}", organizationName );
            return null;
        }

        applicationCreator.createSampleFor( organizationOwner.getOrganization() );

        response.setData( organizationOwner );
        response.setSuccess();

        logger.info( "New organization complete: {}", organizationName );
        return response;
    }

    /*
     * @POST
     *
     * @Consumes(MediaType.MULTIPART_FORM_DATA) public JSONWithPadding
     * newOrganizationFromMultipart(@Context UriInfo ui,
     *
     * @FormDataParam("organization") String organization,
     *
     * @FormDataParam("username") String username,
     *
     * @FormDataParam("name") String name,
     *
     * @FormDataParam("email") String email,
     *
     * @FormDataParam("password") String password) throws Exception { return
     * newOrganizationFromForm(ui, organization, username, name, email,
     * password); }
     */
}
