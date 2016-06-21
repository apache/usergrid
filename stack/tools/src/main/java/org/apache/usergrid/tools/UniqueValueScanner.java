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
package org.apache.usergrid.tools;


import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import com.google.common.base.Optional;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.util.RangeBuilder;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.*;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;



public class UniqueValueScanner extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( UniqueValueScanner.class );

    private static final String APPLICATION_ARG = "app";

    private static final String ENTITY_TYPE_ARG = "entityType";

    private static final String ENTITY_NAME_ARG = "entityName";


    //copied shamelessly from unique value serialization strat.
    private static final ScopedRowKeySerializer<TypeField> ROW_KEY_SER =
        new ScopedRowKeySerializer<>( UniqueTypeFieldRowKeySerializer.get() );


    private final EntityVersionSerializer ENTITY_VERSION_SER = new EntityVersionSerializer();

    private final MultiTenantColumnFamily<ScopedRowKey<TypeField>, EntityVersion> CF_UNIQUE_VALUES =
        new MultiTenantColumnFamily<>( "Unique_Values_V2", ROW_KEY_SER, ENTITY_VERSION_SER );

    private com.netflix.astyanax.Keyspace keyspace;

    private MvccEntitySerializationStrategy mvccEntitySerializationStrategy;

    private UniqueValueSerializationStrategy uniqueValueSerializationStrategy;

    private EntityManager em;

    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = super.createOptions();


        Option appOption = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( true )
            .withDescription( "application id" ).create( APPLICATION_ARG );


        options.addOption( appOption );

        Option collectionOption =
            OptionBuilder.withArgName(ENTITY_TYPE_ARG).hasArg().isRequired( true ).withDescription( "collection name" )
                .create(ENTITY_TYPE_ARG);

        options.addOption( collectionOption );

        Option specificEntityNameOption =
            OptionBuilder.withArgName(ENTITY_NAME_ARG).hasArg().isRequired( true ).withDescription( "specific entity name" )
                .create(ENTITY_NAME_ARG);

        options.addOption( specificEntityNameOption );


        return options;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void runTool( CommandLine line ) throws Exception {

        startSpring();

        UUID appToFilter = null;
        if (!line.getOptionValue(APPLICATION_ARG).isEmpty()) {
            appToFilter = UUID.fromString(line.getOptionValue(APPLICATION_ARG));
        }


        logger.info("Starting entity unique scanner");

        keyspace = injector.getInstance(com.netflix.astyanax.Keyspace.class);
        mvccEntitySerializationStrategy = injector.getInstance(MvccEntitySerializationStrategy.class);
        uniqueValueSerializationStrategy = injector.getInstance(UniqueValueSerializationStrategy.class);


        String entityType = line.getOptionValue(ENTITY_TYPE_ARG);
        String entityName = line.getOptionValue(ENTITY_NAME_ARG);

        if (entityName != null && !entityName.isEmpty()) {

            if(appToFilter == null){
                throw new RuntimeException("Cannot execute UniqueValueScanner with specific entity without the " +
                    "application UUID for which the entity should exist.");
            }

            if(entityType == null){
                throw new RuntimeException("Cannot execute UniqueValueScanner without the entity type (singular " +
                    "collection name).");
            }

            //do stuff
            UniqueValueSet uniqueValueSet = uniqueValueSerializationStrategy.load(
                new ApplicationScopeImpl( new SimpleId(appToFilter, "application" ) ),
                entityType,
                Collections.singletonList(new StringField( "name", entityName) ));

            logger.info("Returned unique value set from serialization load = {}", uniqueValueSet);

        } else {

            // scan through all unique values and log some info

            Iterator<com.netflix.astyanax.model.Row<ScopedRowKey<TypeField>, EntityVersion>> rows = null;
            try {

                rows = keyspace.prepareQuery(CF_UNIQUE_VALUES)
                    .getAllRows()
                    .withColumnRange(new RangeBuilder().build())
                    .execute().getResult().iterator();

            } catch (ConnectionException e) {

            }


            UUID finalAppToFilter = appToFilter;
            rows.forEachRemaining(row -> {

                String fieldName = row.getKey().getKey().getField().getName();
                String scopeType = row.getKey().getScope().getType();
                UUID scopeUUID = row.getKey().getScope().getUuid();

                if (!fieldName.equalsIgnoreCase("name") ||
                    (finalAppToFilter != null && !finalAppToFilter.equals(scopeUUID))
                    ) {
                    // do nothing

                } else {

                    if (em == null && finalAppToFilter.equals(scopeUUID)) {
                        em = emf.getEntityManager(scopeUUID);
                    }

                    Iterator<Column<EntityVersion>> columns = row.getColumns().iterator();
                    columns.forEachRemaining(column -> {

                        EntityVersion entityVersion = column.getName();

                        if (entityType != null &&
                            entityVersion.getEntityId().getType().equalsIgnoreCase(entityType)
                            ) {

                            String fieldValue = row.getKey().getKey().getField().getValue().toString();

                            logger.trace(
                                scopeType + ": " + scopeUUID + ", " +
                                    fieldName + ": " + fieldValue + ", " +
                                    "entity type: " + entityVersion.getEntityId().getType() + ", " +
                                    "entity uuid: " + entityVersion.getEntityId().getUuid()
                            );


                            Entity entity = em.getUniqueEntityFromAlias(entityType, fieldValue, false);

//                       Optional<MvccEntity> entity = mvccEntitySerializationStrategy.
//                            load(new ApplicationScopeImpl(new SimpleId(scopeUUID, scopeType)), entityVersion.getEntityId());
//
//                        if(!entity.isPresent()){

                            if (entity == null) {

                                logger.error("{}: {}. Entity with type=[{}],  name=[{}], and uuid=[{}] has a unique value entry " +
                                        "but cannot be loaded from Mvcc entity serialization",
                                    scopeType,
                                    scopeUUID,
                                    entityVersion.getEntityId().getType(),
                                    fieldValue,
                                    entityVersion.getEntityId().getUuid());
                            }

                        } else {
                            // do nothing
                        }


                    });
                }


            });

        }
    }
}
