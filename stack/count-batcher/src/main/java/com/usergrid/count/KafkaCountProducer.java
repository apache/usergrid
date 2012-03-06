/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
