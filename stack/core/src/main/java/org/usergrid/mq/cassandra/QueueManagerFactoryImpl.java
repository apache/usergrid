/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
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
import org.usergrid.mq.QueueManager;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.CounterUtils;

public class QueueManagerFactoryImpl implements QueueManagerFactory {

	public static final Logger logger = LoggerFactory
			.getLogger(QueueManagerFactoryImpl.class);

	public static String IMPLEMENTATION_DESCRIPTION = "Cassandra Queue Manager Factory 1.0";

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
	public QueueManagerFactoryImpl(CassandraService cass, CounterUtils counterUtils) {
		this.cass = cass;
        this.counterUtils = counterUtils;
	}

	@Override
	public String getImpementationDescription() throws Exception {
		return IMPLEMENTATION_DESCRIPTION;
	}

	@Override
	public QueueManager getQueueManager(UUID applicationId) {
		return new QueueManagerImpl(this, cass, counterUtils, applicationId);
	}

}
