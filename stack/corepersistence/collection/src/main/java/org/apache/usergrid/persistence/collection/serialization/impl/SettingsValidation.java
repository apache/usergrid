/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.serialization.impl;


import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 *
 * This element validates our SerializationFig and CassandraFig are correct for transport values
 * TODO, this feels like a hack (Even though it's legal in GUICE)  When we have more time, we should look at using
 * something like visitors and SPI to perform validation
 */
@Singleton
public class SettingsValidation {


    @Inject
    public SettingsValidation( final CassandraFig cassandraFig, final SerializationFig serializationFig ) {
        final int thriftBufferSize = cassandraFig.getThriftBufferSize();

        Preconditions.checkArgument( thriftBufferSize > 0, CassandraFig.THRIFT_TRANSPORT_SIZE + " must be > than 0"  );

        final int usableThriftBufferSize = ( int ) (thriftBufferSize*.9);

        final int maxEntitySize = serializationFig.getMaxEntitySize();

        Preconditions.checkArgument( maxEntitySize > 0, CassandraFig.THRIFT_TRANSPORT_SIZE + " must be > than 0"  );

        Preconditions.checkArgument(usableThriftBufferSize >= maxEntitySize, "You cannot set the max entity size to more than the thrift buffer size * .9.  Maximum usable thrift size is " + usableThriftBufferSize + " and max entity size is " + maxEntitySize);

    }


}
