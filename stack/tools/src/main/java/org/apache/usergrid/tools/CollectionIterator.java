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


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.common.base.Optional;
import com.netflix.astyanax.MutationBatch;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.results.EntityQueryExecutor;
import org.apache.usergrid.corepersistence.results.IdQueryExecutor;
import org.apache.usergrid.corepersistence.service.CollectionSearch;
import org.apache.usergrid.corepersistence.service.CollectionService;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.*;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.*;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.InflectionUtils;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


import static org.apache.usergrid.persistence.Schema.getDefaultSchema;


public class CollectionIterator extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( CollectionIterator.class );

    private static final String APPLICATION_ARG = "app";

    private static final String ENTITY_TYPE_ARG = "entityType";

    private static final String REMOVE_CONNECTIONS_ARG = "removeConnections";

    private static final String LATEST_TIMESTAMP_ARG = "latestTimestamp";

    private static final String EARLIEST_TIMESTAMP_ARG = "earliestTimestamp";

    private static final String SECONDS_IN_PAST_ARG = "secondsInPast";

    private static final Long DEFAULT_SECONDS_IN_PAST = 60L * 60L; // hour

    private EntityManager em;

    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = super.createOptions();


        Option appOption = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( true )
            .withDescription( "application id" ).create( APPLICATION_ARG );


        options.addOption( appOption );

        Option collectionOption =
                OptionBuilder.withArgName(ENTITY_TYPE_ARG).hasArg().isRequired( true ).withDescription( "singular collection name" )
                        .create(ENTITY_TYPE_ARG);

        options.addOption( collectionOption );

        Option removeConnectionsOption =
                OptionBuilder.withArgName(REMOVE_CONNECTIONS_ARG).hasArg().isRequired( false ).withDescription( "remove orphaned connections" )
                        .create(REMOVE_CONNECTIONS_ARG);

        options.addOption( removeConnectionsOption );

        Option earliestTimestampOption =
                OptionBuilder.withArgName(EARLIEST_TIMESTAMP_ARG).hasArg().isRequired( false ).withDescription( "earliest timestamp to delete" )
                        .create(EARLIEST_TIMESTAMP_ARG);

        options.addOption( earliestTimestampOption );

        Option latestTimestampOption =
                OptionBuilder.withArgName(LATEST_TIMESTAMP_ARG).hasArg().isRequired( false ).withDescription( "latest timestamp to delete" )
                        .create(LATEST_TIMESTAMP_ARG);

        options.addOption( latestTimestampOption );

        Option secondsInPastOption =
            OptionBuilder.withArgName(SECONDS_IN_PAST_ARG).hasArg().isRequired( false ).withDescription( "how many seconds old orphan must be to be deleted" )
                .create(SECONDS_IN_PAST_ARG);

        options.addOption( secondsInPastOption );


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

        String applicationOption = line.getOptionValue(APPLICATION_ARG);
        String entityTypeOption = line.getOptionValue(ENTITY_TYPE_ARG);
        String removeConnectionsOption = line.getOptionValue(REMOVE_CONNECTIONS_ARG);
        String earliestTimestampOption = line.getOptionValue(EARLIEST_TIMESTAMP_ARG);
        String latestTimestampOption = line.getOptionValue(LATEST_TIMESTAMP_ARG);
        String secondsInPastOption = line.getOptionValue(SECONDS_IN_PAST_ARG);

        if (isBlank(applicationOption)) {
            throw new RuntimeException("Application ID not provided.");
        }
        final UUID app = UUID.fromString(line.getOptionValue(APPLICATION_ARG));

        if (isBlank(entityTypeOption)) {
            throw new RuntimeException("Entity type (singular collection name) not provided.");
        }
        String entityType = entityTypeOption;

        final boolean removeOrphans = !isBlank(removeConnectionsOption) && removeConnectionsOption.toLowerCase().equals("yes");

        if (!isBlank(secondsInPastOption) && !isBlank(latestTimestampOption)) {
            throw new RuntimeException("Can't specify both latest timestamp and seconds in past options.");
        }

        long earliest = 0L;
        if (!isBlank(earliestTimestampOption)) {
            try {
                earliest = Long.parseLong(earliestTimestampOption);
            } catch (Exception e) {
                throw new RuntimeException("Cannot convert earliest timestamp to long: " + earliestTimestampOption);
            }
        }
        final long earliestTimestamp = earliest;

        long currentTimestamp = System.currentTimeMillis();

        // default to DEFAULT_SECONDS_IN_PAST
        long latest = currentTimestamp - (DEFAULT_SECONDS_IN_PAST * 1000L);
        if (!isBlank(latestTimestampOption)) {
            try {
                latest = Long.parseLong(latestTimestampOption);
            } catch (Exception e) {
                throw new RuntimeException("Cannot convert latest timestamp to long: " + latestTimestampOption);
            }
        } else if (!isBlank(secondsInPastOption)) {
            try {
                long secondsInPast = Long.parseLong(secondsInPastOption);
                latest = currentTimestamp - (secondsInPast * 1000L);
            } catch (Exception e) {
                throw new RuntimeException("Cannot convert seconds in past to long: " + secondsInPastOption);
            }
        }
        final long latestTimestamp = latest;

        logger.info("Starting Tool: CollectionIterator");
        logger.info("Orphans {} be deleted", removeOrphans ? "WILL" : "will not");
        logger.info("Timestamp range {} to {}", Long.toString(earliestTimestamp), Long.toString(latestTimestamp));
        logger.info("Using Cassandra consistency level: {}", System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM"));

        em = emf.getEntityManager( app );
        EntityRef headEntity = new SimpleEntityRef("application", app);

        CollectionService collectionService = injector.getInstance(CollectionService.class);
        String collectionName = InflectionUtils.pluralize(entityType);
        String simpleEdgeType = CpNamingUtils.getEdgeTypeFromCollectionName(collectionName);
        logger.info("simpleEdgeType: {}", simpleEdgeType);

        ApplicationScope applicationScope = new ApplicationScopeImpl(new SimpleId(app, "application"));
        Id applicationScopeId = applicationScope.getApplication();
        logger.info("applicationScope.getApplication(): {}", applicationScopeId);
        EdgeSerialization edgeSerialization = injector.getInstance(EdgeSerialization.class);

        Query query = new Query();
        query.setCollection(collectionName);
        query.setLimit(1000);

        com.google.common.base.Optional<String> queryString = com.google.common.base.Optional.absent();

        CollectionInfo collection = getDefaultSchema().getCollection(headEntity.getType(), collectionName);

        GraphManagerFactory gmf = injector.getInstance( GraphManagerFactory.class );
        GraphManager gm = gmf.createEdgeManager(applicationScope);


        final SimpleSearchByEdgeType search =
            new SimpleSearchByEdgeType( applicationScopeId, simpleEdgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                Optional.absent(), false );

        gm.loadEdgesFromSource(search).map(markedEdge -> {

            UUID uuid = markedEdge.getTargetNode().getUuid();
            try {
                    EntityRef entityRef = new SimpleEntityRef(entityType, uuid);
                    org.apache.usergrid.persistence.Entity retrieved = em.get(entityRef);

                    long timestamp = 0;
                    String dateString = "NOT TIME-BASED";
                    if (UUIDUtils.isTimeBased(uuid)){
                        timestamp = UUIDUtils.getTimestampInMillis(uuid);
                        Date uuidDate = new Date(timestamp);
                        DateFormat df = new SimpleDateFormat();
                        df.setTimeZone(TimeZone.getTimeZone("GMT"));
                        dateString = df.format(uuidDate) + " GMT";
                    }

                    if ( retrieved != null ){

                        logger.info("{} - {} - entity data found", uuid, dateString);
                    }else{
                        if (removeOrphans && timestamp >= earliestTimestamp && timestamp <= latestTimestamp) {
                            logger.info("{} - {} - entity data NOT found, REMOVING", uuid, dateString);
                            try {
                                //em.removeItemFromCollection(headEntity, collectionName, entityRef );
                                logger.info("entityRef.asId(): {}", entityRef.asId());
                                MutationBatch batch = edgeSerialization.deleteEdge(applicationScope, markedEdge, UUIDUtils.newTimeUUID());
                                logger.info("BATCH: {}", batch);
                                batch.execute();
                            } catch (Exception e) {
                                logger.error("{} - exception while trying to remove orphaned connection, {}", uuid, e.getMessage());
                            }
                        } else if (removeOrphans) {
                            logger.info("{} - {} ({}) - entity data NOT found, not removing because timestamp not in range", uuid, dateString, timestamp);
                        } else {
                            logger.info("{} - {} ({}) - entity data NOT found", uuid, dateString, timestamp);
                        }
                    }
                } catch (Exception e) {
                    logger.error("{} - exception while trying to load entity data, {} ", uuid, e.getMessage());
                }



           return markedEdge;
        }).toBlocking().last();

    }
}
