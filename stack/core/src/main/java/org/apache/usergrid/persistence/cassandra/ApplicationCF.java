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
package org.apache.usergrid.persistence.cassandra;


import java.util.List;

import me.prettyprint.hector.api.ddl.ColumnDefinition;

import static me.prettyprint.hector.api.ddl.ComparatorType.COUNTERTYPE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.getIndexMetadata;


public enum ApplicationCF implements CFEnum {

    /** each row models name:value pairs. {@see org.apache.usergrid.persistence.Schema} for the list of dictionary types */
    ENTITY_DICTIONARIES( "Entity_Dictionaries", "BytesType" ),

    /**
     * Typed vs. untyped dictionary. Dynamic entity dictionaries end up here. {@link
     * EntityManagerImpl#getDictionaryAsMap(org.apache.usergrid.persistence.EntityRef, String)}
     */
    ENTITY_COMPOSITE_DICTIONARIES( "Entity_Composite_Dictionaries",
            "DynamicCompositeType(a=>AsciiType,b=>BytesType,i=>IntegerType,x=>LexicalUUIDType,l=>LongType," +
                    "t=>TimeUUIDType,s=>UTF8Type,u=>UUIDType,A=>AsciiType(reversed=true),B=>BytesType(reversed=true)," +
                    "I=>IntegerType(reversed=true),X=>LexicalUUIDType(reversed=true),L=>LongType(reversed=true)," +
                    "T=>TimeUUIDType(reversed=true),S=>UTF8Type(reversed=true),U=>UUIDType(reversed=true))" ),

    /** Application counters */
    APPLICATION_AGGREGATE_COUNTERS( "Application_Aggregate_Counters", "LongType", COUNTERTYPE.getClassName() ),

    /** Entity counters */
    ENTITY_COUNTERS( "Entity_Counters", "BytesType", COUNTERTYPE.getClassName() ),;

    private final String cf;
    private final String comparator;
    private final String validator;
    private final String indexes;
    private final boolean create;


    ApplicationCF( String cf, String comparator ) {
        this.cf = cf;
        this.comparator = comparator;
        validator = null;
        indexes = null;
        create = true;
    }


    ApplicationCF( String cf, String comparator, String validator ) {
        this.cf = cf;
        this.comparator = comparator;
        this.validator = validator;
        indexes = null;
        create = true;
    }


    ApplicationCF( String cf, String comparator, String validator, String indexes ) {
        this.cf = cf;
        this.comparator = comparator;
        this.validator = validator;
        this.indexes = indexes;
        create = true;
    }


    @Override
    public String toString() {
        return cf;
    }


    @Override
    public String getColumnFamily() {
        return cf;
    }


    @Override
    public String getComparator() {
        return comparator;
    }


    @Override
    public String getValidator() {
        return validator;
    }


    @Override
    public boolean isComposite() {
        return comparator.startsWith( "DynamicCompositeType" );
    }


    @Override
    public List<ColumnDefinition> getMetadata() {
        return getIndexMetadata( indexes );
    }


    @Override
    public boolean create() {
        return create;
    }

}
