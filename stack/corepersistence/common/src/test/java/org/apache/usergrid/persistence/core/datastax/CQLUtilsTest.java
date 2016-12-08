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


import com.datastax.driver.core.DataType;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionImpl;
import org.apache.usergrid.persistence.core.guice.TestCommonModule;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( ITRunner.class )
@UseModules( TestCommonModule.class )
public class CQLUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger( CQLUtilsTest.class );

    @Inject
    CassandraFig cassandraFig;


    @Test
    public void testTableCQL() throws Exception {


        Map<String, DataType.Name> columns = new HashMap<>();
        columns.put("key", DataType.Name.BLOB);
        columns.put("column1", DataType.Name.TEXT);
        columns.put("value", DataType.Name.BLOB);

        List<String> partitionKeys = new ArrayList<>();
        partitionKeys.add("key");

        List<String> columnKeys = new ArrayList<>();
        columnKeys.add("column1");

        Map<String, String> clusteringOrder = new HashMap<>();
        clusteringOrder.put("column1", "DESC");



        TableDefinitionImpl table1 = new TableDefinitionImpl( cassandraFig.getApplicationKeyspace(),
            CQLUtils.quote("table1"),
            partitionKeys,
            columnKeys,
            columns,
            TableDefinitionImpl.CacheOption.KEYS,
            clusteringOrder
            );

        String createCQL = table1.getTableCQL(cassandraFig, TableDefinition.ACTION.CREATE);
        String updateCQL = table1.getTableCQL(cassandraFig, TableDefinition.ACTION.UPDATE);

        assertTrue(
            createCQL.contains(CQLUtils.CREATE_TABLE ) &&
                !createCQL.contains( CQLUtils.ALTER_TABLE )  &&
                createCQL.contains( DataType.Name.BLOB.toString() ) &&
                createCQL.contains( DataType.Name.TEXT.toString() )

        );
        assertTrue(
            updateCQL.contains( CQLUtils.ALTER_TABLE ) &&
                !updateCQL.contains( CQLUtils.CREATE_TABLE ) &&
                !updateCQL.contains( DataType.Name.BLOB.toString() ) &&
                !updateCQL.contains( DataType.Name.TEXT.toString() )
        );
        logger.info(createCQL);
        logger.info(updateCQL);

    }

    @Test
    public void testLegacyCachingOptions() throws Exception{

        final CassandraFig cassandraFig = mock(CassandraFig.class);
        when(cassandraFig.getVersion()).thenReturn("2.0");

        Map<String, DataType.Name> columns = new HashMap<>();
        columns.put("key", DataType.Name.BLOB);
        columns.put("column1", DataType.Name.TEXT);
        columns.put("value", DataType.Name.BLOB);

        List<String> partitionKeys = new ArrayList<>();
        partitionKeys.add("key");

        List<String> columnKeys = new ArrayList<>();
        columnKeys.add("column1");

        Map<String, String> clusteringOrder = new HashMap<>();
        clusteringOrder.put("column1", "DESC");



        TableDefinitionImpl table1 = new TableDefinitionImpl( cassandraFig.getApplicationKeyspace(),
            CQLUtils.quote("table1"),
            partitionKeys,
            columnKeys,
            columns,
            TableDefinitionImpl.CacheOption.KEYS,
            clusteringOrder
        );

        String createCQL = table1.getTableCQL(cassandraFig, TableDefinition.ACTION.CREATE);
        logger.info(createCQL);
        assertTrue(
            createCQL.contains( "\"keys_only\"" ) &&
                !createCQL.contains( "'keys':'ALL'"  )

        );



    }

    @Test
    public void testCachingOptions() throws Exception {

        final CassandraFig cassandraFig = mock(CassandraFig.class);
        when(cassandraFig.getVersion()).thenReturn("2.1");

        Map<String, DataType.Name> columns = new HashMap<>();
        columns.put("key", DataType.Name.BLOB);
        columns.put("column1", DataType.Name.TEXT);
        columns.put("value", DataType.Name.BLOB);

        List<String> partitionKeys = new ArrayList<>();
        partitionKeys.add("key");

        List<String> columnKeys = new ArrayList<>();
        columnKeys.add("column1");

        Map<String, String> clusteringOrder = new HashMap<>();
        clusteringOrder.put("column1", "DESC");



        TableDefinitionImpl table1 = new TableDefinitionImpl( cassandraFig.getApplicationKeyspace(),
            CQLUtils.quote("table1"),
            partitionKeys,
            columnKeys,
            columns,
            TableDefinitionImpl.CacheOption.KEYS,
            clusteringOrder
        );

        String createCQL = table1.getTableCQL(cassandraFig, TableDefinition.ACTION.CREATE);
        logger.info(createCQL);
        assertTrue(
            createCQL.contains( "'keys':'ALL'"  ) &&
            !createCQL.contains( "\"keys_only\"" )

        );


    }

}
