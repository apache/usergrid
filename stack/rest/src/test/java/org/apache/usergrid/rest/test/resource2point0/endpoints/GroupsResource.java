package org.apache.usergrid.rest.test.resource2point0.endpoints;

import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;
import org.apache.usergrid.rest.test.resource2point0.model.Group;

import javax.ws.rs.core.MediaType;

/**
 * Created by rockerston on 12/16/14.
 */
public class GroupsResource extends AbstractCollectionResource<Group,GroupResource> {

    public GroupsResource( ClientContext context, UrlResource parent) {
        super("groups", context, parent);
    }

    @Override
    protected Group instantiateT(ApiResponse response) {
        return new Group(response);
    }

    @Override
    protected GroupResource instantiateK(String name, ClientContext context, UrlResource parent) {
        return new GroupResource(name,context,parent);
    }


}
