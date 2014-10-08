/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.event.impl;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.UUID;


public class EntityDeletedImpl implements EntityDeleted {

    private static final Logger LOG = LoggerFactory.getLogger(EntityDeletedImpl.class);

    private MvccEntitySerializationStrategy mvccEntitySerializationStrategy;
    private MvccLogEntrySerializationStrategy logEntrySerializationStrategy;

    public EntityDeletedImpl(MvccEntitySerializationStrategy mvccEntitySerializationStrategy, MvccLogEntrySerializationStrategy logEntrySerializationStrategy){

        this.mvccEntitySerializationStrategy = mvccEntitySerializationStrategy;
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
    }

    @Override
    public void deleted(CollectionScope scope, Id entityId, UUID inclusiveVersionToDeleteFrom) {
        //TODO: clean up cass versions
        MvccEntity mvccEntity = mvccEntitySerializationStrategy.load(scope,entityId,inclusiveVersionToDeleteFrom);
        final MutationBatch entityDelete = mvccEntitySerializationStrategy.delete(scope, entityId, mvccEntity.getVersion());
        final MutationBatch logDelete = logEntrySerializationStrategy.delete(scope, entityId, inclusiveVersionToDeleteFrom);
        try {
            entityDelete.execute();
            logDelete.execute();
        }catch (ConnectionException ce){
            LOG.error("Error deleing from", ce);
        }

    }
}
