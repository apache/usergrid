package org.apache.usergrid.rest.test.resource2point0.endpoints;

import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Group;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

/**
 * Created by rockerston on 12/16/14.
 */
public class GroupResource extends AbstractEntityResource<Group>  {
    public GroupResource(String name, ClientContext context, UrlResource parent) {
        super(name, context, parent);
    }

    @Override
    protected Group instantiateT(ApiResponse response) {
        return new Group(response);
    }
}