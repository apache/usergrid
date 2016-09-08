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

import com.google.common.base.*;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.util.RangeBuilder;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.*;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
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

    private static final String CONFIRM_UPDATE_ARG = "confirmUpdate";

    private static final String SERIALIZATION_REPAIR_ARG = "useSerializationRepair";

    private static final String FILEPATH_ARG = "file";



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

        Option opOption =
                OptionBuilder.withArgName(OPERATION_ARG).hasArg().isRequired( false ).withDescription( "operation" )
                        .create(OPERATION_ARG);

        options.addOption( opOption );

        Option confirmDeleteOption =
                OptionBuilder.withArgName(CONFIRM_DELETE_ARG).isRequired( false ).withDescription( "confirm delete" )
                        .create(CONFIRM_DELETE_ARG);

        options.addOption( confirmDeleteOption );

        Option confirmUpdateOption =
            OptionBuilder.withArgName(CONFIRM_UPDATE_ARG).isRequired( false ).withDescription( "confirm update" )
                .create(CONFIRM_UPDATE_ARG);

        options.addOption( confirmUpdateOption );

        Option useSerializationRepair =
            OptionBuilder.withArgName(SERIALIZATION_REPAIR_ARG).isRequired( false ).withDescription( "use unique value serialization repair to keep oldest index" )
                .create(SERIALIZATION_REPAIR_ARG);

        options.addOption( useSerializationRepair );

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
        boolean updateOp = operation.toLowerCase().equals("update");
        if (deleteOp && !line.hasOption(CONFIRM_DELETE_ARG)) {
            throw new RuntimeException("Must add confirmDelete option to use delete.");
        }else if( updateOp && !line.hasOption(CONFIRM_UPDATE_ARG) ){
            throw new RuntimeException("Must add confirmUpdate option to use update.");
        }
        String filepath = line.getOptionValue(FILEPATH_ARG);
        if (filepath == null || filepath.isEmpty()) {
            throw new RuntimeException("File is required -- should contain one row per entity formatted like " +
                    "'{uuid}|{entityType}|{fieldType}|{fieldValue}'.  " +
                    "Example: 'b9398e88-ef7f-11e5-9e41-0a2cb9e6caa9|user|email|baasadmins@apigee.com'");
        }

        keyspace = injector.getInstance(com.netflix.astyanax.Keyspace.class);
        mvccEntitySerializationStrategy = injector.getInstance(MvccEntitySerializationStrategy.class);
        uniqueValueSerializationStrategy = injector.getInstance(UniqueValueSerializationStrategy.class);

        AtomicInteger count = new AtomicInteger(0);

        File listFile = new File(filepath);

        boolean useSerializationRepair = false;
        if(line.hasOption(SERIALIZATION_REPAIR_ARG)){
            useSerializationRepair = true;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(listFile))) {
            String fileLine;
            while ((fileLine = br.readLine()) != null) {
                String[] valuesArray = fileLine.trim().split("\\|");
                if (valuesArray.length != 4 && valuesArray.length != 5) {
                    logger.info("Line: >"+fileLine+"<");
                    throw new RuntimeException("Invalid file -- should contain one row per entity formatted like " +
                            "'{uuid}|{entityType}|{fieldType}|{fieldValue}|{newEntityUUID}'.  " +
                            "Example: 'b9398e88-ef7f-11e5-9e41-0a2cb9e6caa9|user|email|whatever@usergrid.com|newEntityUUID'.  " +
                            "Param {newEntityUUID} is optional.");
                }
                UUID appUuid = UUID.fromString(valuesArray[0]);
                String entityType = valuesArray[1];
                String fieldType = valuesArray[2];
                String fieldValue = valuesArray[3];

                UniqueValueSet uniqueValueSet = uniqueValueSerializationStrategy.load(
                    new ApplicationScopeImpl(new SimpleId(appUuid, "application")),
                    ConsistencyLevel.valueOf(System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM")), entityType,
                    Collections.singletonList(new StringField(fieldType, fieldValue)), useSerializationRepair);

                if( updateOp) {

                    if(valuesArray.length!=5){
                        throw new RuntimeException("Missing param {newEntityUUID}");
                    }
                    String updateUUID = valuesArray[4];

                    ApplicationScope applicationScope = new ApplicationScopeImpl(new SimpleId(appUuid, "application"));
                    com.google.common.base.Optional<MvccEntity> entity =
                        mvccEntitySerializationStrategy.load(applicationScope, new SimpleId(UUID.fromString(updateUUID), entityType));

                    if( !entity.isPresent()
                        || !entity.get().getEntity().isPresent() ){
                        throw new RuntimeException("Unable to update unique value index because supplied UUID "+updateUUID+" does not exist");
                    }

                    logger.info("Delete unique value: {}",  uniqueValueSet.getValue(fieldType));
                    uniqueValueSerializationStrategy.delete(applicationScope, uniqueValueSet.getValue(fieldType)).execute();

                    UniqueValue newUniqueValue =
                        new UniqueValueImpl(new StringField(fieldType, fieldValue), entity.get().getId(), entity.get().getVersion());
                    logger.info("Writing new unique value: {}", newUniqueValue);
                    uniqueValueSerializationStrategy.write(applicationScope, newUniqueValue).execute();

                    logger.info("Re-loading unique value set for field");

                }

                uniqueValueSet = uniqueValueSerializationStrategy.load(
                    new ApplicationScopeImpl(new SimpleId(appUuid, "application")),
                    ConsistencyLevel.valueOf(System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM")), entityType,
                    Collections.singletonList(new StringField(fieldType, fieldValue)), useSerializationRepair);

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
                            uniqueValueSerializationStrategy.delete(new ApplicationScopeImpl(new SimpleId(appUuid, "application")), uniqueValue).execute();
                        }
                        catch (Exception e) {
                            logger.error("Exception thrown for UV delete: " + e.getMessage());
                        }
                    });
                }
            }
        }
    }
}
