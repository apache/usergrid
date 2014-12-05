package org.apache.usergrid.rest.test.resource2point0;


import org.apache.usergrid.rest.test.resource2point0.endpoints.Collection;
import org.apache.usergrid.rest.test.resource2point0.endpoints.RootResource;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.EntityResponse;


/**
 * Created by ApigeeCorporation on 12/4/14.
 */
public class DumbClient {

    private final Client client = new Client("http://localhost:8080");

    public void stuff(){
        EntityResponse itr  =  client.org( "test" ).getApp( "test" ).users().getEntityResponse();

        for(Entity entity: itr){

        }
    }

    public void stateful(){



        EntityResponse itr  =  client.org( "test" ).getApp( "test" ).users().getEntityResponse();

        for(Entity entity: itr){

        }
    }
}
