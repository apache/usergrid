package org.apache.usergrid.rest.test.resource2point0.endpoints;

import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Role;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

/**
 * Created by rockerston on 12/16/14.
 */
public class RoleResource  extends AbstractEntityResource<Role>  {
    public RoleResource(String name, ClientContext context, UrlResource parent) {
        super(name, context, parent);
    }

    @Override
    protected Role instantiateT(ApiResponse response) {
        return new Role(response);
    }
}
