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

import com.google.common.base.Optional;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.model.entity.*;

import org.apache.usergrid.utils.InflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import sun.security.provider.SHA;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;


public class ShardManager extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger(ShardManager.class);

    private static final String APPLICATION_ARG = "app";

    private static final String ENTITY_TYPE_ARG = "entityType";

    private static final String REPAIR_TASK = "repairTask";

    private static final String SHARD_TYPE_ARG = "shardType";


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {


        Options options = super.createOptions();


        Option appOption = OptionBuilder.withArgName(APPLICATION_ARG).hasArg().isRequired(true)
            .withDescription("application id").create(APPLICATION_ARG);

        options.addOption(appOption);

        Option collectionOption =
            OptionBuilder.withArgName(ENTITY_TYPE_ARG).hasArg().isRequired(true).withDescription("singular collection name")
                .create(ENTITY_TYPE_ARG);

        options.addOption(collectionOption);

        Option repairOption =
            OptionBuilder.withArgName(REPAIR_TASK).hasArg().isRequired(false).withDescription("repair task to execute")
                .create(REPAIR_TASK);

        options.addOption(repairOption);

        Option shardTypeOption =
            OptionBuilder.withArgName(SHARD_TYPE_ARG).hasArg().isRequired(true).withDescription("either collection or connection")
                .create(SHARD_TYPE_ARG);

        options.addOption(shardTypeOption);

        return options;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void runTool(CommandLine line) throws Exception {

        startSpring();

        if (line.getOptionValue(APPLICATION_ARG).isEmpty()) {
            throw new RuntimeException("Application ID not provided.");
        }
        final UUID app = UUID.fromString(line.getOptionValue(APPLICATION_ARG));

        String entityType = line.getOptionValue(ENTITY_TYPE_ARG);

        String repairTask = line.getOptionValue(REPAIR_TASK);

        String shardType = line.getOptionValue(SHARD_TYPE_ARG);

        boolean repair = false;
        if( isNotEmpty(repairTask) && (
            repairTask.equalsIgnoreCase("removeAllShardEnds") || repairTask.equalsIgnoreCase("removeLastShardEnd") ||
            repairTask.equalsIgnoreCase("resetAllCompactionStatus"))) {

            repair = true;
        }


        logger.info("Starting Tool: ShardManager");
        logger.info("Using Cassandra consistency level: {}", System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM"));

        EntityRef headEntity = new SimpleEntityRef("application", app);

        ApplicationScope applicationScope = new ApplicationScopeImpl(new SimpleId(app, "application"));
        EdgeShardSerialization edgeShardSerialization = injector.getInstance(EdgeShardSerialization.class);

        String collectionName = InflectionUtils.pluralize(entityType);

        // default to assume collection
        String metaType = CpNamingUtils.getEdgeTypeFromCollectionName(collectionName);

            if( isNotEmpty(shardType) ){

            if( shardType.equalsIgnoreCase("collection")){
                metaType = CpNamingUtils.getEdgeTypeFromCollectionName(collectionName);

            }else if( shardType.equalsIgnoreCase("connection")){
                metaType = CpNamingUtils.getEdgeTypeFromConnectionType(entityType);
            }
        }


        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode(headEntity.asId(), metaType);

        Iterator<Shard> shards = edgeShardSerialization.getShardMetaData(applicationScope, Optional.absent(), directedEdgeMeta);

        boolean firstShard = true;
        while (shards.hasNext()) {
            Shard shard = shards.next();

            logger.info("Seeking over shard: {}", shard);

            if(repair) {

                logger.info("Repair enabled with task: {}", repairTask);

                if( repairTask.equalsIgnoreCase("removeLastShardEnd") && firstShard){

                    logger.info("Removing shard end from shard: {}", shard);

                    shard.setShardEnd(Optional.absent());
                    edgeShardSerialization.writeShardMeta(applicationScope, shard, directedEdgeMeta).execute();

                }else if ( repairTask.equalsIgnoreCase("removeAllShardEnds")){

                    logger.info("Removing shard end from shard: {}", shard);

                    shard.setShardEnd(Optional.absent());
                    edgeShardSerialization.writeShardMeta(applicationScope, shard, directedEdgeMeta).execute();

                } else if ( repairTask.equalsIgnoreCase("resetAllCompactionStatus")){

                    logger.info("Setting compacted=false for shard: {}", shard);

                    shard.setCompacted(false);
                    edgeShardSerialization.writeShardMeta(applicationScope, shard, directedEdgeMeta).execute();

                }

                firstShard = false;


            }

        }


        if(repair) {
            // do a final walk-through so changes can be verified
            Iterator<Shard> finalshards = edgeShardSerialization.getShardMetaData(applicationScope, Optional.absent(), directedEdgeMeta);

            while (finalshards.hasNext()) {

                Shard shard = finalshards.next();

                logger.info("Shard after repair: {}", shard);

            }
        }


        logger.info("ShardManager run complete");


    }
}
