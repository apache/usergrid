package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;


/**
 * Created by ApigeeCorporation on 12/9/14.
 */

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource2point0.endpoints.ApplicationResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * This is for the Management endpoint.
 * Holds the information required for building and chaining organization objects to applications.
 * Should also contain the GET,PUT,POST,DELETE methods of functioning in here.
 */
public class OrganizationResource extends NamedResource {

//TODO: need to find a way to integrate having the orgs/<org_name> into the same endpoint.
    //maybe I could append the orgs to the end of the parent
    public OrganizationResource( final String name, final ClientContext context, final UrlResource parent ) {
        super( name, context, parent );
    }

    //TODO: change this so that it reflects the management endpoint
//    public ApplicationResource getApp(final String app){
//        return new ApplicationResource( app, context ,this );
//    }

//    public ApiResponse post(Map<String,String> organization){
//
//        return getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
//                     .post( ApiResponse.class, organization );
//    }

}
