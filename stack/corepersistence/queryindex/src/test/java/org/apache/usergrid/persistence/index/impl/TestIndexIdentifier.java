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
package org.apache.usergrid.persistence.index.impl;


import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;


/**
 * Class is used to generate an index name and alias name
 */
public class TestIndexIdentifier implements IndexLocationStrategy {
    private final CassandraFig cassandraFig;
    private final IndexFig indexFig;
    private final ApplicationScope applicationScope;
    private final String prefix;

    public TestIndexIdentifier(final CassandraFig cassandraFig, final IndexFig config, final ApplicationScope applicationScope) {
        this.cassandraFig = cassandraFig;
        this.indexFig = config;
        this.applicationScope = applicationScope;
        this.prefix = getPrefix();
    }

    private String getPrefix() {
        //remove usergrid

        final String keyspaceName = cassandraFig.getApplicationKeyspace().toLowerCase();
        //check for repetition
        return   keyspaceName;
    }
    @Override
    public String getIndexInitialName() {
        return getIndexRootName();
    }

    /**
     * Get the alias name
     * @return
     */
    @Override
    public IndexAlias getAlias() {
        return new TestIndexAlias(indexFig,prefix);
    }

    /**
     * Get index name, send in additional parameter to add incremental indexes
     * @return
     */
    @Override
    public String getIndexRootName() {

        return prefix;

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

    private class TestIndexAlias implements IndexAlias {
        private final String readAlias;
        private final String writeAlias;

        /**
         *
         * @param indexFig config
         * @param aliasPrefix alias prefix, e.g. app_id etc..
         */
        public TestIndexAlias(IndexFig indexFig,String aliasPrefix) {
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
