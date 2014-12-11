package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;


import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Manages the Management/ORG endpoint.
 */
public class OrgResource  extends NamedResource {

    //TODO: need to find a way to integrate having the orgs/<org_name> into the same endpoint.
    //maybe I could append the orgs to the end of the parent
    public OrgResource( final ClientContext context, final UrlResource parent ) {
        super( "orgs", context, parent );
    }

    //TODO: change this so that it reflects the management endpoint
    //    public ApplicationResource getApp(final String app){
    //        return new ApplicationResource( app, context ,this );
    //    }

    public OrganizationResource organization (final String orgname){
        return new OrganizationResource( orgname,context,parent );
    }

    //TODO: why take in a map? Just use base resource and call post from there,
    //TODO: Why ApiResponse when we could just type what we expect back.
    public ApiResponse post(Map<String,String> organization){

        return getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                            .post( ApiResponse.class, organization );
    }

}
