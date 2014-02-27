/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.usergrid.test;

import java.util.Map;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.query.Query;
import org.apache.usergrid.persistence.query.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EntityManagerFacade {
    private static final Logger logger = LoggerFactory.getLogger( EntityManagerFacade.class );

    private final EntityCollectionManager ecm;
    private final EntityCollectionIndex index;
    private final CollectionScope scope;
    
    public EntityManagerFacade( 
            EntityCollectionManagerFactory collectionFactory, 
            EntityCollectionIndexFactory indexFactory, 
            CollectionScope scope ) {

        this.index = indexFactory.createCollectionIndex( scope );
        this.ecm = collectionFactory.createCollectionManager( scope );
        this.scope = scope;
    }

    public Entity create( String type, Map<String, Object> properties ) {
        if ( !type.equals( scope.getName() )) { 
            throw new RuntimeException("Incorrect type [" + type + "] for scope: " + scope.getName());
        }

        Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), scope.getName()));
        entity = EntityMapUtils.mapToEntity( scope.getName(), entity, properties );
        EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );
        entity = ecm.write( entity ).toBlockingObservable().last();

        index.index( entity );
        return entity;
    }

    public Results searchCollection( Entity user, String type, Query query ) {
        if ( !type.equals( scope.getName() )) { 
            throw new RuntimeException("Incorrect type [" + type + "] for scope: " + scope.getName());
        }
        Results results = index.execute( query );
        return results;
    }

    public Entity get( UUID id ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void addToCollection( Entity user, String type, Entity item ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Entity getApplicationRef() {
        return new Entity();
    }
}
