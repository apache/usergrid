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
package org.usergrid.mq.cassandra;

import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.DynamicCompositeSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.usergrid.mq.QueueManager;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.CounterUtils;

public class QueueManagerFactoryImpl implements QueueManagerFactory,
		ApplicationContextAware {

	public static final Logger logger = LoggerFactory
			.getLogger(QueueManagerFactoryImpl.class);

	public static String IMPLEMENTATION_DESCRIPTION = "Cassandra Queue Manager Factory 1.0";

	ApplicationContext applicationContext;
	CassandraService cass;
	CounterUtils counterUtils;

	public static final StringSerializer se = new StringSerializer();
	public static final ByteBufferSerializer be = new ByteBufferSerializer();
	public static final UUIDSerializer ue = new UUIDSerializer();
	public static final BytesArraySerializer bae = new BytesArraySerializer();
	public static final DynamicCompositeSerializer dce = new DynamicCompositeSerializer();
	public static final LongSerializer le = new LongSerializer();

	/**
	 * Must be constructed with a CassandraClientPool.
	 * 
	 * @param cass
	 *            the cassandra client pool
	 * @param counterUtils
	 *            the CounterUtils
	 */
	public QueueManagerFactoryImpl(CassandraService cass,
			CounterUtils counterUtils) {
		this.cass = cass;
		this.counterUtils = counterUtils;
	}

	@Override
	public String getImpementationDescription() throws Exception {
		return IMPLEMENTATION_DESCRIPTION;
	}

	@Override
	public QueueManager getQueueManager(UUID applicationId) {
		return applicationContext.getAutowireCapableBeanFactory()
				.createBean(QueueManagerImpl.class)
				.init(this, cass, counterUtils, applicationId);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}
