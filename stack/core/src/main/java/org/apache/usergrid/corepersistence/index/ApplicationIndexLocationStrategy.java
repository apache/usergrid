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

import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.utils.StringUtils;

/**
 * Strategy for getting the application index name.
 */
class ApplicationIndexLocationStrategy implements IndexLocationStrategy {
    private final CassandraFig cassandraFig;
    private final IndexFig indexFig;
    private final ApplicationScope applicationScope;
    private final String prefix;
    private final IndexAlias alias;

    public ApplicationIndexLocationStrategy(final CassandraFig cassandraFig,
                                            final IndexFig indexFig,
                                            final ApplicationScope applicationScope){

        this.cassandraFig = cassandraFig;
        this.indexFig = indexFig;
        this.applicationScope = applicationScope;
        this.prefix = getPrefix();
        this.alias =  new ApplicationIndexAlias(indexFig, applicationScope, prefix);
    }

    private String getPrefix() {
        //TODO: add hash buckets by app scope
        //remove usergrid
        final String indexPrefixConfig = StringUtils.isNotEmpty(indexFig.getIndexPrefix())
            ? indexFig.getIndexPrefix().toLowerCase()  ////use lowercase value
            : ""; // default to something so its not null
        final String keyspaceName = cassandraFig.getApplicationKeyspace().toLowerCase();
        //check for repetition
        final boolean removePrefix = indexPrefixConfig.length()==0 || keyspaceName.contains(indexPrefixConfig) ;
        return !removePrefix
            ? indexPrefixConfig + "_" + keyspaceName
            : keyspaceName;
    }

    /**
     * Get the alias name
     * @return
     */
    @Override
    public IndexAlias getAlias() {
        //TODO: include appid

        return alias;
    }

    /**
     * Get index name, send in additional parameter to add incremental indexes
     * @param suffix
     * @return
     */
    @Override
    public String getIndex(String suffix) {
        if (suffix != null) {
            return prefix + "_" + suffix;
        } else {
            return prefix;
        }
    }


    @Override
    public String toString() {
        return "index id: "+prefix;
    }

    @Override
    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }

    @Override
    public int getNumberOfShards() {
        return indexFig.getNumberOfShards();
    }

    @Override
    public int getNumberOfReplicas() {
        return indexFig.getNumberOfReplicas();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApplicationIndexLocationStrategy that = (ApplicationIndexLocationStrategy) o;

        if (!applicationScope.equals(that.applicationScope)) return false;
        return prefix.equals(that.prefix);

    }

    @Override
    public int hashCode() {
        int result = applicationScope.hashCode();
        result = 31 * result + prefix.hashCode();
        return result;
    }

    public class ApplicationIndexAlias implements IndexAlias {

        private final String readAlias;
        private final String writeAlias;

        /**
         *
         * @param indexFig config
         * @param aliasPrefix alias prefix, e.g. app_id etc..
         */
        public ApplicationIndexAlias(IndexFig indexFig, ApplicationScope applicationScope, String aliasPrefix) {
            this.writeAlias = aliasPrefix + "_" + applicationScope.getApplication().getUuid() + "_write_" + indexFig.getAliasPostfix();
            this.readAlias = aliasPrefix + "_" +  applicationScope.getApplication().getUuid() + "_read_" + indexFig.getAliasPostfix();
        }

        public String getReadAlias() {
            return readAlias;
        }

        public String getWriteAlias() {
            return writeAlias;
        }
    }
}
