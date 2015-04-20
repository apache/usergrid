package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;

import com.sun.jersey.api.client.WebResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import javax.ws.rs.core.MediaType;

/**
 * Created by ApigeeCorporation on 4/7/15.
 */
public class DeactivateResource extends NamedResource<DeactivateResource> {
    public DeactivateResource(final ClientContext context, final UrlResource parent) {
        super("deactivate", context, parent);
    }

    @Override
    protected DeactivateResource getThis() {
        return this;
    }
}
