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


import com.google.common.base.Optional;
import com.netflix.astyanax.MutationBatch;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.service.CollectionService;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.impl.EsProvider;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.InflectionUtils;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.*;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createSearchEdgeFromSource;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.createIndexDocId;


public class EntityVersionAudit extends ToolBase {

    /*

        Writes to files in the current directory:
            entity_es_urls.txt (contains the relative URLs for the elasticsearch API to GET a document for an entity and version
            entity_version_agg.txt ( contains the number of versions per entity on a line)
     */

    private static final Logger logger = LoggerFactory.getLogger( EntityVersionAudit.class );

    private static final String APPLICATION_ARG = "app";

    private static final String ENTITY_TYPE_ARG = "entityType";

    private static final String USE_LATEST_VERSION_ARG = "useLatestVersion";

    private static final String ENTITY_UUID = "entityUUID";


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


        Option useLatestVersion =
            OptionBuilder.withArgName(USE_LATEST_VERSION_ARG).hasArg().isRequired( false ).withDescription( "use latest version" )
                .create(USE_LATEST_VERSION_ARG);

        options.addOption( useLatestVersion );

        Option entityUUID =
            OptionBuilder.withArgName(ENTITY_UUID).hasArg().isRequired( false ).withDescription( "specific entity uuid" )
                .create(ENTITY_UUID);

        options.addOption( entityUUID );



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

        logger.info("Starting Tool: EntityVersionAudit");
        logger.info("Using Cassandra consistency level: {}", System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM"));

        startSpring();

        String applicationOption = line.getOptionValue(APPLICATION_ARG);
        String entityTypeOption = line.getOptionValue(ENTITY_TYPE_ARG);

        if (isBlank(applicationOption)) {
            throw new RuntimeException("Application ID not provided.");
        }
        final UUID app = UUID.fromString(line.getOptionValue(APPLICATION_ARG));

        if (isBlank(entityTypeOption)) {
            throw new RuntimeException("Entity type (singular collection name) not provided.");
        }
        String entityType = entityTypeOption;


        boolean useLatestVersion =
            line.getOptionValue(USE_LATEST_VERSION_ARG) != null && line.getOptionValue(USE_LATEST_VERSION_ARG).equalsIgnoreCase("true");
        logger.info("useLatestVersion {}", useLatestVersion);

        final String entityUUID = line.getOptionValue(ENTITY_UUID);
        logger.info("entityUUID {}", entityUUID);


        em = emf.getEntityManager( app );

        String collectionName = InflectionUtils.pluralize(entityType);
        String simpleEdgeType = CpNamingUtils.getEdgeTypeFromCollectionName(collectionName);
        logger.info("simpleEdgeType: {}", simpleEdgeType);

        ApplicationScope applicationScope = new ApplicationScopeImpl(new SimpleId(app, "application"));
        Id applicationScopeId = applicationScope.getApplication();
        logger.info("applicationScope.getApplication(): {}", applicationScopeId);

        GraphManagerFactory gmf = injector.getInstance( GraphManagerFactory.class );
        GraphManager gm = gmf.createEdgeManager(applicationScope);

        EntityCollectionManagerFactory emf = injector.getInstance( EntityCollectionManagerFactory.class );
        EntityCollectionManager ecm = emf.createCollectionManager(applicationScope);

        final SimpleSearchByEdgeType search =
            new SimpleSearchByEdgeType( applicationScopeId, simpleEdgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                Optional.absent(), false );

        final IndexLocationStrategyFactory ilsf = injector.getInstance(IndexLocationStrategyFactory.class);
        final Writer versionAuditWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("entity_version_audit.txt"), "utf-8"));
        final Writer versionAggWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("entity_version_agg.txt"), "utf-8"));

        versionAuditWriter.write("collection,entityUUID,entityVersion,cassandraTimestamp,elasticsearchTimestamp,indexDelayMillis,existsInElasticsearch\n");
        versionAuditWriter.flush();

        final EsProvider esProvider = injector.getInstance(EsProvider.class);

        gm.loadEdgesFromSource(search).map(markedEdge -> {

            UUID uuid = markedEdge.getTargetNode().getUuid();

            if (entityUUID == null || uuid.equals(UUID.fromString(entityUUID))){
                logger.info("matched uuid: {}", uuid);
                try {
                    EntityRef entityRef = new SimpleEntityRef(entityType, uuid);
                    org.apache.usergrid.persistence.Entity retrieved = em.get(entityRef);

                    if ( retrieved != null ){

                        final AtomicInteger versionCount = new AtomicInteger();
                        Observable<MvccLogEntry> versionObs = ecm.getVersionsFromMaxToMin( retrieved.asId(), org.apache.usergrid.utils.UUIDUtils.newTimeUUID() );
                        if (useLatestVersion) {
                            versionObs = versionObs.take(1);
                        }
                        versionObs.forEach( mvccLogEntry -> {

                            IndexLocationStrategy strategy = ilsf.getIndexLocationStrategy(applicationScope);
                            final String readAlias = strategy.getAlias().getReadAlias();

                            final SearchEdge searchEdge = createSearchEdgeFromSource( new SimpleEdge( applicationScope.getApplication(),
                                CpNamingUtils.getEdgeTypeFromCollectionName( InflectionUtils.pluralize( retrieved.asId().getType() ) ), retrieved.asId(),
                                Long.MAX_VALUE ) );

                            final String esDocId = createIndexDocId( applicationScope, retrieved.asId(), mvccLogEntry.getVersion(), searchEdge);
                            GetResponse response =  esProvider.getClient().prepareGet(readAlias, "entity", esDocId)
                                .execute()
                                .actionGet();
                            boolean exists = response.isExists();

                            long indexTimestamp = response.getField("_timestamp") == null ? 0 : (long)response.getField("_timestamp").getValue();
                            long uuidTimestamp = UUIDUtils.getTimestampInMillis(retrieved.getUuid());

                            long diff = 0;
                            if (indexTimestamp > 0) {
                                diff = uuidTimestamp = indexTimestamp;
                            }

                            try {

                                String csvLine =
                                    collectionName + "," +
                                    uuid + "," +
                                    mvccLogEntry.getVersion() + "," +
                                    uuidTimestamp + "," +
                                    indexTimestamp + "," +
                                    diff + "," +
                                    exists;

                                //final String url = "/"+readAlias+"/entity/"+URLEncoder.encode(esDocId, "UTF-8");
                                versionAuditWriter.write(csvLine+"\n");
                                versionAuditWriter.flush();
                                versionCount.incrementAndGet();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });

                        versionAggWriter.write(versionCount.toString()+","+retrieved.asId().getUuid()+"\n");
                        versionAggWriter.flush();

                    }else{
                        logger.info("entity: {} NOT FOUND", uuid);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

           return markedEdge;
        }).toBlocking().lastOrDefault(null);

        versionAuditWriter.close();
        versionAggWriter.close();

    }
}
