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


import java.util.Arrays;
import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.SearchType;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.model.entity.Id;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.Client;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.createContextName;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.createIndexDocId;


/**
 * Represent the properties required to build an index request
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class DeIndexRequest implements BatchRequest {

    private String[] entityTypes;
    private String[] indexes;
    private String documentId;


    public DeIndexRequest( ) {

    }


    public DeIndexRequest(String[] indexes, ApplicationScope applicationScope, IndexScope indexScope, Id id, UUID version) {
        String context = createContextName(applicationScope,indexScope);
        this.indexes = indexes;
        this.entityTypes = SearchType.fromId(id).getTypeNames(applicationScope);
        this.documentId =  createIndexDocId(id, version,context);
    }


    @Override
    public void doOperation(final Client client, final BulkRequestBuilder bulkRequest ){


        for(final String index: indexes) {
            for(String entityType : entityTypes) {
                final DeleteRequestBuilder builder = client.prepareDelete(index, entityType, documentId);
                bulkRequest.add(builder);
            }
        }
    }


    public String[] getIndexes() {
        return indexes;
    }


    public String[] getEntityTypes() {
        return entityTypes;
    }


    public String getDocumentId() {
        return documentId;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final DeIndexRequest that = ( DeIndexRequest ) o;

        if ( !documentId.equals( that.documentId ) ) {
            return false;
        }
        if ( !entityTypes.equals( that.entityTypes ) ) {
            return false;
        }
        if ( !Arrays.equals( indexes, that.indexes ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = Arrays.hashCode( indexes );
        result = 31 * result + entityTypes.hashCode();
        result = 31 * result + documentId.hashCode();
        return result;
    }
}
