package org.apache.usergrid.rest.test.resource2point0.endpoints;


import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Created by ApigeeCorporation on 12/4/14.
 */
public class NamedResource implements UrlResource {

    protected final String name;
    protected final ClientContext context;
    protected final UrlResource parent;


    public NamedResource( final String name, final ClientContext context, final UrlResource parent ) {this.name = name;
        this.context = context;
        this.parent = parent;
    }


    @Override
    public String getPath() {
        return name;
    }
}
