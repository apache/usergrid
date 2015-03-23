package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;


import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Handles /revokeToken endpoint ( as opposed to revokeTokens
 */
public class RevokeTokenResource extends NamedResource {
    public RevokeTokenResource( final ClientContext context, final UrlResource parent ) {
        super( "revoketoken", context, parent );
    }
}
