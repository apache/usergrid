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
package org.apache.usergrid.persistence.core.astyanax;


import org.apache.cassandra.db.marshal.DynamicCompositeType;


/**
 * Simple class to hold constants we'll need for column types
 *
 */
public class ColumnTypes {


    /**
     * Long time with max by the row key and min at the end of the row
     */
    public static final String LONG_TYPE_REVERSED = "LongType(reversed=true)";


    public static final String UUID_TYPE_REVERSED = "UUIDType(reversed=true)";

    public static final String BOOLEAN = "BooleanType";



    /**
     * Constant for the dynamic composite comparator type we'll need
     */
    public static final String DYNAMIC_COMPOSITE_TYPE = DynamicCompositeType.class.getSimpleName() + "(a=>AsciiType,b=>BytesType,i=>IntegerType,x=>LexicalUUIDType,l=>LongType," +
                        "t=>TimeUUIDType,s=>UTF8Type,u=>UUIDType,A=>AsciiType(reversed=true),B=>BytesType(reversed=true)," +
                        "I=>IntegerType(reversed=true),X=>LexicalUUIDType(reversed=true),L=>"+LONG_TYPE_REVERSED+"," +
                        "T=>TimeUUIDType(reversed=true),S=>UTF8Type(reversed=true),U=>"+UUID_TYPE_REVERSED+")";





}
