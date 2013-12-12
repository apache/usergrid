package org.usergrid.rest.applications.collection;


import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.junit.Rule;
import org.junit.Test;
import org.usergrid.rest.AbstractRestIT;
import org.usergrid.rest.TestContextSetup;
import org.usergrid.rest.test.resource.CustomCollection;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.usergrid.utils.MapUtils.hashMap;


/**
 * @author tnine
 */
public class BadGrammarQueryTest extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void catchBadQueryGrammar() {

        CustomCollection things = context.collection( "things" );

        Map actor = hashMap( "displayName", "Erin" );
        Map props = new HashMap();
        props.put( "actor", actor );
        props.put( "content", "bragh" );

        JsonNode activity = things.create( props );

        String query = "select * where name != 'go'";

        ClientResponse.Status status = null;

        try {

            JsonNode incorrectNode = things.query( query, "limit", Integer.toString( 10 ) );
            fail( "This should throw an exception" );
        }
        catch ( UniformInterfaceException uie ) {
             status = uie.getResponse().getClientResponseStatus();


        }

        assertEquals( 400, status.getStatusCode() );
    }
}
