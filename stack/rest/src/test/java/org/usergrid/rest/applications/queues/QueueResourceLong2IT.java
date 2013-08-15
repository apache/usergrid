package org.usergrid.rest.applications.queues;

import com.google.common.collect.BiMap;
import org.junit.Rule;
import org.junit.Test;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.rest.TestContextSetup;
import org.usergrid.rest.test.resource.app.queue.Queue;
import org.usergrid.rest.test.resource.app.queue.Transaction;
import org.usergrid.utils.MapUtils;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@Concurrent()
public class QueueResourceLong2IT extends AbstractQueueResourceIT
{

    @Rule
    public TestContextSetup context = new TestContextSetup( this );

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

}
