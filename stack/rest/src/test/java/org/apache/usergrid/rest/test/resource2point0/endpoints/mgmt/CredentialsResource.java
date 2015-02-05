package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;

import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Credentials;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import javax.ws.rs.core.MediaType;

/**
 */
public class CredentialsResource  extends NamedResource {

   public CredentialsResource( final ClientContext context, final UrlResource parent ) {
       super( "credentials", context, parent );
   }
    public Credentials get(final QueryParameters parameters, final boolean useToken){
        ApiResponse response = getResource(useToken).type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
            .get(ApiResponse.class);
        return new Credentials(response);
    }
    public Credentials get(final QueryParameters parameters){
        return get(parameters, true);
    }
    public Credentials get() {
        return get(null, true);
    }
}
