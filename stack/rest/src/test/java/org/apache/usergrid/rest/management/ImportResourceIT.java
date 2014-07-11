package org.apache.usergrid.rest.management;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.AbstractRestIT;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
public class ImportResourceIT extends AbstractRestIT {

    public ImportResourceIT() throws Exception {

    }

    @Test
    public void importCallSuccessful() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        JsonNode node = null;

        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.OK, responseStatus );
    }

    @Test
    public void importCollectionUUIDRetTest() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.ACCEPTED;
        JsonNode node = null;

        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.ACCEPTED, responseStatus );
        assertNotNull( node.get( "Import Entity" ) );
    }

    @Test
    public void importApplicationUUIDRetTest() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.ACCEPTED;
        JsonNode node = null;


        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.ACCEPTED, responseStatus );
        assertNotNull( node.get( "Import Entity" ) );
    }

    @Test
    public void importOrganizationUUIDRetTest() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.ACCEPTED;
        JsonNode node = null;


        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.ACCEPTED, responseStatus );
        assertNotNull( node.get( "Import Entity" ) );

    }

    /*Creates fake payload for testing purposes.*/
    public HashMap<String, Object> payloadBuilder() {
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        //TODO: always put dummy values here and ignore this test.
        //TODO: add a ret for when s3 values are invalid.
        storage_info.put( "s3_key", "insert key here" );
        storage_info.put( "s3_access_id", "insert access id here" );
        storage_info.put( "bucket_location", "insert bucket name here" );
        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );
        payload.put( "properties", properties );
        return payload;
    }
}
