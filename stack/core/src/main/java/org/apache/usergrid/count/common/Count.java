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
package org.apache.usergrid.count.common;


import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.hector.api.Serializer;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;


/**
 * Loosely models a 'count' of things to
 *
 * @author zznate
 */
@JsonAutoDetect(creatorVisibility = Visibility.PUBLIC_ONLY)
public class Count<K, C> {

    @JsonProperty
    private final String tableName;
    @JsonProperty
    private final K keyName;
    @JsonProperty
    private final C columnName;
    @JsonProperty
    private long value;

    private Serializer<K> keySerializer;
    private Serializer<C> columnNameSerializer;
    private String counterName;


    @JsonCreator
    public Count( @JsonProperty(value = "tableName") String tableName, @JsonProperty(value = "keyName") K keyName,
                  @JsonProperty(value = "columnName") C columnName, @JsonProperty(value = "value") long value ) {
        this.tableName = tableName;
        this.keyName = keyName;
        this.columnName = columnName;
        this.value = value;
        this.keySerializer = SerializerTypeInferer.getSerializer( keyName );
        this.columnNameSerializer = SerializerTypeInferer.getSerializer( columnName );
    }


    public Count apply( Count count ) {
        if ( !StringUtils.equals( count.getCounterName(), getCounterName() ) ) {
            throw new IllegalArgumentException( "Attempt to apply a counter with a different name" );
        }
        this.value += count.getValue();
        return this;
    }


    /** the counter name should uniquely identify the entity being counted. */
    @JsonIgnore
    public String getCounterName() {
        if ( counterName == null ) {
            counterName = tableName + ":" + Hex.encodeHexString( getKeyNameBytes().array() ) + ":" + Hex
                    .encodeHexString( getColumnNameBytes().array() );
        }
        return counterName;
    }


    public long getValue() {
        return value;
    }


    public C getColumnName() {
        return columnName;
    }


    public K getKeyName() {
        return keyName;
    }


    @JsonIgnore
    public ByteBuffer getKeyNameBytes() {
        return keySerializer.toByteBuffer( keyName );
    }


    @JsonIgnore
    public ByteBuffer getColumnNameBytes() {
        return columnNameSerializer.toByteBuffer( columnName );
    }


    @JsonIgnore
    public Serializer<K> getKeySerializer() {
        return keySerializer;
    }


    @JsonIgnore
    public Serializer<C> getColumnNameSerializer() {
        return columnNameSerializer;
    }


    public String getTableName() {
        return tableName;
    }


    @Override
    public String toString() {
        return "Counter Name: ".concat( getCounterName() ).concat( " value: " ).concat( Long.toString( value ) );
    }
}
