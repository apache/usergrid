package org.apache.usergrid.rest.test.resource2point0.endpoints;


import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Created by ApigeeCorporation on 12/4/14.
 */
public class OrganizationResource extends NamedResource {


    public OrganizationResource( final String name, final ClientContext context, final UrlResource parent ) {
        super( name, context, parent );
    }

    public ApplicationResource getApp(final String app){
        return new ApplicationResource( app, this );
    }
}
