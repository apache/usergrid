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
package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Session;

import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValueException;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValuesFig;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValuesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.exception.WriteCommitException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.util.EntityUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.functions.Func1;


/**
 * This phase should invoke any finalization, and mark the entity as committed in the
 * data store before returning
 */
@Singleton
public class WriteCommit implements Func1<CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>> {

    private static final Logger logger = LoggerFactory.getLogger( WriteCommit.class );

    ActorSystemFig actorSystemFig;
    UniqueValuesFig uniqueValuesFig;
    UniqueValuesService akkaUvService;

    @Inject
    private UniqueValueSerializationStrategy uniqueValueStrat;

    private final MvccLogEntrySerializationStrategy logEntryStrat;

    private final MvccEntitySerializationStrategy entityStrat;

    private final Session session;


    @Inject
    public WriteCommit( final MvccLogEntrySerializationStrategy logStrat,
                        final MvccEntitySerializationStrategy entryStrat,
                        final UniqueValueSerializationStrategy uniqueValueStrat,
                        final ActorSystemFig actorSystemFig,
                        final UniqueValuesFig uniqueValuesFig,
                        final UniqueValuesService akkaUvService,
                        final Session session ) {


        Preconditions.checkNotNull( logStrat, "MvccLogEntrySerializationStrategy is required" );
        Preconditions.checkNotNull( entryStrat, "MvccEntitySerializationStrategy is required" );
        Preconditions.checkNotNull( uniqueValueStrat, "UniqueValueSerializationStrategy is required");

        this.logEntryStrat = logStrat;
        this.entityStrat = entryStrat;
        this.uniqueValueStrat = uniqueValueStrat;
        this.actorSystemFig = actorSystemFig;
        this.uniqueValuesFig = uniqueValuesFig;
        this.akkaUvService = akkaUvService;
        this.session = session;

    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<MvccEntity> ioEvent ) {
        return confirmUniqueFields( ioEvent );
    }


    private CollectionIoEvent<MvccEntity> confirmUniqueFields(CollectionIoEvent<MvccEntity> ioEvent) {
        final MvccEntity mvccEntity = ioEvent.getEvent();
        MvccValidationUtils.verifyMvccEntityWithEntity( mvccEntity );

        final Id entityId = mvccEntity.getId();
        final UUID version = mvccEntity.getVersion();
        final ApplicationScope applicationScope = ioEvent.getEntityCollection();

        //set the version into the entity
        final Entity entity = mvccEntity.getEntity().get();

        EntityUtils.setVersion( entity, version );

        MvccValidationUtils.verifyMvccEntityWithEntity( ioEvent.getEvent() );
        ValidationUtils.verifyTimeUuid( version ,"version" );

        final MvccLogEntry startEntry =
            new MvccLogEntryImpl( entityId, version, Stage.COMMITTED, MvccLogEntry.State.COMPLETE );



        MutationBatch logMutation = logEntryStrat.write( applicationScope, startEntry );

        // now get our actual insert into the entity data
        MutationBatch entityMutation = entityStrat.write( applicationScope, mvccEntity );

        // merge the 2 into 1 mutation
        logMutation.mergeShallow( entityMutation );

        // akkaFig may be null when this is called from JUnit tests
        if ( actorSystemFig != null && actorSystemFig.getEnabled() ) {
            String region = ioEvent.getRegion();
            if ( region == null ) {
                region = uniqueValuesFig.getAuthoritativeRegion();
            }
            if ( region == null ) {
                region = actorSystemFig.getRegionLocal();
            }
            confirmUniqueFieldsAkka( mvccEntity, version, applicationScope, region );
        } else {
            confirmUniqueFields( mvccEntity, version, applicationScope, logMutation );
        }

        try {
            logMutation.execute();
        }
        catch ( ConnectionException e ) {
            logger.error( "Failed to execute write asynchronously ", e );
            throw new WriteCommitException( mvccEntity, applicationScope,
                "Failed to execute write asynchronously ", e );
        }

        return ioEvent;
    }


    private void confirmUniqueFields(
        MvccEntity mvccEntity, UUID version, ApplicationScope scope, MutationBatch logMutation) {

        final Entity entity = mvccEntity.getEntity().get();

        // re-write the unique values but this time with no TTL
        final BatchStatement uniqueBatch = new BatchStatement();

        for ( Field field : EntityUtils.getUniqueFields(mvccEntity.getEntity().get()) ) {

                UniqueValue written  = new UniqueValueImpl( field, entity.getId(), version);

                uniqueBatch.add(uniqueValueStrat.writeCQL(scope,  written, -1 ));

                logger.debug("Finalizing {} unique value {}", field.getName(), field.getValue().toString());


        }

        try {
            logMutation.execute();
            session.execute(uniqueBatch);
        }
        catch ( ConnectionException e ) {
            logger.error( "Failed to execute write asynchronously ", e );
            throw new WriteCommitException( mvccEntity, scope,
                "Failed to execute write asynchronously ", e );
        }
    }


    private void confirmUniqueFieldsAkka(
        MvccEntity mvccEntity, UUID version, ApplicationScope scope, String region ) {

        final Entity entity = mvccEntity.getEntity().get();

        try {
            akkaUvService.confirmUniqueValues( scope, entity, version, region );

        } catch (UniqueValueException e) {

            Map<String, Field> violations = new HashMap<>();
            violations.put( e.getField().getName(), e.getField() );

            throw new WriteUniqueVerifyException( mvccEntity, scope, violations  );
        }
    }
}
