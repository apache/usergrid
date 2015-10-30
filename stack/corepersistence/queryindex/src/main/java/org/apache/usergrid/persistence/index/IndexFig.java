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
package org.apache.usergrid.persistence.index;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


@FigSingleton
public interface IndexFig extends GuicyFig {

    String ELASTICSEARCH_HOSTS = "elasticsearch.hosts";

    String ELASTICSEARCH_PORT = "elasticsearch.port";

    String ELASTICSEARCH_CLUSTER_NAME = "elasticsearch.cluster_name";

    String ELASTICSEARCH_NODENAME = "elasticsearch.node_name";

    String ELASTICSEARCH_ALIAS_POSTFIX = "elasticsearch.alias_postfix";

    String ELASTICSEARCH_STARTUP = "elasticsearch.startup";

    String ELASTICSEARCH_NUMBER_OF_SHARDS = "elasticsearch.number_shards";

    String ELASTICSEARCH_NUMBER_OF_REPLICAS = "elasticsearch.number_replicas";

    String QUERY_CURSOR_TIMEOUT_MINUTES = "elasticsearch.cursor_timeout.minutes";

    String ELASTICSEARCH_FORCE_REFRESH = "elasticsearch.force_refresh";

    String INDEX_BATCH_SIZE = "elasticsearch.batch_size";

    String INDEX_WRITE_CONSISTENCY_LEVEL = "elasticsearch.write_consistency_level";

    String INDEX_FLUSH_WORKER_COUNT = "index.flush.workers";

    String ELASTICSEARCH_FAIL_REFRESH = "elasticsearch.fail_refresh";

    String ELASTICSEARCH_WRITE_TIMEOUT= "elasticsearch.write.timeout";

    String ELASTICSEARCH_CLIENT_TYPE = "elasticsearch.client.type";


    /**
     * Comma-separated list of Elasticsearch hosts.
     */
    @Default( "127.0.0.1" )
    @Key( ELASTICSEARCH_HOSTS )
    String getHosts();

    /**
     * The port used when connecting to Elasticsearch.
     */
    @Default( "9300" )
    @Key( ELASTICSEARCH_PORT )
    int getPort();

    /**
     * The Elasticsearch cluster name.
     */
    @Default( "usergrid" )
    @Key( ELASTICSEARCH_CLUSTER_NAME )
    String getClusterName();

    /**
     * Configurable alias name used for the Elasticsearch index.
     */
    @Default( "alias" ) // no underbars allowed
    @Key( ELASTICSEARCH_ALIAS_POSTFIX )
    String getAliasPostfix();

    /**
     * Timeout for the cursor returned with query responses.
     */
    @Default( "2" ) // TODO: does this timeout get extended on each query?
    @Key( QUERY_CURSOR_TIMEOUT_MINUTES )
    int getQueryCursorTimeout();

    /**
     * How Elasticsearch should be started.  Valid values: embedded, forked, or remote
     */
    @Default( "remote" )
    @Key( ELASTICSEARCH_STARTUP )
    String getStartUp();

    /**
     * Force an index refresh after every write. Should only be TRUE for testing purposes.
     */
    @Default( "false" )
    @Key( ELASTICSEARCH_FORCE_REFRESH )
    boolean isForcedRefresh();

    /**
     * Identify the Elasticsearch client node with a unique name.
     */
    @Default( "default" )
    @Key( ELASTICSEARCH_NODENAME )
    String getNodeName();

    /**
     * The number of primary shards to use for an index in Elasticsearch.  Typically 2x or 3x the ES nodes.
     *
     * Depending on the use case for Usergrid, these numbers may vary. Usergrid is defaulted
     * to a higher number of shards based on typical Elasticsearch clusters being >= 6 nodes.
     * You can choose how it's sharded in Elasticsearch to reach optimal indexing for your dataset.  For more
     * info about sharding, here is a good starting point:
     *  <https://www.elastic.co/guide/en/elasticsearch/guide/current/routing-value.html>
     *
     */
    @Default( "18" )
    @Key( ELASTICSEARCH_NUMBER_OF_SHARDS )
    int getNumberOfShards();

    /**
     * The number of replicas to use for the index in Elasticsearch.
     */
    @Default( "1" )
    @Key( ELASTICSEARCH_NUMBER_OF_REPLICAS )
    int getNumberOfReplicas();


    /**
     * The number of failures that occur before refreshing an Elasticsearch client.
     */
    @Default( "20" )
    @Key( ELASTICSEARCH_FAIL_REFRESH )
    int getFailRefreshCount();

    @Default( "2" )
    int getIndexCacheMaxWorkers();



    /**
     * The number of worker threads used for flushing batches of index write requests
     * in the buffer for Elasticsearch.
     */
    @Default("10")
    @Key(INDEX_FLUSH_WORKER_COUNT)
    int getIndexFlushWorkerCount();

    /**
     * The batch size to use when sending batched index write requests to Elasticsearch.
     */
    @Default( "1000" )
    @Key( INDEX_BATCH_SIZE )
    int getIndexBatchSize();

    /**
     * The write consistency level for writing into the Elasticsearch index.  The
     * default value is 'one', and you can configure 'all' and 'quorum'.
     */
    @Default( "one" )
    @Key( INDEX_WRITE_CONSISTENCY_LEVEL )
    String getWriteConsistencyLevel();

    /**
     * Return the type of Elasticsearch client.  Valid values are NODE or TRANSPORT.
     */
    @Key( ELASTICSEARCH_CLIENT_TYPE )
    @Default( "NODE")
    String getClientType();

    /**
     * The maximum number of searches that are allowed during a refresh.
     */
    @Key("elasticsearch.refresh_search_max")
    @Default("10")
    int maxRefreshSearches();

    /**
     * The timeout used when writing into the Elasticsearch index. (in milliseconds)
     */
    @Default( "5000" )
    @Key( ELASTICSEARCH_WRITE_TIMEOUT )
    long getWriteTimeout();


    @Default("1000")
    @Key( "elasticsearch_queue_error_sleep_ms" )
    long getSleepTimeForQueueError();
}
