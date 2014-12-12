package org.apache.usergrid.rest.test.resource2point0.endpoints;


import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.sun.jersey.api.client.WebResource;


/**
 * Class that would hold all the rest calls that are duplicated ( such as posts, or puts where all we have to do
 * is send a map through jersey.
 */
public class RestResource extends NamedResource {

    //TODO: wouldn't need a name, so maybe it isn't a NamedResource but just a URL Resource.
    public RestResource( final ClientContext context, final UrlResource parent ) {
        super("name",context, parent );
    }

    public ApiResponse post(){
        return null;
    }

    public ApiResponse put(){
        return null;

    }

    public ApiResponse get(){
        return null;

    }

    public ApiResponse delete(){
        return null;

    }



}
