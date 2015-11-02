/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.index.impl;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;


/**
 * Container for index operations.
 */
public class IndexOperationMessage implements Serializable {
    @JsonProperty
    private final Set<IndexOperation> indexRequests;

    @JsonProperty
    private final Set<DeIndexOperation> deIndexRequests;

    @JsonProperty
    private long creationTime;




    public IndexOperationMessage() {
        this.indexRequests = new HashSet<>();
        this.deIndexRequests = new HashSet<>();
        this.creationTime = System.currentTimeMillis();
    }


    public void addIndexRequest( final IndexOperation indexRequest ) {
        indexRequests.add( indexRequest );
    }



    public void addDeIndexRequest( final DeIndexOperation deIndexRequest ) {
        this.deIndexRequests.add( deIndexRequest );
    }



    public Set<IndexOperation> getIndexRequests() {
        return indexRequests;
    }


    public Set<DeIndexOperation> getDeIndexRequests() {
        return deIndexRequests;
    }


    @JsonIgnore
    public boolean isEmpty(){
        return indexRequests.isEmpty() && deIndexRequests.isEmpty();
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final IndexOperationMessage that = ( IndexOperationMessage ) o;

        if ( !deIndexRequests.equals( that.deIndexRequests ) ) {
            return false;
        }
        if ( !indexRequests.equals( that.indexRequests ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = indexRequests.hashCode();
        result = 31 * result + deIndexRequests.hashCode();
        return result;
    }


    public long getCreationTime() {
        return creationTime;
    }

    public void ingest(IndexOperationMessage singleMessage) {
        this.indexRequests.addAll(singleMessage.getIndexRequests().stream().collect(Collectors.toList()));
        this.deIndexRequests.addAll(singleMessage.getDeIndexRequests().stream().collect(Collectors.toList()));
    }
}
