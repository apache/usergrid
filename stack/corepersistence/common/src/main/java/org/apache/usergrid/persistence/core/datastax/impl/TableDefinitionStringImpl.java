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

package org.apache.usergrid.persistence.core.datastax.impl;

import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;


public class TableDefinitionStringImpl implements TableDefinition {

    private String keyspace;
    private String tableName;
    private String cql;


    public TableDefinitionStringImpl( String keyspace, String tableName, String cql ) {
        this.keyspace = keyspace;
        this.tableName = tableName;
        this.cql = cql;
    }


    @Override
    public String getKeyspace() {
        return keyspace;
    }

    @Override
    public String getTableName() {
        return tableName;
    }


    @Override
    public String getTableCQL(CassandraFig cassandraFig, ACTION tableAction) throws Exception {
        return cql;
    }
}
