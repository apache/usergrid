/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.index.impl.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.impl.EntityIndexMapUtils;
import org.apache.usergrid.persistence.index.impl.IndexEdgeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.EntityMap;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ESDistributionLoadTest {

    private static final Logger logger = LoggerFactory.getLogger(DebugInfo.ES_LOAD_TEST);

    private final String[] hosts;
    private final String indexName;

    private final int port;
    private final int maxThreads;
    private final int dataSize;
    private final int batchSize;
    private final int batchProcessSize;

    private final UUID managementAppUUID;
    private final String type;

    private final Client client;

    private static final List<String> JSON_LIST = new CopyOnWriteArrayList<>();
    private static final String ENTITY = "entity";

    ESDistributionLoadTest() {
        hosts = Config.getHosts();
        indexName = Config.getIndexAlias();
        port = Config.getPort();
        maxThreads = Config.getMaxThreads();
        dataSize = Config.getDataSize();
        batchSize = Config.getReadBatchSize();
        batchProcessSize = Config.getProcessBatchSize();
        managementAppUUID = Config.getManagementAppUuid();
        type = Config.getCollectionType();

        client = createClient();
        createIndexAndMappings();
    }

    public void start() {
        logger.info("Starting load...");

        final Id appId = createAppDoc();
        final File file = new File(Config.getDataFilePath());
        int poolSize = maxThreads;
        if (batchSize < maxThreads) {
            poolSize = maxThreads / batchSize;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        for (int i=0; i < dataSize; i+=batchSize) {
            JSON_LIST.clear();
            JSON_LIST.addAll(getListOfJson(file, i, batchSize));
            if (JSON_LIST.isEmpty()) break;
            System.out.println(JSON_LIST.get(0));
            processBatch(executorService, appId, batchProcessSize, poolSize);
        }
        executorService.shutdown();

        while (!executorService.isTerminated()) {}
        logger.info("Load complete");
    }

    protected Client createClient() {
        if ("transport".equalsIgnoreCase(Config.getClient())) {
            ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder().put("cluster.name", Config.getClusterName())
                    .put("client.transport.sniff", true);
            Client transportClient = new TransportClient(settings);
            for (String host : this.hosts) {
                if (!host.trim().isEmpty()) {
                    ((TransportClient) transportClient).addTransportAddress(new InetSocketTransportAddress(host.trim(), port));
                }
            }
            return transportClient;
        } else {
            StringBuilder hostString = new StringBuilder();
            for (String host : this.hosts) {
                hostString.append( host ).append( ":" ).append( port ).append( "," );
            }
            hostString.deleteCharAt(hostString.length() - 1);
            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("discovery.zen.ping.unicast.hosts", hostString)
                    .put("discovery.zen.ping.multicast.enabled", "false").put("http.enabled", false)
                    .put("client.transport.ping_timeout", 2000) // milliseconds
                    .put("client.transport.nodes_sampler_interval", 100).put("network.tcp.blocking", true)
                    .put("node.client", true).build();

            Node node = NodeBuilder.nodeBuilder().clusterName(Config.getClusterName()).settings(settings).build();
            return node.client();
        }
    }

    protected void createIndexAndMappings() {
        try {
            AdminClient adminClient = client.admin();
            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("index.number_of_shards", Config.getNumberOfShards())
                    .put("index.number_of_replicas", Config.getNumberOfReplica())

                            //dont' allow unmapped queries, and don't allow dynamic mapping
                    .put("index.query.parse.allow_unmapped_fields", false )
                    .put("index.mapper.dynamic", false)
                    .put("action.write_consistency", WriteConsistencyLevel.ONE)
                    .build();
            IndicesExistsRequestBuilder indicesExistsRequestBuilder = new IndicesExistsRequestBuilder(client.admin().indices(), indexName);
            IndicesExistsResponse indicesExistsResponse = adminClient.indices().exists(indicesExistsRequestBuilder.request()).actionGet();
            if (indicesExistsResponse.isExists()) {
                logger.info("Index {} already exists", indexName);
            } else {
                CreateIndexResponse createIndexResponse = adminClient.indices().prepareCreate(indexName)
                        .setSettings(settings)
                        .execute()
                        .actionGet();
                if (createIndexResponse.isAcknowledged()) {
                    logger.info("created index {}", indexName);
                } else {
                    logger.error("creating index {} failed!", indexName);
                }
            }
            IndicesAliasesRequestBuilder aliasesRequestBuilder = adminClient.indices().prepareAliases();
            aliasesRequestBuilder.addAlias(indexName, indexName+"_alias");
            boolean aliasAck = aliasesRequestBuilder.execute().actionGet().isAcknowledged();
            if (aliasAck) {
                logger.info("created allias {}", indexName);
            } else {
                logger.error("creating alias {} failed", indexName);
            }
            InputStream is = ESDistributionLoadTestInvoker.class.getClassLoader().getResourceAsStream("usergrid-mappings.json");
            String mappings = "";
            if (is != null) {
                mappings = IOUtils.toString(is, Charsets.UTF_8);
            } else {
                System.out.println("Unable to find usergrid-mappings!!!");
                logger.error("Unable to find usergrid-mappings!");
            }
            boolean mappingAck = adminClient.indices().preparePutMapping(indexName).setType(ENTITY)
                    .setSource(mappings).execute().get().isAcknowledged();
            if (mappingAck) {
                logger.info("created mapping for index {}", indexName);
            } else {
                logger.error("creating mapping for index {} failed", indexName);
            }
        } catch (Exception ex) {
            logger.error("Error creating index and mapping. Exiting...", ex);
            System.out.println("Error creating index and mapping. Exiting..." + ex.getMessage());
            System.exit(1);
        }
    }

    protected Id createAppDoc() {
        DocIdGenerator docIdGenerator = new DocIdGenerator(managementAppUUID);
        UUID appUUID = UUIDGenerator.newTimeUUID();
        long timestamp = System.currentTimeMillis();
        List<String> appDocIds = docIdGenerator.getAppEdgeDocIds(appUUID, appUUID, timestamp);
        for (int i = 0; i < 3; i++) {
            appDocIds.add(docIdGenerator.getRoleDocIdForApp(appUUID, timestamp));
        }
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk().setConsistencyLevel(WriteConsistencyLevel.ONE);
        for (String docId : appDocIds) {
            IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName, ENTITY, docId).setSource("");
            bulkRequestBuilder.add(indexRequestBuilder);
        }
        bulkRequestBuilder.execute().actionGet();
        return new SimpleId(appUUID, DocIdGenerator.APPLICATION);
    }

    protected Map.Entry<String, UUID> createCollectionEntityDocId(UUID appUUID, String type) {
        DocIdGenerator docIdGenerator = new DocIdGenerator(managementAppUUID);
        long timestamp = System.currentTimeMillis();
        return docIdGenerator.getCollectionEntityIdForApp(appUUID, type, timestamp);
    }

    protected List<String> getListOfJson(File file, int start, int size) {
        if (start < 0 || size < 0) {
            throw new IllegalArgumentException("invalid start or size");
        }
        System.out.println("Fetching from "+start+ "to "+(start+size));
        LineIterator lineIterator = null;
        List<String> jsonList = new ArrayList<>(size);
        int lines = 0;
        int retrieved = 0;
        try {
            lineIterator = FileUtils.lineIterator(file, "UTF-8");
            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                if (lines >= start) {
                    jsonList.add(line);
                    retrieved++;
                }
                if (retrieved >= size) {
                    break;
                }
                lines++;
            }
        } catch (IOException ex) {
            logger.error("Error reading file {} - {}", file.getName(), ex.getMessage());
        } finally {
            LineIterator.closeQuietly(lineIterator);
        }
        return jsonList;
    }

    protected void processBatch(final ExecutorService executorService, final Id appId, final int batchSize, int poolSize) {
        int done = 0;
        while (done < JSON_LIST.size()) {
            List<Future> futures = new ArrayList<>(poolSize);
            BatchProcessor[] batchProcessors = new BatchProcessor[poolSize];
            for (int i = 0; i < poolSize; i++) {
                batchProcessors[i] = new BatchProcessor(appId, done, batchSize);
                Future f = executorService.submit(batchProcessors[i]);
                futures.add(f);
                done+=batchSize;
            }
            for (Future future : futures) {
                try {
                    future.get();
                } catch (ExecutionException | InterruptedException ex) {
                    System.out.println("execution failed!");
                }
            }
            /*try {
                //Thread.sleep(2000);
            } catch (InterruptedException ex) {

            }*/
        }
    }

    class BatchProcessor extends Thread {

        private int start, size;
        private Id appId;

        BatchProcessor(Id appId, int start, int size) {
            this.appId = appId;
            this.start = start;
            this.size = size;
        }

        @Override
        public void run() {
            long timestamp = System.currentTimeMillis();
            final BulkRequestBuilder bulkRequestBuilder = client.prepareBulk().setConsistencyLevel(WriteConsistencyLevel.ONE);
            int bulkSize = 0;
            for (int i=start; i<(start+size); i++) {
                if (i >= JSON_LIST.size()) break;
                Map.Entry<String, UUID> entry = createCollectionEntityDocId(appId.getUuid(), type);
                String jsonObject = ESDistributionLoadTest.JSON_LIST.get(i);
                Map<String, Object> json;
                try {
                    json = EntityIndexMapUtils.objectMapper.readValue(jsonObject, new TypeReference<HashMap<String, Object>>() {
                    });
                    /* json.get("name");
                    * how do we find the entity uuid given a name and collection type? we do not have c*. Not sure how to search fields column but
                    * even if we could search, then it is extra load on ES and wouldn't reflect what UG is trying to do. So we create new entity all the time.
                    * */
                } catch (Exception ex) {
                    logger.error("Error converting json {} to map, invalid json? skipping...", jsonObject);
                    continue;
                }
                EntityMap entityMap = new EntityMap(new SimpleId(entry.getValue(), type), entry.getValue());
                Entity cpEntity = Entity.fromMap(entityMap);
                cpEntity = EntityIndexMapUtils.fromMap(cpEntity, json);
                Map<String, Object> dataMap = EntityIndexMapUtils.toSourceMap(new ApplicationScopeImpl(appId),
                        new IndexEdgeImpl(cpEntity.getId(), cpEntity.getId().getUuid().toString(), SearchEdge.NodeType.SOURCE, timestamp), cpEntity);
                IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName, ENTITY, entry.getKey()).setSource(dataMap);
                bulkRequestBuilder.add(indexRequestBuilder);
                bulkSize++;
            }
            try {
                BulkResponse bulkResponse = bulkRequestBuilder.execute().get();
                if (bulkResponse.hasFailures()) {
                    logger.info("batch has failures {}", bulkResponse.buildFailureMessage());
                    for (BulkItemResponse bulkItemResponse : bulkResponse.getItems()) {
                        if (bulkItemResponse.isFailed()) {
                            logger.error("Request failed for json {} with {}", bulkItemResponse.getId(), bulkItemResponse.getFailureMessage());
                        }
                    }
                } else {
                    logger.info("Batch {} completed OK", bulkSize);
                }
            } catch (InterruptedException | ExecutionException ex) {

            }
        }

        @Override
        public String toString() {
            return "BatchProcessor{" +
                    "start=" + start +
                    ", size=" + size +
                    ", appId=" + appId +
                    '}';
        }
    }
}
