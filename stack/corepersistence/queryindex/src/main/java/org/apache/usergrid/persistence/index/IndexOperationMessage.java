/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.index;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.index.impl.DeIndexRequest;
import org.apache.usergrid.persistence.index.impl.IndexRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * Container for index operations.
 */
public class IndexOperationMessage implements Serializable {
    private final Set<IndexRequest> indexRequests;
    private final Set<DeIndexRequest> deIndexRequests;



    private final BetterFuture<IndexOperationMessage> containerFuture;


    public IndexOperationMessage() {
        final IndexOperationMessage parent = this;
        this.indexRequests = new HashSet<>();
        this.deIndexRequests = new HashSet<>();
        this.containerFuture = new BetterFuture<>( new Callable<IndexOperationMessage>() {
            @Override
            public IndexOperationMessage call() throws Exception {
                return parent;
            }
        } );
    }


    public void addIndexRequest( final IndexRequest indexRequest ) {
        indexRequests.add( indexRequest );
    }


    public void addAllIndexRequest( final Set<IndexRequest> indexRequests ) {
        this.indexRequests.addAll( indexRequests );
    }


    public void addDeIndexRequest( final DeIndexRequest deIndexRequest ) {
        this.deIndexRequests.add( deIndexRequest );
    }


    public void addAllDeIndexRequest( final Set<DeIndexRequest> deIndexRequests ) {
        this.deIndexRequests.addAll( deIndexRequests );
    }


    public Set<IndexRequest> getIndexRequests() {
        return indexRequests;
    }


    public Set<DeIndexRequest> getDeIndexRequests() {
        return deIndexRequests;
    }


    @JsonIgnore
    public boolean isEmpty(){
        return indexRequests.isEmpty() && deIndexRequests.isEmpty();
    }

    /**
     * return the promise
     */
    @JsonIgnore
    public BetterFuture<IndexOperationMessage> getFuture() {
        return containerFuture;
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

    public void done() {
        //if this has been serialized, it could be null. don't NPE if it is, there's nothing to ack
        final BetterFuture<IndexOperationMessage> future = getFuture();

        if(future != null ){
            future.done();
        }
    }
}
