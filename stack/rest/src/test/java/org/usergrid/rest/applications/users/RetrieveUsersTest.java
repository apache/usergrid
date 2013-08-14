package org.usergrid.rest.applications.users;

import org.junit.Rule;
import org.junit.Test;
import org.usergrid.rest.AbstractRestIT;
import org.usergrid.rest.TestContextSetup;
import org.usergrid.rest.test.resource.CustomCollection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class RetrieveUsersTest extends AbstractRestIT {

  @Rule
  public TestContextSetup context = new TestContextSetup( this );

  @Test //USERGRID-1222
  public void queryForUsername() {
    CustomCollection users = context.collection("users");

    Map props = new HashMap();

    props.put("username","Alica");
    users.create(props);

    props.put("username","Bob");
    users.create(props);

    String query = "select *";
    String incorrectQuery = "select * where username = 'Alica'";

    assertEquals(users.entityValue(query,"username",0),users.entityValue(incorrectQuery,"username",0));
  }
}