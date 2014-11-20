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

package org.apache.usergrid.corepersistence.migration;


import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.core.guice.CurrentImpl;
import org.apache.usergrid.persistence.core.guice.PreviousImpl;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationException;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.functions.Action1;


/**
 * Migration for migrating graph edges to the new Shards
 */
public class EntityDataMigration implements DataMigration {


    private final MvccEntitySerializationStrategy v1Serialization;
    private final MvccEntitySerializationStrategy v2Serialization;

    private final ManagerCache managerCache;
    private final Keyspace keyspace;


    @Inject
    public EntityDataMigration( @PreviousImpl final MvccEntitySerializationStrategy v1Serialization,
                                @CurrentImpl final MvccEntitySerializationStrategy v2Serialization,
                                final ManagerCache managerCache, final Keyspace keyspace ) {
        this.v1Serialization = v1Serialization;
        this.v2Serialization = v2Serialization;
        this.managerCache = managerCache;
        this.keyspace = keyspace;
    }


    @Override
    public void migrate( final ProgressObserver observer ) throws Throwable {

        final AtomicLong atomicLong = new AtomicLong();

        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache, 1000 )
                                     .doOnNext( new Action1<AllEntitiesInSystemObservable.ApplicationEntityGroup>() {


                                                 @Override
                                                 public void call(
                                                         final AllEntitiesInSystemObservable.ApplicationEntityGroup
                                                                 applicationEntityGroup ) {


                                                     final UUID now = UUIDGenerator.newTimeUUID();

                                                     final Id appScopeId =
                                                             applicationEntityGroup.applicationScope.getApplication();


                                                     final MutationBatch totalBatch = keyspace.prepareMutationBatch();

                                                     //go through each entity in the system, and load it's entire
                                                     // history
                                                     for ( Id entityId : applicationEntityGroup.entityIds ) {

                                                         CollectionScope currentScope = CpNamingUtils
                                                                 .getCollectionScopeNameFromEntityType( appScopeId,
                                                                         entityId.getType() );


                                                         //for each element in the history in the previous version,
                                                         // copy it to the CF in v2
                                                         Iterator<MvccEntity> allVersions = v1Serialization
                                                                 .loadDescendingHistory( currentScope, entityId, now,
                                                                         1000 );

                                                         while ( allVersions.hasNext() ) {
                                                             final MvccEntity version = allVersions.next();

                                                             final MutationBatch versionBatch =
                                                                     v2Serialization.write( currentScope, version );

                                                             totalBatch.mergeShallow( versionBatch );

                                                             if ( atomicLong.incrementAndGet() % 50 == 0 ) {
                                                                 executeBatch( totalBatch, observer, atomicLong );
                                                             }
                                                         }
                                                     }

                                                     executeBatch( totalBatch, observer, atomicLong );
                                                 }
                                             } ).toBlocking().last();
    }


    private void executeBatch( final MutationBatch batch, final ProgressObserver po, final AtomicLong count ) {
        try {
            batch.execute();

            po.update( getVersion(), "Finished copying " + count + " entities to the new format" );
        }
        catch ( ConnectionException e ) {
            po.failed( getVersion(), "Failed to execute mutation in cassandra" );
            throw new DataMigrationException( "Unable to migrate batches ", e );
        }
    }


    @Override
    public int getVersion() {
        return Versions.VERSION_3;
    }
}
