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
package org.apache.usergrid.rest.management.organizations.applications;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isEmpty;


@Component( "org.apache.usergrid.rest.management.organizations.applications.ApplicationsResource" )
@Scope( "prototype" )
@Produces( {
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
} )
public class ApplicationsResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( ApplicationsResource.class );

    OrganizationInfo organization;


    public ApplicationsResource() {
    }


    public ApplicationsResource init( OrganizationInfo organization ) {
        this.organization = organization;
        return this;
    }


    @RequireOrganizationAccess
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getOrganizationApplications(
        @Context UriInfo ui,
        @QueryParam( "deleted" ) @DefaultValue( "false" ) Boolean deleted, // only return deleted apps if true
        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get organization application" );

        BiMap<UUID, String> applications = management.getApplicationsForOrganization( organization.getUuid() );
        response.setData( applications.inverse() );

        return response;
    }


    @RequireOrganizationAccess
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse newApplicationForOrganization( @Context UriInfo ui, Map<String, Object> json,
                                                          @QueryParam( "callback" ) @DefaultValue( "callback" )
                                                          String callback ) throws Exception {
        String applicationName = ( String ) json.get( "name" );
        return newApplicationForOrganizationFromForm( ui, json, callback, applicationName );
    }


    @RequireOrganizationAccess
    @POST
    @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse newApplicationForOrganizationFromForm( @Context UriInfo ui, Map<String, Object> json,
                                                                  @QueryParam( "callback" ) @DefaultValue( "callback" )
                                                                  String callback,
                                                                  @FormParam( "name" ) String applicationName )
            throws Exception {

        logger.debug("newApplicationForOrganizationFromForm");

        Preconditions.checkArgument( !isEmpty( applicationName ),
                "The 'name' parameter is required and cannot be empty: " + applicationName );

        ApiResponse response = createApiResponse();
        response.setAction( "new application for organization" );

        ApplicationInfo applicationInfo = management.createApplication( organization.getUuid(), applicationName );

        LinkedHashMap<String, UUID> applications = new LinkedHashMap<String, UUID>();
        applications.put( applicationInfo.getName(), applicationInfo.getId() );
        response.setData( applications );
        response.setResults( management.getApplicationMetadata( applicationInfo.getId() ) );
        return response;

    }


    @Path(RootResource.APPLICATION_ID_PATH)
    public ApplicationResource applicationFromOrganizationByApplicationId(
        @Context UriInfo ui, @PathParam( "applicationId" ) String applicationIdStr ) throws Exception {

        return getSubResource( ApplicationResource.class )
            .init(organization, UUID.fromString(applicationIdStr));
    }


    @Path( "{applicationName}" )
    public ApplicationResource applicationFromOrganizationByApplicationName(
        @Context UriInfo ui, @PathParam( "applicationName" ) String applicationName ) throws Exception {

        String appName =
                applicationName.contains( "/" ) ? applicationName : organization.getName() + "/" + applicationName;

        ApplicationInfo application = management.getApplicationInfo( appName );

        if ( application == null ) {
            throw new EntityNotFoundException(
                    String.format( "Application %s does not exist for organization %s", applicationName,
                            organization.getName() ) );
        }

        return getSubResource( ApplicationResource.class ).init( organization, application );
    }
}
