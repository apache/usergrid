package org.apache.usergrid.rest.test.resource2point0.endpoints;


import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Root resource for stuff
 */
public class RootResource implements UrlResource {


    private final String serverUrl;
    private final ClientContext context;


    public RootResource( final String serverUrl, final ClientContext context ) {this.serverUrl = serverUrl;
        this.context = context;
    }


    @Override
    public String getPath() {
        return serverUrl;
    }


    /**
     * Get the management resource
     * @return
     */
    public ManagementResource management(){
        return new ManagementResource( context, this);
    }


    /**
     * Get hte organization resource
     * @param orgName
     * @return
     */
    public OrganizationResource  org(final String orgName){
        return new OrganizationResource( orgName,context,  this );
    }
}
