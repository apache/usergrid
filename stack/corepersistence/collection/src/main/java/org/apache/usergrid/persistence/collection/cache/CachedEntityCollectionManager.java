/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.cache;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.collection.*;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.FieldSetImpl;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;


@Singleton
public class CachedEntityCollectionManager implements EntityCollectionManager {

    /**
     * The collection manager we perform real i/o from
     */
    private EntityCollectionManager targetEntityCollectionManager;


    /** Short-term cache to keep us from reloading same Entity during single request. */
    private Cache<Id, Entity> entityCache;

    private Action1<Entity> cacheAdd = new Action1<Entity>() {
        @Override
        public void call( final Entity entity ) {
            entityCache.put( entity.getId(), entity );
        }
    };


    @Inject
    public CachedEntityCollectionManager( final EntityCacheFig entityCacheFig,
                                          final EntityCollectionManager targetEntityCollectionManager ) {
        this.targetEntityCollectionManager = targetEntityCollectionManager;


        entityCache = CacheBuilder.newBuilder().maximumSize( entityCacheFig.getCacheSize() )
                                  .expireAfterWrite( entityCacheFig.getCacheTimeout(), TimeUnit.SECONDS )
                                  .build();
    }

    public Observable<FieldSet> getAllEntities(final Collection<Field> fields) {
        return rx.Observable.just(fields).map( new Func1<Collection<Field>, FieldSet>() {
            @Override
            public FieldSet call( Collection<Field> fields ) {

                    final FieldSet response = new FieldSetImpl(fields.size());

                    return response;
            }
        } );
    }

    @Override
    public Observable<Entity> write( final Entity entity ) {
        return targetEntityCollectionManager.write( entity ).doOnNext( cacheAdd );
    }


    @Override
    public Observable<Id> delete( final Id entityId ) {
        return targetEntityCollectionManager.delete( entityId ).doOnNext( new Action1<Id>() {
            @Override
            public void call( final Id id ) {
                entityCache.invalidate( id );
            }
        } );
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {
        final Entity entity = entityCache.getIfPresent( entityId );

        if ( entity != null ) {
            return Observable.just( entity );
        }

        return targetEntityCollectionManager.load( entityId ).doOnNext( cacheAdd );

    }


    @Override
    public Observable<VersionSet> getLatestVersion( final Collection<Id> entityId ) {
        return targetEntityCollectionManager.getLatestVersion( entityId );
    }


    @Override
    public Observable<Id> getIdField( final Field field ) {
        return targetEntityCollectionManager.getIdField( field );
    }


    @Override
    public Observable<EntitySet> load( final Collection<Id> entityIds ) {
        return targetEntityCollectionManager.load( entityIds );
    }


    @Override
    public Observable<Entity> update( final Entity entity ) {
        return targetEntityCollectionManager.update( entity ).doOnNext( cacheAdd );
    }


    @Override
    public Health getHealth() {
        return targetEntityCollectionManager.getHealth();
    }
}
