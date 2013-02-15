package org.usergrid.rest.applications.queues;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import com.sun.jersey.api.client.UniformInterfaceException;

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

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    // now consume and make sure we get 100 each. We'll use the default for this
    // test first
    testMessages(queue, count, null);

  }

  /**
   * Tests that after delete, we can't receive messages
   */
  @Test
  public void delete() {
    TestAdminUser testAdmin = new TestAdminUser("queueresourcetest.delete", "queueresourcetest.delete@usergrid.com",
        "queueresourcetest.delete@usergrid.com");

    // create the text context
    TestContext context = TestContext.create(this).withOrg("queueresourcetest.delete").withApp("delete")
        .withUser(testAdmin).initAll();

    // TODO test
    Queue queue = context.application().queues().queue("test");

    try {
      queue.delete();
    } catch (UniformInterfaceException uie) {
      assertEquals(501, uie.getResponse().getClientResponseStatus().getStatusCode());
      return;
    }

    fail("I shouldn't get here");

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

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    // now consume and make sure we get 100 each. We'll use the default for this
    // test first
    testMessages(queue, count, "client1");
    testMessages(queue, count, "client2");

    // change back to client 1, and we shouldn't have anything
    // now consume and make sure we get 100 each. We'll use the default for this
    // test first
    queue = queue.withClientId("client1");

    JsonNode node = queue.getNextEntry();

    assertNull(node);

  }

  @Test
  public void subscribe() {
    TestAdminUser testAdmin = new TestAdminUser("queueresourcetest.subscribe",
        "queueresourcetest.subscribe@usergrid.com", "queueresourcetest.subscribe@usergrid.com");

    // create the text context
    TestContext context = TestContext.create(this).withOrg("queueresourcetest.subscribe").withApp("subscribe")
        .withUser(testAdmin).initAll();

    // TODO test
    Queue queue = context.application().queues().queue("test");

    queue.subscribers().subscribe("testsub1");
    queue.subscribers().subscribe("testsub2");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    testMessages(queue, count, null);

    // now consume and make sure we get 100 in the queue
    queue = context.application().queues().queue("testsub1");

    testMessages(queue, count, null);

    queue = context.application().queues().queue("testsub2");

    testMessages(queue, count, null);

  }

  /**
   * Tests that after unsubscribing, we don't continue to deliver messages to
   * other queues
   */
  @Test
  public void unsubscribe() {
    TestAdminUser testAdmin = new TestAdminUser("queueresourcetest.unsubscribe",
        "queueresourcetest.unsubscribe@usergrid.com", "queueresourcetest.unsubscribe@usergrid.com");

    // create the text context
    TestContext context = TestContext.create(this).withOrg("queueresourcetest.unsubscribe").withApp("unsubscribe")
        .withUser(testAdmin).initAll();

    // TODO test
    Queue queue = context.application().queues().queue("test");

    queue.subscribers().subscribe("testsub1");
    queue.subscribers().subscribe("testsub2");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    testMessages(queue, count, null);

    // now consume and make sure we get 100 in the queue
    queue = context.application().queues().queue("testsub1");

    testMessages(queue, count, null);

    queue = context.application().queues().queue("testsub2");

    testMessages(queue, count, null);

    // now unsubscribe the second queue
    queue = context.application().queues().queue("test");

    queue.subscribers().unsubscribe("testsub1");

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    testMessages(queue, count, null);

    // now consume and make sure we get 100 in the queue
    queue = context.application().queues().queue("testsub1");

    testMessages(queue, 0, null);

    queue = context.application().queues().queue("testsub2");

    testMessages(queue, count, null);

  }

  /**
   * Test that when receiving the messages from a queue, we receive the same
   * amount as "count". Starts from 0 to count-1 for message bodies. Client id
   * optional
   * 
   * @param queue
   * @param count
   * @param clientId
   */
  private void testMessages(Queue queue, int count, String clientId) {

    queue.withClientId(clientId);

    for (int i = 0; i < count; i++) {
      JsonNode entries = queue.getNextEntry();

      assertEquals(i, entries.get("id").asInt());

    }

    // get the next one, should be empty

    JsonNode node = queue.getNextEntry();

    assertNull(node);
  }

}
