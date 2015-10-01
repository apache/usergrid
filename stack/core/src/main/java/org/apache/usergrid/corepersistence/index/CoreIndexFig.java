/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.corepersistence.index;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;

/**
 * configuration for index in core
 */
@FigSingleton
public interface CoreIndexFig extends GuicyFig {

    String ELASTICSEARCH_MANAGEMENT_NUMBER_OF_SHARDS = "elasticsearch.management_number_shards";

    String ELASTICSEARCH_MANAGEMENT_NUMBER_OF_REPLICAS = "elasticsearch.management_number_replicas";


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
    @Key( ELASTICSEARCH_MANAGEMENT_NUMBER_OF_SHARDS )
    int getManagementNumberOfShards();

    @Default( "1" )
    @Key( ELASTICSEARCH_MANAGEMENT_NUMBER_OF_REPLICAS )
    int getManagementNumberOfReplicas();

    @Default( "management" )
    @Key( "elasticsearch.management_index" )
    String getManagementAppIndexName();

    @Default( "5" )
    @Key( "elasticsearch.index_bucket_count" )
    int getNumberOfIndexBuckets();


    //offset the bucket by a certain amount to remove older buckets from range e.g
    // if range was 1-5 offset should be 5 to remove 1-5 from set
    @Default( "0" )
    @Key( "elasticsearch.index_bucket_offset" )
    int getBucketOffset();
}
