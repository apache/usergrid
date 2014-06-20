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
package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * A synchronous implementation that will block until the call is returned.
 */
public class EntityCollectionManagerSyncImpl implements EntityCollectionManagerSync {


    private final EntityCollectionManager em;


    @Inject
    public EntityCollectionManagerSyncImpl( final EntityCollectionManagerFactory emf,
                                            @Assisted final CollectionScope scope ) {

        //this feels a bit hacky, and I franky don't like it.  However, this is the only
        //way to get this to work I can find with guice, without having to manually implement the factory
        Preconditions.checkNotNull( emf, "entityCollectionManagerFactory is required" );
        Preconditions.checkNotNull( scope, "scope is required" );
        this.em = emf.createCollectionManager( scope );
    }


    @Override
    public Entity write( final Entity entity ) {
        return em.write( entity ).toBlocking().single();
    }


    @Override
    public void delete( final Id entityId ) {
        em.delete( entityId ).toBlocking().last();
    }


    @Override
    public Entity load( final Id entityId ) {
        return em.load( entityId ).toBlocking().lastOrDefault( null );
    }
}
