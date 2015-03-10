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

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.Client;


/**
 * Represent the properties required to build an index request
 */
public class DeIndexRequest implements BatchRequest {

    public final String[] indexes;
    public final String entityType;
    public final String documentId;


    public DeIndexRequest( final String[] indexes, final String entityType, final String documentId) {
        this.indexes = indexes;
        this.entityType = entityType;
        this.documentId = documentId;
    }


    @Override
    public void doOperation(final Client client, final BulkRequestBuilder bulkRequest ){


        for(final String index: indexes) {
            final DeleteRequestBuilder builder = client.prepareDelete( index, entityType, documentId);

            bulkRequest.add( builder );
        }
    }


    public String[] getIndexes() {
        return indexes;
    }


    public String getEntityType() {
        return entityType;
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
        if ( !entityType.equals( that.entityType ) ) {
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
        result = 31 * result + entityType.hashCode();
        result = 31 * result + documentId.hashCode();
        return result;
    }
}
