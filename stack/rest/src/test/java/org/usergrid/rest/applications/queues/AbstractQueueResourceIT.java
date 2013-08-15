package org.usergrid.rest.applications.queues;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.AbstractRestIT;
import org.usergrid.rest.test.resource.app.queue.Queue;

import java.util.*;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;


public class AbstractQueueResourceIT extends AbstractRestIT {
    /**
     * Commands for editing queue information
     *
     * @author tnine
     *
     */
    protected interface QueueCommand {

      /**
       * Perform any modifications on the queue and return it
       *
       * @param queue
       * @return
       */
      public Queue processQueue(Queue queue);
    }

    /**
     * Interface for handling responses from the queue (per message)
     *
     * @author tnine
     *
     */
    protected interface ResponseHandler {
      /**
       * Do something with the response
       *
       * @param node
       */
      public void response(JsonNode node);

      /**
       * Validate the results are correct
       */
      public void assertResults();
    }

    protected class QueueClient implements Callable<Void> {

      private ResponseHandler handler;
      private QueueCommand[] commands;
      private Queue queue;

      protected QueueClient(Queue queue, ResponseHandler handler, QueueCommand... commands) {
        this.queue = queue;
        this.handler = handler;
        this.commands = commands;
      }

      /*
       * (non-Javadoc)
       *
       * @see java.util.concurrent.Callable#call()
       */
      @Override
      public Void call() throws Exception {
        List<JsonNode> entries = null;

        do {
          for (QueueCommand command : commands) {
            queue = command.processQueue(queue);
          }

          entries = queue.getNextPage();

          for (JsonNode entry : entries) {
            handler.response(entry);
          }

        } while (entries.size() > 0);

        return null;
      }

    }

    protected class NoLastCommand implements QueueCommand {

      /*
       * (non-Javadoc)
       *
       * @see
       * org.usergrid.rest.applications.queues.QueueResourceIT.QueueCommand#
       * processQueue(org.usergrid.rest.test.resource.app.Queue)
       */
      @Override
      public Queue processQueue(Queue queue) {
        return queue.withLast(null);
      }

    }

    protected class ClientId implements QueueCommand {

      private String clientId;

      public ClientId(String clientId) {
        this.clientId = clientId;
      }

      /*
       * (non-Javadoc)
       *
       * @see
       * org.usergrid.rest.applications.queues.QueueResourceIT.QueueCommand#
       * processQueue(org.usergrid.rest.test.resource.app.Queue)
       */
      @Override
      public Queue processQueue(Queue queue) {
        return queue.withClientId(clientId);
      }

    }

    /**
     * Simple handler ensure we get up to count messages
     *
     * @author tnine
     *
     */
    protected class IncrementHandler implements ResponseHandler {

      int max;
      int current = 0;

      protected IncrementHandler(int max) {
        this.max = max;
      }

      /*
       * (non-Javadoc)
       *
       * @see
       * org.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
       * #response(org.codehaus.jackson.JsonNode)
       */
      @Override
      public void response(JsonNode node) {
        if (current > max) {
          fail(String.format("Received %d messages, but we should only receive %d", current, max));
        }

        assertEquals(current, node.get("id").asInt());
        current++;

      }

      @Override
      public void assertResults() {
        assertEquals(max, current);
      }

    }

    /**
     * Simple handler ensure we get up to count messages
     *
     * @author tnine
     *
     */
    protected class DecrementHandler implements ResponseHandler {

      int max;
      int current;
      int count = 0;

      protected DecrementHandler(int max) {
        this.max = max;
        current = max - 1;
      }

      /*
       * (non-Javadoc)
       *
       * @see
       * org.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
       * #response(org.codehaus.jackson.JsonNode)
       */
      @Override
      public void response(JsonNode node) {
        if (current < 0) {
          fail(String.format("Received %d messages, but we should only receive %d", current, max));
        }

        assertEquals(current, node.get("id").asInt());
        current--;
        count++;

      }

      @Override
      public void assertResults() {
        assertEquals(max, count);
      }

    }

    /**
     * Simple handler ensure we get up to count messages from x to y ascending
     *
     * @author tnine
     *
     */
    protected class ForwardMatchHandler implements ResponseHandler {

      int startValue;
      int count;
      int current = 0;

      protected ForwardMatchHandler(int startValue, int count) {
        this.startValue = startValue;
        this.count = count;

      }

      /*
       * (non-Javadoc)
       *
       * @see
       * org.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
       * #response(org.codehaus.jackson.JsonNode)
       */
      @Override
      public void response(JsonNode node) {

        assertEquals(startValue + current, node.get("id").asInt());

        current++;

      }

      @Override
      public void assertResults() {
        // only ever invoked once
        assertEquals(count, current);

      }

    }

    /**
     * Simple handler ensure we get up to count messages from x to y ascending
     *
     * @author tnine
     *
     */
    protected class ReverseMatchHandler implements ResponseHandler {

      int startValue;
      int count;
      int current = 0;

      protected ReverseMatchHandler(int startValue, int count) {
        this.startValue = startValue;
        this.count = count;

      }

      /*
       * (non-Javadoc)
       *
       * @see
       * org.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
       * #response(org.codehaus.jackson.JsonNode)
       */
      @Override
      public void response(JsonNode node) {

        assertEquals(startValue - count, node.get("id").asInt());

        current++;

      }

      @Override
      public void assertResults() {
        // only ever invoked once
        assertEquals(count, current);

      }

    }

    /**
     * Simple handler to build a list of the message responses
     *
     * @author tnine
     *
     */
    protected class TransactionResponseHandler extends IncrementHandler {

      List<JsonNode> responses = new ArrayList<JsonNode>();

      protected TransactionResponseHandler(int max) {
        super(max);
      }

      /*
       * (non-Javadoc)
       *
       * @see
       * org.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
       * #response(org.codehaus.jackson.JsonNode)
       */
      @Override
      public void response(JsonNode node) {
        super.response(node);

        JsonNode transaction = node.get("transaction");

        assertNotNull(transaction);

        responses.add(node);

      }

      /**
       * Get transaction ids from messages. Key is messageId, value is
       * transactionId
       *
       * @return
       */
      public BiMap<String, String> getTransactionToMessageId() {
        BiMap<String, String> map = HashBiMap.create(responses.size());

        for (JsonNode message : responses) {
          map.put(message.get("uuid").asText(), message.get("transaction").asText());
        }

        return map;

      }

      /**
       * Get all message ids from the response
       *
       * @return
       */
      public List<String> getMessageIds() {
        List<String> results = new ArrayList<String>(responses.size());

        for (JsonNode message : responses) {
          results.add(message.get("uuid").asText());
        }

        return results;
      }

    }

    /**
     * Simple handler to build a list of the message responses asynchronously.
     * Ensures that no responses are duplicated
     *
     * @author tnine
     *
     */
    protected class AsyncTransactionResponseHandler implements ResponseHandler {

      private TreeMap<Integer, JsonNode> responses = new TreeMap<Integer, JsonNode>();
      private Map<Integer, String> threads = new HashMap<Integer, String>();
      private int max;

      protected AsyncTransactionResponseHandler(int max) {
        this.max = max;

      }

      /*
       * (non-Javadoc)
       *
       * @see
       * org.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
       * #response(org.codehaus.jackson.JsonNode)
       */
      @Override
      public void response(JsonNode node) {
        JsonNode transaction = node.get("transaction");

        assertNotNull(transaction);

        Integer id = node.get("id").asInt();

        // we shouldn't have this response
        assertNull(String.format("received id %d twice from thread %s and then thread %s", id, threads.get(id), Thread
            .currentThread().getName()), threads.get(id));

        threads.put(id, Thread.currentThread().getName());

        responses.put(id, node);

      }

      /**
       * Get transaction ids from messages. Key is messageId, value is
       * transactionId
       *
       * @return
       */
      public BiMap<String, String> getTransactionToMessageId() {
        BiMap<String, String> map = HashBiMap.create(responses.size());

        for (JsonNode message : responses.values()) {
          map.put(message.get("uuid").asText(), message.get("transaction").asText());
        }

        return map;

      }

      /**
       * Get all message ids from the response
       *
       * @return
       */
      public List<String> getMessageIds() {
        List<String> results = new ArrayList<String>(responses.size());

        for (JsonNode message : responses.values()) {
          results.add(message.get("uuid").asText());
        }

        return results;
      }

      @Override
      public void assertResults() {
        int count = 0;

        for (JsonNode message : responses.values()) {
          assertEquals(count, message.get("id").asInt());
          count++;
        }

        assertEquals(max, count);
      }

    }


    /**
     * Test that when receiving the messages from a queue, we receive the same
     * amount as "count". Starts from 0 to count-1 for message bodies. Client id
     * optional
     *
     * @param queue the queue
     * @param handler the handler
     * @param commands the commands
     */
    protected void testMessages(Queue queue, ResponseHandler handler, QueueCommand... commands) {
        List<JsonNode> entries = null;

        do {
            for (QueueCommand command : commands) {
                queue = command.processQueue(queue);
            }

            entries = queue.getNextPage();

            for (JsonNode entry : entries) {
                handler.response(entry);
            }

        } while (entries.size() > 0);

    }
}
