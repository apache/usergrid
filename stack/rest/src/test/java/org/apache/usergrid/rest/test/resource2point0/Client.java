package org.apache.usergrid.rest.test.resource2point0;


import org.apache.usergrid.rest.test.resource2point0.endpoints.Collection;
import org.apache.usergrid.rest.test.resource2point0.endpoints.ManagementResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.OrganizationResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.RootResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.EntityResponse;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Created by ApigeeCorporation on 12/4/14.
 */
public class Client implements UrlResource {


    private final String serverUrl;
    private final ClientContext context;
//This should work independantly of test frameowkr. Need to be able to pull the WebResource Url and not have to integrate the webresource into the Endpoints/Client.
    //This uses jeresy to create the client. Initialize the client with the webresource, and then the CLIENT calls the root resource.
    //
    //after initialization of the client htne use it to build our path using our resources.
    //Just keep checking in early and checkin often.

    public Client( final String serverUrl ) {
        this.serverUrl = serverUrl;
        this.context = new ClientContext();
    }


    @Override
    public String getPath() {
        return serverUrl;
    }


    /**
     * Get the management resource
     */
    public ManagementResource management() {
        return new ManagementResource( context, this );
    }


    /**
     * Get hte organization resource
     */
    public OrganizationResource org( final String orgName ) {
        return new OrganizationResource( orgName, context,  this );
    }


    public void loginAdminUser( final String username, final String password ) {
        final String token = management().token().post(username, password);

        context.setToken( token );
    }
}
