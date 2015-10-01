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
package org.apache.usergrid.persistence.index;

import java.io.Serializable;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
/**
 * location strategy for index
  Naming Configuration
 ---
 clusterName = config{usergrid.cluster_name}
 keyspaceName = config{cassandra.keyspace.application}
 managementName = config{elasticsearch.managment_index}
 indexRoot = {clusterName}_{keyspaceName}
 managementIndexName = {indexRoot}_{managementName}
 managementAliasName = {indexRoot}_{managementName}_read_alias || {indexRoot}_{managementName}_read_alias
 applicationIndexName = {indexRoot}_applications_{bucketId}
 applicationAliasName = {indexRoot}_{appId}_read_alias || {indexRoot}_{appId}_write_alias
 */
public interface IndexLocationStrategy extends Serializable {
    /**
     * get the alias name
     * @return
     */
    IndexAlias getAlias();

    /**
     * get index name
     * @return
     */
    String getIndexRootName();


    /**
     * get the initial index name, to create the first instance of the index
     * @return
     */
     String getIndexInitialName() ;

    /**
     * return unique string
     * @return
     */
    String toString();

    /**
     * only used by search types
     * @return
     */
    ApplicationScope getApplicationScope();

    /**
     * number of shards for default
     * @return
     */
    int getNumberOfShards();

    /**
     * default replicas
     * @return
     */
    int getNumberOfReplicas();
}
