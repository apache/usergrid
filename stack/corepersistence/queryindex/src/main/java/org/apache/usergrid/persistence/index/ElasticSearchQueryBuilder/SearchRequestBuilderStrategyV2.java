/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index.ElasticSearchQueryBuilder;


import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.impl.EsProvider;
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.impl.IndexingUtils;


/**
 * The class to use when creating new custom queries for specific terms to elasticsearch.
 */
public class SearchRequestBuilderStrategyV2 {
    private static final Logger logger = LoggerFactory.getLogger( SearchRequestBuilderStrategyV2.class );

    private final EsProvider esProvider;
    private final ApplicationScope applicationScope;
    private final IndexAlias alias;
    private final int cursorTimeout;


    public SearchRequestBuilderStrategyV2( final EsProvider esProvider, final ApplicationScope applicationScope,
                                         final IndexAlias alias, int cursorTimeout ) {

        this.esProvider = esProvider;
        this.applicationScope = applicationScope;
        this.alias = alias;
        this.cursorTimeout = cursorTimeout;
    }

    public SearchRequestBuilder getBuilder(){
        SearchRequestBuilder srb =
            esProvider.getClient().prepareSearch( alias.getReadAlias() ).setTypes( IndexingUtils.ES_ENTITY_TYPE ).setSearchType(
                SearchType.QUERY_THEN_FETCH);


        return srb;
    }

    public SearchScrollRequestBuilder getScrollBuilder(String scrollId){
        return esProvider.getClient().prepareSearchScroll( scrollId );
    }

}
