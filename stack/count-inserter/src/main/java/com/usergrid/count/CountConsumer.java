package com.usergrid.count;

import com.google.common.collect.ImmutableMap;
import com.usergrid.count.common.Count;
import com.usergrid.count.common.CountSerDeUtils;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaMessageStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumes messages from a Kafka queue
 *
 * TODO move to count inserter?
 * @author zznate
 */
public class CountConsumer {

    private Logger log = LoggerFactory.getLogger(CountConsumer.class);

    public static final int DEF_BATCH_INSERT_SIZE = 25;
    public static final int DEF_CONSUMER_COUNT = 4;

    private String topicName;
    private Properties properties;
    private ConsumerConnector consumerConnector;
    private CounterStore counterStore;
    private int batchInsertSize = DEF_BATCH_INSERT_SIZE;
    private int consumerCount = DEF_CONSUMER_COUNT;
    private ExecutorService pollingExecutor;
    private ExecutorService execution;
    private AtomicBoolean suspended = new AtomicBoolean(false);

    /**
     * Create a CountConusmer
     * @param topicName the topic name for the queue to consume
     * @param props the {@link Properties} object which will used to
     *              construct a {@link ConsumerConfig}
     */
    public CountConsumer(String topicName, Properties props) {
        this.topicName = topicName;
        this.properties = props;
    }

    /**
     * CounterStore implementation to use
     * @param counterStore
     */
    public void setCounterStore(CounterStore counterStore) {
        this.counterStore = counterStore;
    }

    /**
     * Set the number of parallel consumer which will be run by this service
     * @param consumerCount
     */
    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    /**
     * Number of Count objects to pull off the wire before invoking the actual insertion
     * @param batchInsertSize
     */
    public void setBatchInsertSize(int batchInsertSize) {
        this.batchInsertSize = batchInsertSize;
    }

    /**
     * Initialize this instance doing three things:
     * <li>populate the configuration</li>
     * <li>initialize the connector</li>
     * <li>Start the executor service</li>
     */
    public void init() {
        ConsumerConfig consumerConfig = new ConsumerConfig(properties);
        consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);
        pollingExecutor = Executors.newFixedThreadPool(consumerCount);
    }

    /**
     * Start polling the queue for new tasks. Work is handed off to {@link #setConsumerCount(int)}
     * threads. {@link CounterStore#save(java.util.Collection)} is invoked once {@link #setBatchInsertSize(int)}
     * threshold has been exceeded.
     *
     */
    public void doExecute() {
        Map<String, List<KafkaMessageStream<String>>> topicMessageStreams =
                consumerConnector.createMessageStreams(ImmutableMap.of(topicName, consumerCount), new StringDecoder());

        List<KafkaMessageStream<String>> streams = topicMessageStreams.get(topicName);

        for(final KafkaMessageStream<String> stream: streams) {
            pollingExecutor.submit(new Runnable() {
                List<Count> counts = new ArrayList<Count>(batchInsertSize);
                public void run() {
                    // TODO instrument
                    log.debug("run() invoked for stream processing");
                    try {
                        for(String message: stream) {
                            // TODO instrument counter
                            Count count = CountSerDeUtils.deserialize(message);
                            counts.add(count);
                            log.debug("Processing {} from wire", count);
                            if ( counts.size() >= batchInsertSize ) {
                                // TODO instrument histogram
                                log.debug("batchInsertSize triggered, invoking counterStore#save");
                                counterStore.save(counts);
                                consumerConnector.commitOffsets();
                                counts.clear();
                            }
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        if ( counts != null && counts.size() > 0 ) {
                            counterStore.save(counts);
                        }
                    }
                }
            });

        }
    }

    public void shutdown() {
        consumerConnector.shutdown();
        suspended.set(true);
        if ( pollingExecutor != null ) {
            pollingExecutor.shutdown();
        }
    }



}
