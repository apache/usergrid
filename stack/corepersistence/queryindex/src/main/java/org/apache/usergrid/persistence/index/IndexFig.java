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

import org.apache.usergrid.persistence.index.impl.EsProvider;


@FigSingleton
public interface IndexFig extends GuicyFig {

    String ELASTICSEARCH_HOSTS = "elasticsearch.hosts";

    String ELASTICSEARCH_PORT = "elasticsearch.port";

    String ELASTICSEARCH_CLUSTER_NAME = "elasticsearch.cluster_name";

    String ELASTICSEARCH_NODENAME = "elasticsearch.node_name";

    String ELASTICSEARCH_INDEX_PREFIX = "elasticsearch.index_prefix";

    String ELASTICSEARCH_ALIAS_POSTFIX = "elasticsearch.alias_postfix";

    String ELASTICSEARCH_STARTUP = "elasticsearch.startup";

    String ELASTICSEARCH_NUMBER_OF_SHARDS = "elasticsearch.number_shards";

    String ELASTICSEARCH_NUMBER_OF_REPLICAS = "elasticsearch.number_replicas";

    String QUERY_CURSOR_TIMEOUT_MINUTES = "elasticsearch.cursor_timeout.minutes";

    String ELASTICSEARCH_FORCE_REFRESH = "elasticsearch.force_refresh";

    String INDEX_BUFFER_SIZE = "elasticsearch.buffer_size";

    String INDEX_BUFFER_TIMEOUT = "elasticsearch.buffer_timeout";

    String INDEX_BATCH_SIZE = "elasticsearch.batch_size";

    String INDEX_WRITE_CONSISTENCY_LEVEL = "elasticsearch.write_consistency_level";

    /**
     * The number of worker threads to flush collapsed batches
     */
    String INDEX_FLUSH_WORKER_COUNT = "index.flush.workers";

    /**
     * the number of times we can fail before we refresh the client
     */
    String ELASTICSEARCH_FAIL_REFRESH = "elasticsearch.fail_refresh";

    String QUERY_LIMIT_DEFAULT = "index.query.limit.default";




    /**
     * Timeout calls to elasticsearch.
     * @return
     */
    String ELASTICSEARCH_QUERY_TIMEOUT = "elasticsearch.query.timeout";

    String ELASTICSEARCH_WRITE_TIMEOUT= "elasticsearch.write.timeout";

    /**
     * The client type to use.  Valid values are NODE or TRANSPORT
     */
    String ELASTICSEARCH_CLIENT_TYPE = "elasticsearch.client.type";



    @Default( "127.0.0.1" )
    @Key( ELASTICSEARCH_HOSTS )
    String getHosts();

    @Default( "9300" )
    @Key( ELASTICSEARCH_PORT )
    int getPort();

    @Default( "usergrid" )
    @Key( ELASTICSEARCH_CLUSTER_NAME )
    String getClusterName();

    @Default( "usergrid" ) // no underbars allowed
    @Key( ELASTICSEARCH_INDEX_PREFIX )
    String getIndexPrefix();

    @Default( "alias" ) // no underbars allowed
    @Key( ELASTICSEARCH_ALIAS_POSTFIX )
    String getAliasPostfix();

    @Default( "2" ) // TODO: does this timeout get extended on each query?
    @Key( QUERY_CURSOR_TIMEOUT_MINUTES )
    int getQueryCursorTimeout();

    /** How to start ElasticSearch, may be embedded, forked or remote. */
    @Default( "remote" )
    @Key( ELASTICSEARCH_STARTUP )
    String getStartUp();

    @Default( "10" )
    @Key( QUERY_LIMIT_DEFAULT )
    int getQueryLimitDefault();

    @Default( "false" )
    @Key( ELASTICSEARCH_FORCE_REFRESH )
    boolean isForcedRefresh();

    /** Identify the client node with a unique name. */
    @Default( "default" )
    @Key( ELASTICSEARCH_NODENAME )
    String getNodeName();

    @Default( "6" )
    @Key( ELASTICSEARCH_NUMBER_OF_SHARDS )
    int getNumberOfShards();

    @Default( "1" )
    @Key( ELASTICSEARCH_NUMBER_OF_REPLICAS )
    int getNumberOfReplicas();

    @Default( "20" )
    @Key( ELASTICSEARCH_FAIL_REFRESH )
    int getFailRefreshCount();

    @Default( "2" )
    int getIndexCacheMaxWorkers();

    /**
     * how long to wait before the buffer flushes to send
     */
    @Default( "250" )
    @Key( INDEX_BUFFER_TIMEOUT )
    long getIndexBufferTimeout();

    /**
     * size of the buffer to build up before you send results
     */
    @Default( "1000" )
    @Key( INDEX_BUFFER_SIZE )
    int getIndexBufferSize();

    @Default("10")
    @Key(INDEX_FLUSH_WORKER_COUNT)
    int getIndexFlushWorkerCount();



    /**
     * Request batch size for ES
     */
    @Default( "1000" )
    @Key( INDEX_BATCH_SIZE )
    int getIndexBatchSize();

    @Default( "one" )
    @Key( INDEX_WRITE_CONSISTENCY_LEVEL )
    String getWriteConsistencyLevel();

    /**
     * Return the type of client.  Valid values or NODE or TRANSPORT
     * @return
     */
    @Key( ELASTICSEARCH_CLIENT_TYPE )
    @Default( "NODE")
    String getClientType();

    @Key("elasticsearch.refresh_search_max")
    @Default("25")
    int maxRefreshSearches();

    @Key("elasticsearch.refresh_sleep_ms")
    @Default("100")
    long refreshSleep();

    @Default( "5000" )
    @Key( ELASTICSEARCH_QUERY_TIMEOUT )
    long getQueryTimeout();

    @Default( "5000" )
    @Key( ELASTICSEARCH_WRITE_TIMEOUT )
    long getWriteTimeout();

}
