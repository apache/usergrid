package org.apache.usergrid.rest.test.resource2point0.endpoints;


import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Created by ApigeeCorporation on 12/4/14.
 */
public class ApplicationResource extends NamedResource {


    public ApplicationResource( final String name,final ClientContext context,  final UrlResource parent ) {
        super( name, context, parent );
    }

    public Collection users(){
        return new Collection("users", this);
    }
}
