/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.index.impl;


import java.io.IOException;

import org.apache.lucene.search.Query;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;


/**
 * Marker interface that does nothing
 */
public class NoOpQueryBuilder extends AbstractQueryBuilder implements QueryBuilder {

    public static final NoOpQueryBuilder INSTANCE = new NoOpQueryBuilder();

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {

    }

    @Override
    protected void doXContent( final XContentBuilder builder, final Params params ) throws IOException {
         //no op
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        return null;
    }

    @Override
    protected boolean doEquals(AbstractQueryBuilder other) {
        return false;
    }

    @Override
    protected int doHashCode() {
        return 0;
    }


    @Override
    public String getWriteableName() {
        return null;
    }
}
