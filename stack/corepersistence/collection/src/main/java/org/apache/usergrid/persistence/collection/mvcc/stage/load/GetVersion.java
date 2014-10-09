/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.mvcc.stage.load;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.List;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func1;


/** 
 * Gets the latest version UUID for an Entity without loading the Entity.
 */
public class GetVersion implements Func1<CollectionIoEvent<Id>, UUID> {

    private static final Logger LOG = LoggerFactory.getLogger( GetVersion.class );

    private final MvccLogEntrySerializationStrategy logStrat;


    @Inject
    public GetVersion( final MvccLogEntrySerializationStrategy logStrat ) {
        Preconditions.checkNotNull( logStrat, "logStrat is required" );
        this.logStrat = logStrat;
    }


    @Override
    public UUID call( CollectionIoEvent<Id> idEvent ) {

        Id id = idEvent.getEvent();
        CollectionScope cs = idEvent.getEntityCollection();

        final UUID latestVersion;
        try {
            List<MvccLogEntry> logEntries = logStrat.load( cs, id, UUIDGenerator.newTimeUUID(), 1 );
            latestVersion = logEntries.size()>0 ? logEntries.get(0).getVersion():null;

        } catch (ConnectionException ex) {
            throw new RuntimeException("Unable to get latest version of entity " +
                id.getType() + ":" + id.getUuid().toString(), ex );
        }
       
        return latestVersion;
    }

}
