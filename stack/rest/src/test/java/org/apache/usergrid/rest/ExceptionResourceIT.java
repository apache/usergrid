package org.apache.usergrid.rest;


import java.util.Map;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ServerErrorException;

import org.junit.Test;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Organization;
import org.apache.usergrid.rest.test.resource.model.Token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Created by ApigeeCorporation on 1/5/16.
 */
public class ExceptionResourceIT extends AbstractRestIT{

    @Test
    public void testNonExistingEndpoint(){
        try {

            clientSetup.getRestClient()
                       .pathResource( getOrgAppPath( "non_existant_delete_endpoint" ) ).delete( );
        }catch(NotAllowedException e){
           // assertNotNull(e);
            assertEquals( 405,e.getResponse().getStatus());
        }
    }

    //test uncovered endpoints
    @Test
    public void testNotImplementedException(){
        try {

            clientSetup.getRestClient().management().orgs().delete( true );

        }catch(NotAllowedException e){
            // assertNotNull(e);
            assertEquals( 405,e.getResponse().getStatus());
        }
    }
    @Test
    public void testDeleteFromWrongEndpoint(){
        try {
            clientSetup.getRestClient()
                       .pathResource( clientSetup.getOrganizationName() + "/" + clientSetup.getAppName()  ).delete( );


        }catch(NotAllowedException e){
            // assertNotNull(e);
            assertEquals( 405,e.getResponse().getStatus());
        }
    }



    public Organization createOrgPayload(String baseName,Map properties ){
        String orgName = baseName + UUIDUtils.newTimeUUID();
        return new Organization( orgName+ UUIDUtils.newTimeUUID(),orgName,orgName+"@usergrid",orgName,orgName, properties);
    }
}
