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



import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.ColumnFamily;


/**
 * Simple wrapper to force every column family to use ScopedRowKeys
 *
 * @author tnine
 */
public class MultiTennantColumnFamily<R extends ScopedRowKey<?>, V >
    extends ColumnFamily<R, V> {

    public MultiTennantColumnFamily( final String columnFamilyName, final Serializer<R> keySerializer,
                                     final Serializer<V> columnSerializer ) {

        super( columnFamilyName, keySerializer, columnSerializer );
    }


    public MultiTennantColumnFamily( final String columnFamilyName, final Serializer<R> keySerializer,
                                     final Serializer<V> columnSerializer, final Serializer<?> defaultValueSerializer ) {

        super( columnFamilyName, keySerializer, columnSerializer, defaultValueSerializer );
    }
}
