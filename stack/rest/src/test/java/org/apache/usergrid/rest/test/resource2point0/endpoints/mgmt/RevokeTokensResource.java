package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;

import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

/**
 * Created by ApigeeCorporation on 4/7/15.
 */
public class RevokeTokensResource extends NamedResource<RevokeTokensResource> {
    public RevokeTokensResource(final ClientContext context, final UrlResource parent) {
        super("revoketokens", context, parent);
    }

    @Override
    protected RevokeTokensResource getThis() {
        return this;
    }
}
