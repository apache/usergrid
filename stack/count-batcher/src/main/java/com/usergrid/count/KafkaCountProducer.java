package com.usergrid.count;

import com.usergrid.count.common.Count;
import com.usergrid.count.common.CountSerDeUtils;
import kafka.javaapi.producer.Producer;
import kafka.javaapi.producer.ProducerData;
import kafka.producer.ProducerConfig;

import java.util.Properties;

/**
 * Entry point for coutner transport
 * TODO move to count-batcher
 * @author zznate
 */
public class KafkaCountProducer implements CountProducer {

    public static final String DEF_TOPIC_NAME = "ug_count";

    private Properties properties;
    private Producer<String,String> producer;
    private String topic = DEF_TOPIC_NAME;

    public KafkaCountProducer(String topicName, Properties properties) {
        this.properties = properties;
        this.topic = topicName;
    }

    /**
     * Initialize plumbing for this transport
     */
    public void init() {
        ProducerConfig config = new ProducerConfig(properties);
        producer = new Producer<String, String>(config);
    }

    /**
     * Send the Count over the wire
     * @param count
     */
    public void send(Count count) {
        String jsonVal = CountSerDeUtils.serialize(count);
        ProducerData<String, String> data = new ProducerData<String, String>(topic, jsonVal);
        producer.send(data);
    }
}
