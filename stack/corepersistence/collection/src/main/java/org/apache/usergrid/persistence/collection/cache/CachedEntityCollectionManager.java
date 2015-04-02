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


import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.FieldSet;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.functions.Action1;


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

    @Override
    public Observable<FieldSet> getEntitiesFromFields(  final String entityType, final Collection<Field> fields) {
        return targetEntityCollectionManager.getEntitiesFromFields( entityType, fields );
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
    public Observable<Id> getIdField( final String entityType,  final Field field ) {
        return targetEntityCollectionManager.getIdField( entityType, field );
    }


    @Override
    public Observable<EntitySet> load( final Collection<Id> entityIds ) {
        return targetEntityCollectionManager.load( entityIds );
    }

    @Override
    public Health getHealth() {
        return targetEntityCollectionManager.getHealth();
    }
}
