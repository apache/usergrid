package org.apache.usergrid.rest.test.resource2point0.endpoints;


import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Created by ApigeeCorporation on 12/4/14.
 */
public class ManagementResource extends NamedResource {
    public ManagementResource( final ClientContext context, final UrlResource parent ) {
        super( "management", context, parent );
    }

    public TokenResource token(){
        return new TokenResource( context, this );
    }

}
