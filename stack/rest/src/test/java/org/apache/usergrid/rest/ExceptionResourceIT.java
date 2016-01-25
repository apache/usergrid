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
import org.apache.usergrid.services.exceptions.UnsupportedServiceOperationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ExceptionResourceIT extends AbstractRestIT{

    @Test
    public void testNonExistingEndpoint(){
        try {

            clientSetup.getRestClient()
                       .pathResource( getOrgAppPath( "non_existant_delete_endpoint" ) ).delete( );
            fail("Should have thrown below exception");

        }catch(NotAllowedException e){
            assertEquals( 405,e.getResponse().getStatus());
        }
    }

    //test uncovered endpoints
    @Test
    public void testNotImplementedException(){
        try {

            clientSetup.getRestClient().management().orgs().delete( true );
            fail("Should have thrown below exception");

        }catch(NotAllowedException e){
            assertEquals( 405,e.getResponse().getStatus());
        }
    }
    @Test
    public void testDeleteFromWrongEndpoint(){
        try {
            clientSetup.getRestClient()
                       .pathResource( clientSetup.getOrganizationName() + "/" + clientSetup.getAppName()  ).delete( );
            fail("Should have thrown below exception");

        }catch(NotAllowedException e){
            assertEquals( 405,e.getResponse().getStatus());
        }
    }

    @Test
    public void testUnsupportedServiceOperation(){
        try {
            clientSetup.getRestClient()
                       .pathResource( getOrgAppPath( "users" ) ).delete( );
            fail("Should have thrown below exception");

        }catch(NotAllowedException e){
            assertEquals( 405,e.getResponse().getStatus());
        }
    }

}
