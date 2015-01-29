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
package org.apache.usergrid.persistence.core.migration.data;


import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UTF8Type;

import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.StringRowCompositeSerializer;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.serializers.StringSerializer;


@Singleton
public class MigrationInfoSerializationImpl implements MigrationInfoSerialization {

    /**
     * Cache to cache versions temporarily
     */
    private final LoadingCache<String, Integer> versionCache = CacheBuilder.newBuilder()
        //cache the local value for 1 minute
        .expireAfterWrite(1, TimeUnit.MINUTES).build( new CacheLoader<String, Integer>() {
            @Override
            public Integer load( final String key ) throws Exception {
                return getVersion();
            }
        } );

    /**
     * Just a hard coded scope since we need it
     */
    private static final Id STATIC_ID =
            new SimpleId( UUID.fromString( "00000000-0000-1000-8000-000000000000" ), "status" );


    private static final ScopedRowKeySerializer<String> ROW_KEY_SER =
            new ScopedRowKeySerializer<String>( StringRowCompositeSerializer.get() );

    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();


    public static final MultiTennantColumnFamily<ScopedRowKey<String>, String> CF_MIGRATION_INFO =
            new MultiTennantColumnFamily<>( "Data_Migration_Info", ROW_KEY_SER, STRING_SERIALIZER );


    private static final ScopedRowKey<String> ROW_KEY = ScopedRowKey.fromKey( STATIC_ID, "" );

    private static final String COL_STATUS_MESSAGE = "statusMessage";

    private static final String COLUMN_VERSION = "version";

    private static final String COLUMN_STATUS_CODE = "statusCode";

    private final Keyspace keyspace;


    @Inject
    public MigrationInfoSerializationImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public void setStatusMessage( final String message ) {

        try {
            keyspace.prepareColumnMutation( CF_MIGRATION_INFO, ROW_KEY, COL_STATUS_MESSAGE ).putValue( message, null )
                    .execute();
        }
        catch ( ConnectionException e ) {
            throw new DataMigrationException( "Unable to save status", e );
        }
    }


    @Override
    public String getStatusMessage() {
        try {
            return keyspace.prepareQuery( CF_MIGRATION_INFO ).getKey( ROW_KEY ).getColumn( COL_STATUS_MESSAGE )
                           .execute().getResult().getStringValue();
        }
        //swallow, it doesn't exist
        catch ( NotFoundException nfe ) {
            return null;
        }
        catch ( ConnectionException e ) {
            throw new DataMigrationException( "Unable to retrieve status", e );
        }
    }


    @Override
    public void setVersion( final int version ) {
        try {
            keyspace.prepareColumnMutation( CF_MIGRATION_INFO, ROW_KEY, COLUMN_VERSION ).putValue( version, null )
                    .execute();
        }
        catch ( ConnectionException e ) {
            throw new DataMigrationException( "Unable to save status", e );
        }

        versionCache.invalidateAll();
    }


    @Override
    public int getVersion() {
        try {
            return keyspace.prepareQuery( CF_MIGRATION_INFO ).getKey( ROW_KEY ).getColumn( COLUMN_VERSION ).execute()
                           .getResult().getIntegerValue();
        }
        //swallow, it doesn't exist
        catch ( NotFoundException nfe ) {
            return 0;
        }
        catch ( ConnectionException e ) {
            throw new DataMigrationException( "Unable to retrieve status", e );
        }
    }


    @Override
    public void setStatusCode( final int status ) {
        try {
            keyspace.prepareColumnMutation( CF_MIGRATION_INFO, ROW_KEY, COLUMN_STATUS_CODE ).putValue( status, null )
                    .execute();
        }
        catch ( ConnectionException e ) {
            throw new DataMigrationException( "Unable to save status", e );
        }
    }


    @Override
    public int getStatusCode() {
        try {
            return keyspace.prepareQuery( CF_MIGRATION_INFO ).getKey( ROW_KEY ).getColumn( COLUMN_STATUS_CODE )
                           .execute().getResult().getIntegerValue();
        }
        //swallow, it doesn't exist
        catch ( NotFoundException nfe ) {
            return 0;
        }
        catch ( ConnectionException e ) {
            throw new DataMigrationException( "Unable to retrieve status", e );
        }
    }

    @Override
    public int getCurrentVersion() {
        try {
            return versionCache.get("currentversion");
        }catch (Exception ee){
            throw new RuntimeException(ee);
        }
    }

    @Override
    public void invalidate() {
        versionCache.invalidateAll();
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.singletonList(
                new MultiTennantColumnFamilyDefinition( CF_MIGRATION_INFO, BytesType.class.getSimpleName(),
                        UTF8Type.class.getSimpleName(), BytesType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.KEYS ) );
    }
}
