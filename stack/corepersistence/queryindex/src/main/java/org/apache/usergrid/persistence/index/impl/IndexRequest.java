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


import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;


/**
 * Represent the properties required to build an index request
 */
public class IndexRequest implements BatchRequest {

    public final  String writeAlias;
    public final String entityType;
    public final String documentId;

    public final Map<String, Object> data;


    public IndexRequest( final String writeAlias, final String entityType, final String documentId,
                         final Map<String, Object> data ) {
        this.writeAlias = writeAlias;
        this.entityType = entityType;
        this.documentId = documentId;
        this.data = data;
    }


    public void  doOperation(final Client client, final BulkRequestBuilder bulkRequest ){
        IndexRequestBuilder builder =
                      client.prepareIndex(writeAlias, entityType, documentId).setSource( data );


        bulkRequest.add( builder );

    }


    public String getWriteAlias() {
        return writeAlias;
    }


    public String getEntityType() {
        return entityType;
    }


    public String getDocumentId() {
        return documentId;
    }


    public Map<String, Object> getData() {
        return data;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final IndexRequest that = ( IndexRequest ) o;

        if ( !data.equals( that.data ) ) {
            return false;
        }
        if ( !documentId.equals( that.documentId ) ) {
            return false;
        }
        if ( !entityType.equals( that.entityType ) ) {
            return false;
        }
        if ( !writeAlias.equals( that.writeAlias ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = writeAlias.hashCode();
        result = 31 * result + entityType.hashCode();
        result = 31 * result + documentId.hashCode();
        result = 31 * result + data.hashCode();
        return result;
    }
}
