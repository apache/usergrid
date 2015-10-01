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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;

/**
 * Strategy to replicate an index naming convention and publish elsewhere
 */
public class ReplicatedIndexLocationStrategy implements IndexLocationStrategy {

    private IndexAlias alias;
    private String rootName;
    private String indexInitialName;
    private ApplicationScope applicationScope;
    private int numberShards;
    private int numberReplicas;

    public ReplicatedIndexLocationStrategy(){

    }

    public ReplicatedIndexLocationStrategy(IndexLocationStrategy indexLocationStrategy){
        rootName = indexLocationStrategy.getIndexRootName();
        indexInitialName = indexLocationStrategy.getIndexInitialName();
        applicationScope = indexLocationStrategy.getApplicationScope();
        numberShards = indexLocationStrategy.getNumberOfShards();
        numberReplicas = indexLocationStrategy.getNumberOfReplicas();
        alias = new ReplicatedIndexAlias( indexLocationStrategy.getAlias() );
    }

    @Override
    @JsonSerialize()
    @JsonDeserialize(as=ReplicatedIndexAlias.class)
    public IndexAlias getAlias() {
        return alias;
    }

    protected void setAlias(IndexAlias alias) {
        this.alias = alias;
    }


    @Override
    @JsonSerialize()
    public String getIndexRootName() {
        return rootName;
    }
    protected void setIndexRootName(String indexRootName) {
        this.rootName = indexRootName;
    }

    @Override
    @JsonSerialize()
    public String getIndexInitialName() {
        return indexInitialName;
    }

    protected void setIndexInitialName(String indexInitialName) {
        this.indexInitialName = indexInitialName;
    }


    @Override
    @JsonSerialize()
    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }

    protected void setApplicationScope(ApplicationScope applicationScope) {
        this.applicationScope = applicationScope;
    }


    @Override
    @JsonSerialize()
    public int getNumberOfShards() {
        return numberShards;
    }
    public void setNumberOfShards(int shards) {
        numberShards = shards;
    }
    @Override
    @JsonSerialize()
    public int getNumberOfReplicas() {
        return numberReplicas;
    }

    public void setNumberOfReplicas(int replicas) {
        numberReplicas = replicas;
    }
    public static class ReplicatedIndexAlias implements IndexAlias{

        private String readAlias;
        private String writeAlias;

        public ReplicatedIndexAlias(){

        }
        public ReplicatedIndexAlias(IndexAlias alias){
            this.readAlias = alias.getReadAlias();
            this.writeAlias = alias.getWriteAlias();
        }
        @Override
        @JsonSerialize()
        public String getReadAlias() {
            return readAlias;
        }

        @Override
        @JsonSerialize()
        public String getWriteAlias() {
            return writeAlias;
        }
    }
}
