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



import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.netflix.hystrix.HystrixCommandProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValueException;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValuesFig;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValuesService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.util.EntityUtils;

import rx.functions.Action1;

import java.util.*;


/**
 * This phase execute all unique value verification on the MvccEntity.
 */
@Singleton
public class WriteUniqueVerify implements Action1<CollectionIoEvent<MvccEntity>> {

    private static final Logger logger = LoggerFactory.getLogger( WriteUniqueVerify.class );

    private ActorSystemFig actorSystemFig;
    private UniqueValuesFig uniqueValuesFig;
    private UniqueValuesService akkaUvService;

    private final UniqueValueSerializationStrategy uniqueValueStrat;

    private static int uniqueVerifyPoolSize = 100;

    private static int uniqueVerifyTimeoutMillis= 5000;

    protected final SerializationFig serializationFig;

    protected final Keyspace keyspace;

    protected final Session session;

    private final CassandraConfig cassandraFig;


    @Inject
    public WriteUniqueVerify(final UniqueValueSerializationStrategy uniqueValueSerializiationStrategy,
                             final SerializationFig serializationFig,
                             final Keyspace keyspace,
                             final CassandraConfig cassandraFig,
                             final ActorSystemFig actorSystemFig,
                             final UniqueValuesFig uniqueValuesFig,
                             final UniqueValuesService akkaUvService,
                             final Session session ) {

        this.keyspace = keyspace;
        this.cassandraFig = cassandraFig;
        this.actorSystemFig = actorSystemFig;
        this.uniqueValuesFig = uniqueValuesFig;
        this.akkaUvService = akkaUvService;
        this.session = session;

        Preconditions.checkNotNull( uniqueValueSerializiationStrategy, "uniqueValueSerializationStrategy is required" );
        Preconditions.checkNotNull( serializationFig, "serializationFig is required" );

        this.uniqueValueStrat = uniqueValueSerializiationStrategy;
        this.serializationFig = serializationFig;

        uniqueVerifyPoolSize = this.serializationFig.getUniqueVerifyPoolSize();
    }


    @Override
    public void call( final CollectionIoEvent<MvccEntity> ioevent ) {
        if ( actorSystemFig != null && actorSystemFig.getEnabled() && uniqueValuesFig.getUnqiueValueViaCluster() ) {
            verifyUniqueFieldsAkka( ioevent );
        } else {
            verifyUniqueFields( ioevent );
        }
    }

    private void verifyUniqueFieldsAkka(CollectionIoEvent<MvccEntity> ioevent) {

        MvccValidationUtils.verifyMvccEntityWithEntity( ioevent.getEvent() );

        final MvccEntity mvccEntity = ioevent.getEvent();

        final Entity entity = mvccEntity.getEntity().get();

        final ApplicationScope applicationScope = ioevent.getEntityCollection();

        String authoritativeRegion = ioevent.getAuthoritativeRegion();
        if ( StringUtils.isEmpty(authoritativeRegion) ) {
            authoritativeRegion = uniqueValuesFig.getAuthoritativeRegion();
        }
        if ( StringUtils.isEmpty(authoritativeRegion) ) {
            authoritativeRegion = actorSystemFig.getRegionLocal();
        }
        try {
            akkaUvService.reserveUniqueValues( applicationScope, entity, mvccEntity.getVersion(), authoritativeRegion );

        } catch (UniqueValueException e) {
            Map<String, Field> violations = new HashMap<>();
            violations.put( e.getField().getName(), e.getField() );
            throw new WriteUniqueVerifyException( mvccEntity, applicationScope, violations  );
        }
    }

    private void verifyUniqueFields(CollectionIoEvent<MvccEntity> ioevent) {

        MvccValidationUtils.verifyMvccEntityWithEntity( ioevent.getEvent() );

        final MvccEntity mvccEntity = ioevent.getEvent();

        final Entity entity = mvccEntity.getEntity().get();

        final ApplicationScope scope = ioevent.getEntityCollection();

        final BatchStatement batch = new BatchStatement();
        //allocate our max size, worst case
        final List<Field> uniqueFields = new ArrayList<>( entity.getFields().size() );

        //
        // Construct all the functions for verifying we're unique
        //

        final Map<String, Field> preWriteUniquenessViolations = new HashMap<>( uniqueFields.size() );

        for ( final Field field : EntityUtils.getUniqueFields(entity)) {

            // if it's unique, create a function to validate it and add it to the list of
            // concurrent validations

            // use write-first then read strategy
            final UniqueValue written = new UniqueValueImpl( field, mvccEntity.getId(), mvccEntity.getVersion() );


            // don't use read repair on this pre-write check
            UniqueValueSet set = uniqueValueStrat.load(scope, cassandraFig.getDataStaxReadCl(),
                written.getEntityId().getType(), Collections.singletonList(written.getField()), false);

            set.forEach(uniqueValue -> {

                if(!uniqueValue.getEntityId().getUuid().equals(written.getEntityId().getUuid())){

                    if(logger.isTraceEnabled()){
                        logger.trace("Pre-write violation detected. Attempted write for unique value [{}={}] and " +
                            "entity id [{}], entity version [{}] conflicts with already existing entity id [{}], " +
                            "entity version [{}]",
                            written.getField().getName(),
                            written.getField().getValue().toString(),
                            written.getEntityId().getUuid(),
                            written.getEntityVersion(),
                            uniqueValue.getEntityId().getUuid(),
                            uniqueValue.getEntityVersion());
                    }

                    preWriteUniquenessViolations.put(field.getName(), field);

                }

            });



            // only build the batch statement if we don't have a violation for the field
            if( preWriteUniquenessViolations.get(field.getName()) == null) {

                // use TTL in case something goes wrong before entity is finally committed
                batch.add(uniqueValueStrat.writeCQL(scope, written, serializationFig.getTimeout()));

                uniqueFields.add(field);
            }
        }

        if(preWriteUniquenessViolations.size() > 0 ){
            if(logger.isTraceEnabled()){
                logger.trace("Pre-write unique violations found, raising exception before executing first write");
            }

            throw new WriteUniqueVerifyException(mvccEntity, scope, preWriteUniquenessViolations );
        }

        //short circuit nothing to do
        if ( uniqueFields.size() == 0 ) {
            return  ;
        }

        //perform the write
        session.execute(batch);


        // use simple thread pool to verify fields in parallel
        ConsistentReplayCommand cmd = new ConsistentReplayCommand(
            uniqueValueStrat,cassandraFig,scope, entity.getId().getType(), uniqueFields,entity);

        Map<String,Field> uniquenessViolations = cmd.execute();

        //do we want to do this?

        //We have violations, throw an exception
        if ( !uniquenessViolations.isEmpty() ) {
            throw new WriteUniqueVerifyException( mvccEntity, ioevent.getEntityCollection(), uniquenessViolations );
        }
    }


    private static class ConsistentReplayCommand extends HystrixCommand<Map<String,Field>>{

        private final UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
        private final CassandraConfig fig;
        private final ApplicationScope scope;
        private final String type;
        private final List<Field> uniqueFields;
        private final Entity entity;

        public ConsistentReplayCommand( UniqueValueSerializationStrategy uniqueValueSerializationStrategy,
                                        CassandraConfig fig, ApplicationScope scope, final String type, List<Field>
                                            uniqueFields, Entity entity ){
            super(REPLAY_GROUP);
            this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
            this.fig = fig;
            this.scope = scope;
            this.type = type;
            this.uniqueFields = uniqueFields;
            this.entity = entity;
        }

        @Override
        protected Map<String, Field> run() throws Exception {
            return executeStrategy(fig.getDataStaxReadCl());
        }

        @Override
        protected Map<String, Field> getFallback() {
            // fallback with same CL as there are many reasons the 1st execution failed, not just due to consistency problems
            return executeStrategy(fig.getDataStaxReadCl());

        }

        public Map<String, Field> executeStrategy(ConsistencyLevel consistencyLevel){

            final UniqueValueSet uniqueValues;

            // load ascending for verification to make sure we wrote is the last read back
            // don't read repair on this read because our write-first strategy will introduce a duplicate
            uniqueValues =
                uniqueValueSerializationStrategy.load( scope, consistencyLevel, type,  uniqueFields, false);



            final Map<String, Field> uniquenessViolations = new HashMap<>( uniqueFields.size() );

            //loop through each field that was unique
            for ( final Field field : uniqueFields ) {

                final UniqueValue uniqueValue = uniqueValues.getValue( field.getName() );

                if ( uniqueValue == null ) {
                    throw new RuntimeException(
                        String.format( "Could not retrieve unique value for field %s, unable to verify",
                            field.getName() ) );
                }

                final Id returnedEntityId = uniqueValue.getEntityId();

                if ( !entity.getId().equals(returnedEntityId) ) {

                    if(logger.isTraceEnabled()) {
                        logger.trace("Violation occurred when verifying unique value [{}={}]. " +
                            "Returned entity id [{}] does not match expected entity id [{}]",
                            field.getName(), field.getValue().toString(),
                            returnedEntityId,
                            entity.getId()
                        );
                    }

                    uniquenessViolations.put( field.getName(), field );
                }
            }

            return uniquenessViolations;
        }
    }

    /**
     * Command group used for realtime user commands
     */
    private static final HystrixCommand.Setter
        REPLAY_GROUP = HystrixCommand.Setter.withGroupKey( HystrixCommandGroupKey.Factory.asKey( "uniqueVerify" ) )
        .andThreadPoolPropertiesDefaults(
            HystrixThreadPoolProperties.Setter().withCoreSize( uniqueVerifyPoolSize ) )
        .andCommandPropertiesDefaults(
            HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(uniqueVerifyTimeoutMillis));
}
