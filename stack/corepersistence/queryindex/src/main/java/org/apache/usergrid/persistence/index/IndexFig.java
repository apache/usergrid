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

    public static final String ELASTICSEARCH_HOSTS = "elasticsearch.hosts";

    public static final String ELASTICSEARCH_PORT = "elasticsearch.port";

    public static final String ELASTICSEARCH_CLUSTER_NAME = "elasticsearch.cluster_name";

    public static final String ELASTICSEARCH_NODENAME = "elasticsearch.node_name";

    public static final String ELASTICSEARCH_INDEX_PREFIX = "elasticsearch.index_prefix";

    public static final String ELASTICSEARCH_ALIAS_POSTFIX = "elasticsearch.alias_postfix";

    public static final String ELASTICSEARCH_STARTUP = "elasticsearch.startup";

    public static final String ELASTICSEARCH_NUMBER_OF_SHARDS = "elasticsearch.number_shards";

    public static final String ELASTICSEARCH_NUMBER_OF_REPLICAS = "elasticsearch.number_replicas";

    public static final String QUERY_CURSOR_TIMEOUT_MINUTES = "elasticsearch.cursor_timeout.minutes";

    public static final String ELASTICSEARCH_FORCE_REFRESH = "elasticsearch.force_refresh";

    public static final String INDEX_BUFFER_SIZE = "elasticsearch.buffer_size";

    public static final String INDEX_BUFFER_TIMEOUT = "elasticsearch.buffer_size";

    public static final String INDEX_BATCH_SIZE = "elasticsearch.batch_size";

    public static final String INDEX_WRITE_CONSISTENCY_LEVEL = "elasticsearch.write_consistency_level";

    /**
     * the number of times we can fail before we refresh the client
     */
    public static final String ELASTICSEARCH_FAIL_REFRESH = "elasticsearch.fail_refresh";

    public static final String QUERY_LIMIT_DEFAULT = "index.query.limit.default";

    @Default( "127.0.0.1" )
    @Key( ELASTICSEARCH_HOSTS )
    String getHosts();

    @Default( "9300" )
    @Key( ELASTICSEARCH_PORT )
    int getPort();

    @Default( "usergrid" )
    @Key( ELASTICSEARCH_CLUSTER_NAME)
    String getClusterName();

    @Default( "usergrid" ) // no underbars allowed
    @Key( ELASTICSEARCH_INDEX_PREFIX )
    String getIndexPrefix();

    @Default( "alias" ) // no underbars allowed
    @Key( ELASTICSEARCH_ALIAS_POSTFIX )
    String getAliasPostfix();

    @Default( "1" ) // TODO: does this timeout get extended on each query?
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
    public boolean isForcedRefresh();

    /** Identify the client node with a unique name. */
    @Default("default")
    @Key( ELASTICSEARCH_NODENAME )
    public String getNodeName();

    @Default("6")
    @Key( ELASTICSEARCH_NUMBER_OF_SHARDS )
    public int getNumberOfShards();

    @Default("1")
    @Key( ELASTICSEARCH_NUMBER_OF_REPLICAS )
    public int getNumberOfReplicas();

    @Default( "20" )
    @Key( ELASTICSEARCH_FAIL_REFRESH )
    int getFailRefreshCount();

    @Default("2")
    int getIndexCacheMaxWorkers();

    /**
     * how long to wait before the buffer flushes to send
     * @return
     */
    @Default("250")
    @Key( INDEX_BUFFER_TIMEOUT )
    int getIndexBufferTimeout();

    /**
     * size of the buffer to build up before you send results
     * @return
     */
    @Default("1000")
    @Key( INDEX_BUFFER_SIZE )
    int getIndexBufferSize();

    /**
     * Request batch size for ES
     * @return
     */
    @Default("1000")
    @Key( INDEX_BATCH_SIZE)
    int getIndexBatchSize();

    @Default("one")
    @Key( INDEX_WRITE_CONSISTENCY_LEVEL )
    String getWriteConsistencyLevel();
}
