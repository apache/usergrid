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

    public static final String ELASTICSEARCH_INDEXNAME_PREFIX = "elasticsearch.index_name.prefix";

    public static final String ELASTICSEARCH_EMBEDDED = "elasticsearch.embedded";

    public static final String QUERY_CURSOR_TIMEOUT_MINUTES = "elasticsearch.cursor_timeout.minutes";

    public static final String ELASTICSEARCH_FORCE_REFRESH = "elasticsearch.force_refresh";

    public static final String QUERY_LIMIT_DEFAULT = "index.query.limit.default";
    
    @Default( "127.0.0.1" )
    @Key( ELASTICSEARCH_HOSTS )
    String getHosts();

    @Default( "9200" )
    @Key( ELASTICSEARCH_PORT )
    int getPort();

    @Default( "usergrid_" )
    @Key( ELASTICSEARCH_INDEXNAME_PREFIX )
    String getIndexNamePrefix();
    
    @Default( "1" ) // TODO: does this timeout get extended on each query?
    @Key( QUERY_CURSOR_TIMEOUT_MINUTES )
    int getQueryCursorTimeout();

    @Default( "false" )
    @Key( ELASTICSEARCH_EMBEDDED )
    boolean isEmbedded();

    @Default( "10" )
    @Key( QUERY_LIMIT_DEFAULT )
    int getQueryLimitDefault();

    @Default( "false" ) 
    @Key( ELASTICSEARCH_FORCE_REFRESH )
    public boolean isForcedRefresh();
}
