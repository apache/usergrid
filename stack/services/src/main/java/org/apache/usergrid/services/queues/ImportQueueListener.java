/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.services.queues;


import java.util.List;
import java.util.Properties;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.usergrid.management.importer.ImportService;
import org.apache.usergrid.management.importer.ImportServiceImpl;

import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.queue.QueueMessage;
import org.apache.usergrid.services.ServiceManagerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;


//TODO: make sure this is properly instantiated by guice
@Singleton
public class ImportQueueListener extends QueueListener {

    /**
     * Initializes the QueueListener. Need to wire the factories up in guice.
     */
    private static final Logger logger = LoggerFactory.getLogger( ImportQueueListener.class );

    @Autowired
    ImportService importService;

    public static String QUEUE_NAME = "import_v1";
    //TODO: someway to tell the base class what the queuename is. The scope would be different.

    @Inject
    public ImportQueueListener( final ServiceManagerFactory smf, final EntityManagerFactory emf,
                                final Injector injector, final Properties props ) {
        super( smf, emf, injector,  props );
    }


    /**
     * Executes import specific functionality on the list of messages that was returned from the
     * queue.
     * @param messages
     */
    @Override
    public void onMessage( final List<QueueMessage> messages ) throws Exception {
        /**
         * Much like in the original queueListener , we need to translate the Messages that we get
         * back from the QueueMessage into something like an Import message. The way that a
         * notification does it is in line 163 of the notification QueueListener we take the body
         * of the message and typecast it into a model called ApplicationQueueMessage.  Then it does
         * work on the message.
         */
        logger.debug("Doing work in onMessage in ImportQueueListener");
        for (QueueMessage message : messages) {
            ImportQueueMessage queueMessage = ( ImportQueueMessage ) message.getBody();

//        TODO   We still need to hide this queue behind the scheduler importService.downloadAndImportFile( queueMessage );

        }

    }

    //TODO: make this set from the properties file. Due to having a shared amazon account.
    @Override
    public String getQueueName() {
        return QUEUE_NAME;
    }

}
