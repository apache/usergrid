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

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class TableDefinitionTest {

    @Test
    public void testNullTableName(){

        try{
            TableDefinition table1 = new TableDefinition(null, null, null, null, null);
        } catch (NullPointerException npe){
            assertEquals("Table name cannot be null", npe.getMessage());
        }


    }

    @Test
    public void testNullPrimaryKeys(){

        try{
            TableDefinition table1 = new TableDefinition("table1", null, null, null, null);
        } catch (NullPointerException npe){
            assertEquals("Primary Key(s) cannot be null", npe.getMessage());
        }


    }

    @Test
    public void testNullColumns(){

        try{
            TableDefinition table1 = new TableDefinition("table1",
                new ArrayList<>(), null, null, null);
        } catch (NullPointerException npe){
            assertEquals("Columns cannot be null", npe.getMessage());
        }


    }

    @Test
    public void testNullCacheOption(){

        try{
            TableDefinition table1 = new TableDefinition("table1",
                new ArrayList<>(),
                new HashMap<>(), null, null);
        } catch (NullPointerException npe){
            assertEquals("CacheOption cannot be null", npe.getMessage());
        }


    }
}
