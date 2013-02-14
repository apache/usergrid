package org.usergrid.rest.applications.queues;

import static org.junit.Assert.*;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.rest.test.resource.TestContext;
import org.usergrid.rest.test.resource.user.Queue;
import org.usergrid.rest.test.security.TestAdminUser;
import org.usergrid.utils.MapUtils;

public class QueueResourceTest extends AbstractRestTest {

  
  
  
  @Test
  public void inOrder() {
    
    TestAdminUser testAdmin = new TestAdminUser("queueresourcetest.inorder", "queueresourcetest.inorder@usergrid.com",
        "queueresourcetest.inorder@usergrid.com");
    

    // create the text context
    TestContext context = TestContext.create(this).withOrg("queueresourcetest.inorder").withApp("testInOrder")
        .withUser(testAdmin).initAll();

    // TODO test
    Queue queue = context.application().queues().queue("test");

    final int count = 100;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    // now consume and make sure we get 100 each. We'll use the default for this
    // test first

    for (int i = 0; i < count; i++) {
      JsonNode entries = queue.getNextEntry();

      assertEquals(i, entries.get("id").asInt());

    }

    // get the next one, should be empty

    JsonNode node = queue.getNextEntry();

    assertNull(node);

  }

  @Test
  public void topic() {
    TestAdminUser testAdmin = new TestAdminUser("queueresourcetest.topic", "queueresourcetest.topic@usergrid.com",
        "queueresourcetest.topic@usergrid.com");
    

    // create the text context
    TestContext context = TestContext.create(this).withOrg("queueresourcetest.topic").withApp("topic")
        .withUser(testAdmin).initAll();

    // TODO test
    Queue queue = context.application().queues().queue("test");

    final int count = 100;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    // now consume and make sure we get 100 each. We'll use the default for this
    // test first
    queue = queue.withClientId("client1");

    for (int i = 0; i < count; i++) {
      JsonNode entries = queue.getNextEntry();

      assertEquals(i, entries.get("id").asInt());

    }

    // get the next one, should be empty

    JsonNode node = queue.getNextEntry();

    assertNull(node);

    // now change the client id and we should get 100 again
    queue = queue.withClientId("client2");

    for (int i = 0; i < count; i++) {
      JsonNode entries = queue.getNextEntry();

      assertEquals(i, entries.get("id").asInt());

    }

    // get the next one, should be empty

    node = queue.getNextEntry();

    assertNull(node);

    // change back to client 1, and we shouldn't have anything
    // now consume and make sure we get 100 each. We'll use the default for this
    // test first
    queue = queue.withClientId("client1");

    node = queue.getNextEntry();

    assertNull(node);

  }
}
