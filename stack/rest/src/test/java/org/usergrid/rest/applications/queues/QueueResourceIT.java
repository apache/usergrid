package org.usergrid.rest.applications.queues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.mq.QueuePosition;
import org.usergrid.rest.TestContextSetup;
import org.usergrid.rest.test.resource.app.queue.Queue;
import org.usergrid.rest.test.resource.app.queue.Transaction;
import org.usergrid.utils.MapUtils;

import com.google.common.collect.BiMap;
import com.sun.jersey.api.client.UniformInterfaceException;

@Concurrent()
public class QueueResourceIT extends AbstractQueueResourceIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );

  @Test
  public void inOrder() {

    Queue queue = context.application().queues().queue("test");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    IncrementHandler handler = new IncrementHandler(count);
    // now consume and make sure we get each message. We'll use the default for
    // this
    // test first
    testMessages(queue, handler, new NoLastCommand());

    handler.assertResults();

  }

  @Test
  public void inOrderPaging() {
    Queue queue = context.application().queues().queue("test");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    queue = queue.withLimit(15);

    IncrementHandler handler = new IncrementHandler(count);

    // now consume and make sure we get each message. We'll use the default for
    // this
    // test first
    testMessages(queue, handler, new NoLastCommand());

    handler.assertResults();

  }

  /**
   * Read all messages with the client, then re-issue the reads from the start
   * position to test we do this properly
   */
  @Test
  public void startPaging() {

    Queue queue = context.application().queues().queue("test");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    queue = queue.withLimit(15);

    // now consume and make sure we get each message. We'll use the default for
    // this test first
    IncrementHandler handler = new IncrementHandler(count);

    testMessages(queue, handler, new NoLastCommand());
    handler.assertResults();

    queue = queue.withPosition(QueuePosition.START.name()).withLast(null);

    // now test it again, we should get same results when we explicitly read
    // from start and pass back the last
    handler = new IncrementHandler(count);
    testMessages(queue, handler);
    handler.assertResults();

  }

  @Test
  public void reverseOrderPaging() {

    Queue queue = context.application().queues().queue("test");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    queue = queue.withLimit(15);

    IncrementHandler handler = new IncrementHandler(count);

    testMessages(queue, handler);
    handler.assertResults();

    DecrementHandler decrement = new DecrementHandler(30);

    queue = queue.withLimit(15).withPosition(QueuePosition.END.name()).withLast(null);

    testMessages(queue, decrement);
    decrement.assertResults();

  }

  /**
   * Tests that after delete, we can't receive messages
   */
  @Test
  public void delete() {

    Queue queue = context.application().queues().queue("test");

    try {
      queue.delete();
    } catch (UniformInterfaceException uie) {
      assertEquals(501, uie.getResponse().getClientResponseStatus().getStatusCode());
      return;
    }

    fail("I shouldn't get here");

  }

  /**
   * Read messages ad-hoc with filtering
   */
  @Test
  @Ignore("Currently unsupported.  Needs fixed with iterators")
  public void filterForward() {

    Queue queue = context.application().queues().queue("test");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("name", "todd");
      data.put("id", i);
      data.put("indexed", true);

      queue.post(data);
    }

    queue = queue.withLimit(1).withPosition(QueuePosition.START.name())
        .withFilters("name = 'todd'", "id >= 10", "id <= 20").withLast(null);

    // test it the first time, we should match
    ForwardMatchHandler handler = new ForwardMatchHandler(10, 10);
    testMessages(queue, handler);
    handler.assertResults();

    // test it again, shoudl still match
    handler = new ForwardMatchHandler(10, 10);
    testMessages(queue, handler);
    handler.assertResults();

  }

  /**
   * Read messages ad-hoc with filtering
   */
  @Test
  @Ignore("Currently unsupported.  Needs fixed with iterators")
  public void filterReverse() {

    Queue queue = context.application().queues().queue("test");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("name", "todd").map("id", String.valueOf(i)).map("indexed", "true"));
    }

    queue = queue.withLimit(1).withPosition(QueuePosition.END.name())
        .withFilters("name = 'todd'", "id >= 20", "id <= 30").withLast(null);

    // test it the first time, we should match
    ReverseMatchHandler handler = new ReverseMatchHandler(30, 10);
    testMessages(queue, handler);
    handler.assertResults();

    // test it again, shoudl still match
    handler = new ReverseMatchHandler(10, 10);
    testMessages(queue, handler);
    handler.assertResults();

  }

  @Test
  public void topic() {

    Queue queue = context.application().queues().queue("test");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    // now consume and make sure we get each message. We'll use the default for
    // this
    // test first

    IncrementHandler handler = new IncrementHandler(count);
    testMessages(queue, handler, new ClientId("client1"), new NoLastCommand());
    handler.assertResults();

    handler = new IncrementHandler(count);
    testMessages(queue, handler, new ClientId("client2"), new NoLastCommand());
    handler.assertResults();

    // change back to client 1, and we shouldn't have anything
    // now consume and make sure we get each message. We'll use the default for
    // this
    // test first
    queue = queue.withClientId("client1");

    JsonNode node = queue.getNextEntry();

    assertNull(node);

  }

  @Test
  public void subscribe() {

    Queue queue = context.application().queues().queue("test");

    queue.subscribers().subscribe("testsub1");
    queue.subscribers().subscribe("testsub2");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    IncrementHandler handler = new IncrementHandler(count);

    testMessages(queue, handler, new NoLastCommand());

    handler.assertResults();

    // now consume and make sure we get messages in the queue
    queue = context.application().queues().queue("testsub1");

    handler = new IncrementHandler(count);

    testMessages(queue, handler, new NoLastCommand());

    handler.assertResults();

    handler = new IncrementHandler(count);

    queue = context.application().queues().queue("testsub2");

    testMessages(queue, handler, new NoLastCommand());

    handler.assertResults();

  }

  /**
   * Tests that after unsubscribing, we don't continue to deliver messages to
   * other queues
   */
  @Test
  public void unsubscribe() {

    Queue queue = context.application().queues().queue("test");

    queue.subscribers().subscribe("testsub1");
    queue.subscribers().subscribe("testsub2");

    final int count = 30;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    IncrementHandler handler = new IncrementHandler(count);

    testMessages(queue, handler, new NoLastCommand());

    handler.assertResults();

    handler = new IncrementHandler(count);

    // now consume and make sure we get messages in the queue
    queue = context.application().queues().queue("testsub1");

    testMessages(queue, handler, new NoLastCommand());
    handler.assertResults();

    handler = new IncrementHandler(count);

    queue = context.application().queues().queue("testsub2");

    testMessages(queue, handler, new NoLastCommand());
    handler.assertResults();

    // now unsubscribe the second queue
    queue = context.application().queues().queue("test");

    queue.subscribers().unsubscribe("testsub1");

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    handler = new IncrementHandler(count);

    testMessages(queue, handler, new NoLastCommand());
    handler.assertResults();

    // now consume and make sure we don't have messages in the ququq
    queue = context.application().queues().queue("testsub1");

    handler = new IncrementHandler(0);

    testMessages(queue, handler, new NoLastCommand());

    handler.assertResults();

    queue = context.application().queues().queue("testsub2");

    handler = new IncrementHandler(count);

    testMessages(queue, handler, new NoLastCommand());

    handler.assertResults();

  }

  @Test
  public void transactionTimeout() throws InterruptedException {

    Queue queue = context.application().queues().queue("test");

    final int count = 2;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    // now consume and make sure we get each message. We should receive each
    // message, and we'll use this for comparing results later
    final long timeout = 5000;

    queue = queue.withTimeout(timeout);

    TransactionResponseHandler transHandler = new TransactionResponseHandler(count);

    testMessages(queue, transHandler, new NoLastCommand());

    long start = System.currentTimeMillis();

    transHandler.assertResults();

    List<String> originalMessageIds = transHandler.getMessageIds();
    BiMap<String, String> transactionInfo = transHandler.getTransactionToMessageId();

    // now read again, we shouldn't have any results because our timeout hasn't
    // lapsed
    IncrementHandler incrementHandler = new IncrementHandler(0);

    testMessages(queue, incrementHandler, new NoLastCommand());

    incrementHandler.assertResults();

    // now sleep until our timeout expires
    Thread.sleep(timeout - (System.currentTimeMillis() - start));

    // now re-read our messages, we should get them all again
    transHandler = new TransactionResponseHandler(count);

    testMessages(queue, transHandler, new NoLastCommand());

    start = System.currentTimeMillis();

    transHandler.assertResults();

    List<String> returned = transHandler.getMessageIds();

    assertTrue(returned.size() > 0);

    // compare the replayed messages and the make sure they're in the same order
    BiMap<String, String> newTransactions = transHandler.getTransactionToMessageId();

    for (int i = 0; i < originalMessageIds.size(); i++) {
      // check the messages come back in the same order, they should
      assertEquals(originalMessageIds.get(i), returned.get(i));

      assertNotNull(transactionInfo.get(originalMessageIds.get(i)));

    }

    // sleep again before testing a second timeout
    Thread.sleep(timeout - (System.currentTimeMillis() - start));
    // now re-read our messages, we should get them all again
    transHandler = new TransactionResponseHandler(count);

    testMessages(queue, transHandler, new NoLastCommand());

    start = System.currentTimeMillis();

    transHandler.assertResults();

    returned = transHandler.getMessageIds();

    assertTrue(returned.size() > 0);

    // compare the replayed messages and the make sure they're in the same order
    newTransactions = transHandler.getTransactionToMessageId();

    for (int i = 0; i < originalMessageIds.size(); i++) {
      // check the messages come back in the same order, they should
      assertEquals(originalMessageIds.get(i), returned.get(i));

      assertNotNull(transactionInfo.get(originalMessageIds.get(i)));

      // ack the transaction we were returned
      Transaction transaction = queue.transactions().transaction(newTransactions.get(originalMessageIds.get(i)));
      transaction.delete();

    }

    // now sleep again we shouldn't have any messages since we acked all the
    // transactions
    Thread.sleep(timeout - (System.currentTimeMillis() - start));

    incrementHandler = new IncrementHandler(0);

    testMessages(queue, incrementHandler, new NoLastCommand());

    incrementHandler.assertResults();

  }

  @Test
  public void transactionPageSize() throws InterruptedException {

    Queue queue = context.application().queues().queue("test");

    final int count = 100;

    @SuppressWarnings("unchecked")
    Map<String, ?>[] data = new Map[count];

    for (int i = 0; i < count; i++) {
      data[i] = MapUtils.hashMap("id", i);

    }

    queue.post(data);

    // now consume and make sure we get each message. We should receive each
    // message, and we'll use this for comparing results later
    final long timeout = 20000;

    // read 50 messages at a time
    queue = queue.withTimeout(timeout).withLimit(50);

    TransactionResponseHandler transHandler = new TransactionResponseHandler(count);

    testMessages(queue, transHandler, new NoLastCommand());

    long start = System.currentTimeMillis();

    transHandler.assertResults();

    List<String> originalMessageIds = transHandler.getMessageIds();
    BiMap<String, String> transactionInfo = transHandler.getTransactionToMessageId();

    for (int i = 0; i < originalMessageIds.size(); i++) {
      // check the messages come back in the same order, they should
      assertEquals(originalMessageIds.get(i), originalMessageIds.get(i));

      assertNotNull(transactionInfo.get(originalMessageIds.get(i)));

      // ack the transaction we were returned
      Transaction transaction = queue.transactions().transaction(transactionInfo.get(originalMessageIds.get(i)));
      transaction.delete();

    }

    // now sleep until our timeout expires
    Thread.sleep(Math.max(0, timeout - (System.currentTimeMillis() - start)));

    IncrementHandler incrementHandler = new IncrementHandler(0);

    testMessages(queue, incrementHandler, new NoLastCommand());

    incrementHandler.assertResults();

  }

  /**
   * Tests that if we page before transaction expiration, we're always getting
   * all elements consecutively
   * 
   * @throws InterruptedException
   */
  @Test
  public void transactionPageConsistent() throws InterruptedException {

    Queue queue = context.application().queues().queue("test");

    final int count = 1000;

    @SuppressWarnings("unchecked")
    Map<String, ?>[] data = new Map[count];

    for (int i = 0; i < count; i++) {
      data[i] = MapUtils.hashMap("id", i);

    }

    queue.post(data);

    // now consume and make sure we get each message. We should receive each
    // message, and we'll use this for comparing results later
    final long timeout = 20000;

    // read 50 messages at a time
    queue = queue.withTimeout(timeout).withLimit(120);

    IncrementHandler incrementHandler = new IncrementHandler(count);

    testMessages(queue, incrementHandler, new NoLastCommand());

    incrementHandler.assertResults();
  }

  @Test
  public void transaction10KMax() throws InterruptedException {

    Queue queue = context.application().queues().queue("test");
    queue.post(MapUtils.hashMap("id", 0));

    queue = queue.withTimeout(10000).withLimit(10001);

    try {
      queue.getNextPage();
    } catch (UniformInterfaceException uie) {

      return;
    }

    fail("An exception should be thrown");

  }

  @Test
  public void transactionRenewal() throws InterruptedException {
    Queue queue = context.application().queues().queue("test");

    final int count = 2;

    for (int i = 0; i < count; i++) {
      queue.post(MapUtils.hashMap("id", i));
    }

    // now consume and make sure we get each message. We should receive each
    // message, and we'll use this for comparing results later
    final long timeout = 5000;

    queue = queue.withTimeout(timeout);

    TransactionResponseHandler transHandler = new TransactionResponseHandler(count);

    testMessages(queue, transHandler, new NoLastCommand());

    long start = System.currentTimeMillis();

    transHandler.assertResults();

    List<String> originalMessageIds = transHandler.getMessageIds();
    BiMap<String, String> transactionInfo = transHandler.getTransactionToMessageId();

    // now read again, we shouldn't have any results because our timeout hasn't
    // lapsed
    IncrementHandler incrementHandler = new IncrementHandler(0);

    testMessages(queue, incrementHandler, new NoLastCommand());

    incrementHandler.assertResults();

    // now sleep until our timeout expires
    Thread.sleep(timeout - (System.currentTimeMillis() - start));

    // renew the transactions, then read. We shouldn't get any messages.
    List<String> returned = transHandler.getMessageIds();

    assertTrue(returned.size() > 0);

    // compare the replayed messages and the make sure they're in the same order
    BiMap<String, String> newTransactions = transHandler.getTransactionToMessageId();

    for (int i = 0; i < originalMessageIds.size(); i++) {
      // check the messages come back in the same order, they should
      assertEquals(originalMessageIds.get(i), returned.get(i));

      assertNotNull(transactionInfo.get(originalMessageIds.get(i)));

      // ack the transaction we were returned
      Transaction transaction = queue.transactions().transaction(newTransactions.get(originalMessageIds.get(i)));
      transaction.renew(timeout);

    }

    // now re-read our messages, we should not get any
    incrementHandler = new IncrementHandler(0);
    testMessages(queue, incrementHandler, new NoLastCommand());

    incrementHandler.assertResults();

    start = System.currentTimeMillis();

    // sleep again before testing the transactions time out (since we're not
    // renewing them)
    Thread.sleep(timeout - (System.currentTimeMillis() - start));

    // now re-read our messages, we should get them all again
    transHandler = new TransactionResponseHandler(count);

    testMessages(queue, transHandler, new NoLastCommand());

    start = System.currentTimeMillis();

    transHandler.assertResults();

    returned = transHandler.getMessageIds();

    assertTrue(returned.size() > 0);

    // compare the replayed messages and the make sure they're in the same order
    newTransactions = transHandler.getTransactionToMessageId();

    for (int i = 0; i < originalMessageIds.size(); i++) {
      // check the messages come back in the same order, they should
      assertEquals(originalMessageIds.get(i), returned.get(i));

      assertNotNull(transactionInfo.get(originalMessageIds.get(i)));

      // ack the transaction we were returned
      Transaction transaction = queue.transactions().transaction(newTransactions.get(originalMessageIds.get(i)));
      transaction.delete();

    }

    // now sleep again we shouldn't have any messages since we acked all the
    // transactions
    Thread.sleep(timeout - (System.currentTimeMillis() - start));

    incrementHandler = new IncrementHandler(0);

    testMessages(queue, incrementHandler, new NoLastCommand());

    incrementHandler.assertResults();

  }

  @Test
  @Ignore("This is caused by timeuuids getting generated out of order within a millisecond.  Disabling until the timeuuid issue is resolved next sprint.  For job scheduling, this is not an issue")
  public void concurrentConsumers() throws InterruptedException, ExecutionException {

    int consumerSize = 8;
    int count = 10000;
    int batchsize = 100;

    ExecutorService executor = Executors.newFixedThreadPool(consumerSize);

    Queue queue = context.application().queues().queue("test");

    // post the messages in batch
    for (int i = 0; i < count / batchsize; i++) {

      @SuppressWarnings("unchecked")
      Map<String, ?>[] elements = new Map[batchsize];

      for (int j = 0; j < batchsize; j++) {
        elements[j] = MapUtils.hashMap("id", i * batchsize + j);
      }

      queue.post(elements);
    }

    // now consume and make sure we get each message. We should receive each
    // message, and we'll use this for comparing results later
    final long timeout = 60000;

    // set our timeout and read 10 messages at a time
    queue = queue.withTimeout(timeout).withLimit(10);

    AsyncTransactionResponseHandler transHandler = new AsyncTransactionResponseHandler(count);

    NoLastCommand command = new NoLastCommand();

    List<Future<Void>> futures = new ArrayList<Future<Void>>(consumerSize);

    for (int i = 0; i < consumerSize; i++) {
      Future<Void> future = executor.submit(new QueueClient(queue, transHandler, command));

      futures.add(future);
    }

    // wait for tests to finish
    for (Future<Void> future : futures) {
      future.get();
    }

    // now assert we're good.

    transHandler.assertResults();

  }
}
