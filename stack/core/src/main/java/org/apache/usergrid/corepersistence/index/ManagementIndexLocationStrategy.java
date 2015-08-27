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

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.guicyfig.ClusterFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;

/**
 * Strategy for getting the management index name
 */
class ManagementIndexLocationStrategy implements IndexLocationStrategy {
    private final String indexName;
    private final ClusterFig clusterFig;
    private final CassandraFig cassandraFig;
    private final IndexFig indexFig;
    private final CoreIndexFig coreIndexFig;
    private final IndexAlias alias;
    private final ApplicationScope applicationScope;

    public ManagementIndexLocationStrategy(final ClusterFig clusterFig, CassandraFig cassandraFig, final IndexFig indexFig, final CoreIndexFig coreIndexFig){
        this.clusterFig = clusterFig;
        this.cassandraFig = cassandraFig;
        this.indexFig = indexFig;
        this.coreIndexFig = coreIndexFig;
        this.applicationScope = CpNamingUtils.getApplicationScope( CpNamingUtils.getManagementApplicationId().getUuid());
        //use lowercase values
        this.indexName = clusterFig.getClusterName().toLowerCase() + "_" +
                         cassandraFig.getApplicationKeyspace().toLowerCase() + "_" +
                         coreIndexFig.getManagementAppIndexName().toLowerCase();
        this.alias = new ManagementIndexAlias(indexFig,indexName);
    }
    @Override
    public IndexAlias getAlias() {
        return alias;
    }

    @Override
    public String getIndexRootName() {
        return indexName;
    }
    @Override
    public String getIndexInitialName() {
        return getIndexRootName();
    }
    @Override
    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }

    @Override
    public int getNumberOfShards() {
        return coreIndexFig.getManagementNumberOfShards();
    }

    @Override
    public int getNumberOfReplicas() {
        return coreIndexFig.getManagementNumberOfReplicas();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ManagementIndexLocationStrategy that = (ManagementIndexLocationStrategy) o;

        if (!applicationScope.equals(that.applicationScope)) return false;
        return indexName.equals(that.indexName);

    }

    @Override
    public int hashCode() {
        int result = applicationScope.hashCode();
        result = 31 * result + indexName.hashCode();
        return result;
    }

    public class ManagementIndexAlias implements IndexAlias{

        private final String readAlias;
        private final String writeAlias;

        /**
         *
         * @param indexFig config
         * @param aliasPrefix alias prefix, e.g. app_id etc..
         */
        public ManagementIndexAlias(IndexFig indexFig,String aliasPrefix) {
            this.writeAlias = aliasPrefix + "_write_" + indexFig.getAliasPostfix();
            this.readAlias = aliasPrefix + "_read_" + indexFig.getAliasPostfix();
        }

        public String getReadAlias() {
            return readAlias;
        }

        public String getWriteAlias() {
            return writeAlias;
        }
    }
}
