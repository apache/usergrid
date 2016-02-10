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
package org.apache.usergrid.persistence.core.datastax;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class CQLUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger( CQLUtilsTest.class );

    @Test
    public void testTableCQL() throws Exception {


        Map<String, String> columns = new HashMap<>();
        columns.put("key", "blob");
        columns.put("column1", "text");
        columns.put("value", "blob");

        List<String> primaryKeys = new ArrayList<>();
        primaryKeys.add("key");
        primaryKeys.add("column1");

        Map<String, String> clusteringOrder = new HashMap<>();
        clusteringOrder.put("column1", "DESC");



        TableDefinition table1 = new TableDefinition(
            "table1",
            primaryKeys,
            columns,
            TableDefinition.CacheOption.KEYS,
            clusteringOrder
            );

        String createCQL = CQLUtils.getTableCQL(table1, CQLUtils.ACTION.CREATE);
        String updateCQL = CQLUtils.getTableCQL(table1, CQLUtils.ACTION.UPDATE);

        assertTrue( createCQL.contains( CQLUtils.CREATE_TABLE ) && !createCQL.contains( CQLUtils.ALTER_TABLE ) );
        assertTrue( updateCQL.contains( CQLUtils.ALTER_TABLE ) && !updateCQL.contains( CQLUtils.CREATE_TABLE ) );
        //logger.info("CREATE: {}", createCQL);
        //logger.info("UPDATE: {}", updateCQL);

    }

}
