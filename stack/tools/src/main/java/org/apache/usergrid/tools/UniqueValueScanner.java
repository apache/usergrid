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


import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ConsistencyLevel;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.util.RangeBuilder;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
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

    private static final String ENTITY_FIELD_TYPE_ARG = "fieldType";



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
            OptionBuilder.withArgName(ENTITY_NAME_ARG).hasArg().isRequired( false ).withDescription( "specific entity name" )
                .create(ENTITY_NAME_ARG);

        options.addOption( specificEntityNameOption );

        Option fieldTypeOption =
            OptionBuilder.withArgName(ENTITY_FIELD_TYPE_ARG).hasArg().isRequired( false ).withDescription( "field type" )
                .create(ENTITY_FIELD_TYPE_ARG);

        options.addOption( fieldTypeOption );

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

        logger.info("Staring Tool: UniqueValueScanner");
        logger.info("Using Cassandra consistency level: {}", System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM"));


        keyspace = injector.getInstance(com.netflix.astyanax.Keyspace.class);
        mvccEntitySerializationStrategy = injector.getInstance(MvccEntitySerializationStrategy.class);
        uniqueValueSerializationStrategy = injector.getInstance(UniqueValueSerializationStrategy.class);

        String fieldType =
            line.getOptionValue(ENTITY_FIELD_TYPE_ARG) != null ?  line.getOptionValue(ENTITY_FIELD_TYPE_ARG)  : "name" ;
        String entityType = line.getOptionValue(ENTITY_TYPE_ARG);
        String entityName = line.getOptionValue(ENTITY_NAME_ARG);

        AtomicInteger count = new AtomicInteger(0);

        if (entityName != null && !entityName.isEmpty()) {

            if(appToFilter == null){
                throw new RuntimeException("Cannot execute UniqueValueScanner with specific entity without the " +
                    "application UUID for which the entity should exist.");
            }

            if(entityType == null){
                throw new RuntimeException("Cannot execute UniqueValueScanner without the entity type (singular " +
                    "collection name).");
            }

            logger.info("Running entity unique load only");


            //do stuff w/o read repair
            UniqueValueSet uniqueValueSet = uniqueValueSerializationStrategy.load(
                new ApplicationScopeImpl( new SimpleId(appToFilter, "application" ) ),
                ConsistencyLevel.valueOf(System.getProperty("usergrid.read.cl", "LOCAL_QUORUM")), entityType,
                Collections.singletonList(new StringField( fieldType, entityName) ), false);

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("[");

            uniqueValueSet.forEach( uniqueValue -> {


                String entry = "fieldName="+uniqueValue.getField().getName()+
                    ", fieldValue="+uniqueValue.getField().getValue()+
                    ", uuid="+uniqueValue.getEntityId().getUuid()+
                    ", type="+uniqueValue.getEntityId().getType()+
                    ", version="+uniqueValue.getEntityVersion();
                stringBuilder.append("{").append(entry).append("},");
            });

            stringBuilder.deleteCharAt(stringBuilder.length() -1);
            stringBuilder.append("]");

            logger.info("Returned unique value set from serialization load = {}", stringBuilder.toString());

        } else {

            logger.info("Running entity unique scanner only");


            // scan through all unique values and log some info

            Iterator<com.netflix.astyanax.model.Row<ScopedRowKey<TypeField>, EntityVersion>> rows = null;
            try {

                rows = keyspace.prepareQuery(CF_UNIQUE_VALUES)
                    .setConsistencyLevel(com.netflix.astyanax.model.ConsistencyLevel.valueOf(System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM")))
                    .getAllRows()
                    .withColumnRange(new RangeBuilder().setLimit(1000).build())
                    .execute().getResult().iterator();

            } catch (ConnectionException e) {

                logger.error("Error connecting to cassandra", e);
            }


            UUID finalAppToFilter = appToFilter;

            if( rows != null) {
                rows.forEachRemaining(row -> {

                    count.incrementAndGet();

                    if(count.get() % 1000 == 0 ){
                        logger.info("Scanned {} rows in {}", count.get(), CF_UNIQUE_VALUES.getName());
                    }

                    final String fieldName = row.getKey().getKey().getField().getName();
                    final String fieldValue = row.getKey().getKey().getField().getValue().toString();
                    final String scopeType = row.getKey().getScope().getType();
                    final UUID scopeUUID = row.getKey().getScope().getUuid();


                    if (!fieldName.equalsIgnoreCase(fieldType) ||
                        (finalAppToFilter != null && !finalAppToFilter.equals(scopeUUID))
                        ) {
                        // do nothing

                    } else {


                        // if we have more than 1 column, let's check for a duplicate
                        if (row.getColumns() != null && row.getColumns().size() > 1) {

                            final List<EntityVersion> values = new ArrayList<>(row.getColumns().size());

                            Iterator<Column<EntityVersion>> columns = row.getColumns().iterator();
                            columns.forEachRemaining(column -> {


                                final EntityVersion entityVersion = column.getName();


                                logger.trace(
                                    scopeType + ": " + scopeUUID + ", " +
                                        fieldName + ": " + fieldValue + ", " +
                                        "entity type: " + entityVersion.getEntityId().getType() + ", " +
                                        "entity uuid: " + entityVersion.getEntityId().getUuid()
                                );


                                if (entityType != null &&
                                    entityVersion.getEntityId().getType().equalsIgnoreCase(entityType)
                                    ) {

                                    // add the first value into the list
                                    if (values.size() == 0) {

                                        values.add(entityVersion);


                                    } else {

                                        if (!values.get(0).getEntityId().getUuid().equals(entityVersion.getEntityId().getUuid())) {

                                            values.add(entityVersion);

                                            logger.error("Duplicate found for field [{}={}].  Entry 1: [{}], Entry 2: [{}]",
                                                fieldName, fieldValue, values.get(0).getEntityId(), entityVersion.getEntityId());

                                        }

                                    }


                                }

                            });
                        }
                    }


                });
            }else{

                logger.warn("No rows returned from table: {}", CF_UNIQUE_VALUES.getName());

            }

        }
    }
}
