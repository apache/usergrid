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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
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



public class UniqueValueManager extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( UniqueValueManager.class );

    private static final String OPERATION_ARG = "op";

    private static final String CONFIRM_DELETE_ARG = "confirmDelete";

    private static final String FILEPATH_ARG = "file";



    //copied shamelessly from unique value serialization strat.
    private static final ScopedRowKeySerializer<TypeField> ROW_KEY_SER =
        new ScopedRowKeySerializer<>( UniqueTypeFieldRowKeySerializer.get() );


    private final EntityVersionSerializer ENTITY_VERSION_SER = new EntityVersionSerializer();

    private final MultiTenantColumnFamily<ScopedRowKey<TypeField>, EntityVersion> CF_UNIQUE_VALUES =
        new MultiTenantColumnFamily<>( "Unique_Values_V2", ROW_KEY_SER, ENTITY_VERSION_SER );

    private Session session;

    private MvccEntitySerializationStrategy mvccEntitySerializationStrategy;

    private UniqueValueSerializationStrategy uniqueValueSerializationStrategy;

    private EntityManager em;

    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = super.createOptions();

        Option opOption =
                OptionBuilder.withArgName(OPERATION_ARG).hasArg().isRequired( false ).withDescription( "operation" )
                        .create(OPERATION_ARG);

        options.addOption( opOption );

        Option confirmDeleteOption =
                OptionBuilder.withArgName(CONFIRM_DELETE_ARG).isRequired( false ).withDescription( "confirm delete" )
                        .create(CONFIRM_DELETE_ARG);

        options.addOption( confirmDeleteOption );

        Option filepathOption =
            OptionBuilder.withArgName(FILEPATH_ARG).hasArg().isRequired( true )
                .withDescription( "path to file containing UV info" ).create(FILEPATH_ARG);

        options.addOption( filepathOption );

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

        logger.info("Staring Tool: UniqueValueManager");
        logger.info("Using Cassandra consistency level: {}", System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM"));

        String operation = line.getOptionValue(OPERATION_ARG) != null ? line.getOptionValue(OPERATION_ARG) : "get";
        boolean deleteOp = operation.toLowerCase().equals("delete");
        if (deleteOp && !line.hasOption(CONFIRM_DELETE_ARG)) {
            throw new RuntimeException("Must add confirmDelete option to use delete.");
        }
        String filepath = line.getOptionValue(FILEPATH_ARG);
        if (filepath == null || filepath.isEmpty()) {
            throw new RuntimeException("File is required -- should contain one row per entity formatted like " +
                    "'{uuid}|{entityType}|{fieldType}|{fieldValue}'.  " +
                    "Example: 'b9398e88-ef7f-11e5-9e41-0a2cb9e6caa9|user|email|baasadmins@apigee.com'");
        }

        session = injector.getInstance(Session.class);
        mvccEntitySerializationStrategy = injector.getInstance(MvccEntitySerializationStrategy.class);
        uniqueValueSerializationStrategy = injector.getInstance(UniqueValueSerializationStrategy.class);

        AtomicInteger count = new AtomicInteger(0);

        File listFile = new File(filepath);

        try (BufferedReader br = new BufferedReader(new FileReader(listFile))) {
            String fileLine;
            while ((fileLine = br.readLine()) != null) {
                String[] valuesArray = fileLine.trim().split("\\|");
                if (valuesArray.length != 4) {
                    logger.info("Line: >"+fileLine+"<");
                    throw new RuntimeException("Invalid file -- should contain one row per entity formatted like " +
                            "'{uuid}|{entityType}|{fieldType}|{fieldValue}'.  " +
                            "Example: 'b9398e88-ef7f-11e5-9e41-0a2cb9e6caa9|user|email|whatever@usergrid.com'");
                }
                UUID appUuid = UUID.fromString(valuesArray[0]);
                String entityType = valuesArray[1];
                String fieldType = valuesArray[2];
                String fieldValue = valuesArray[3];

                UniqueValueSet uniqueValueSet = uniqueValueSerializationStrategy.load(
                        new ApplicationScopeImpl(new SimpleId(appUuid, "application")),
                        ConsistencyLevel.valueOf(System.getProperty("usergrid.read.cl", "LOCAL_QUORUM")), entityType,
                        Collections.singletonList(new StringField(fieldType, fieldValue)), false);

                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("[");

                uniqueValueSet.forEach(uniqueValue -> {


                    String entry = "fieldName=" + uniqueValue.getField().getName() +
                            ", fieldValue=" + uniqueValue.getField().getValue() +
                            ", uuid=" + uniqueValue.getEntityId().getUuid() +
                            ", type=" + uniqueValue.getEntityId().getType() +
                            ", version=" + uniqueValue.getEntityVersion();
                    stringBuilder.append("{").append(entry).append("},");
                });

                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                stringBuilder.append("]");

                logger.info("Returned unique value set from serialization load = {}", stringBuilder.toString());

                if (deleteOp) {
                    uniqueValueSet.forEach(uniqueValue -> {
                        logger.info("DELETING UNIQUE VALUE");
                        try {
                            BatchStatement batchStatement = uniqueValueSerializationStrategy.
                                deleteCQL(new ApplicationScopeImpl(new SimpleId(appUuid, "application")), uniqueValue);

                            session.execute(batchStatement);
                        }
                        catch (Exception e) {
                            logger.error("Exception thrown for UV delete: " + e.getMessage());
                        }
                    });
                }
            }
        }
    }

        /*
        } else {

            logger.info("Running entity unique scanner only");


            // scan through all unique values and log some info

            Iterator<com.netflix.astyanax.model.Row<ScopedRowKey<TypeField>, EntityVersion>> rows = null;
            try {

                rows = keyspace.prepareQuery(CF_UNIQUE_VALUES)
                    .setConsistencyLevel(ConsistencyLevel.valueOf(System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM")))
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
    */
}
