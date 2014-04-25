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
package org.apache.usergrid.persistence.index.legacy;

import java.util.HashMap;
import java.util.Map;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.util.EntityBuilder;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.value.Location;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.index.query.EntityRef;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.apache.usergrid.persistence.index.utils.EntityMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * Implements just enough of the old EntityManager interface to get old tests imported from the
 * Usergrid 1.0 Core module working against the new Core Persistence index & query system.
 */
public class EntityManagerFacade {
    private static final Logger logger = LoggerFactory.getLogger( EntityManagerFacade.class );

    private final OrganizationScope orgScope;
    private final CollectionScope appScope;
    private final EntityCollectionManagerFactory ecmf;
    private final EntityIndexFactory ecif;
    private final Map<String, String> typesByCollectionNames = new HashMap<String, String>();

    private final Map<CollectionScope, EntityCollectionManager> managers = new HashMap<>();
    private final Map<CollectionScope, EntityIndex> indexes = new HashMap<>();
    
    public EntityManagerFacade( 
        OrganizationScope orgScope, 
        CollectionScope appScope, 
        EntityCollectionManagerFactory ecmf, 
        EntityIndexFactory ecif ) {

        this.appScope = appScope;
        this.orgScope = orgScope;
        this.ecmf = ecmf;
        this.ecif = ecif;
    }

    private EntityIndex getIndex( CollectionScope scope ) { 
        EntityIndex eci = indexes.get( scope );
        if ( eci == null ) {
            eci = ecif.createEntityIndex( orgScope, appScope );
            indexes.put( scope, eci );
        }
        return eci;
    }

    private EntityCollectionManager getManager( CollectionScope scope ) { 
        EntityCollectionManager ecm = managers.get( scope );
        if ( ecm == null ) {
            ecm = ecmf.createCollectionManager( scope );
            managers.put( scope, ecm);
        }
        return ecm;
    }

    public Entity create( String type, Map<String, Object> properties ) {

        CollectionScope scope = new CollectionScopeImpl( 
                orgScope.getOrganization(), appScope.getOwner(), type );
        EntityCollectionManager ecm = getManager( scope );
        EntityIndex eci = getIndex( scope );

        final String collectionName;
        if ( type.endsWith("y") ) {
            collectionName = type.substring( 0, type.length() - 1) + "ies";
        } else {
            collectionName = type + "s";
        }
        typesByCollectionNames.put( collectionName, type );

        Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), type ));
        entity = EntityMapUtils.fromMap( entity, properties );
        entity.setField(new LongField("created", entity.getId().getUuid().timestamp()) );
        entity.setField(new LongField("modified", entity.getId().getUuid().timestamp()) );
        entity = ecm.write( entity ).toBlockingObservable().last();

        eci.index( scope, entity );
        return entity;
    }

    public Results searchCollection( Entity user, String collectionName, Query query ) {

        String type = typesByCollectionNames.get( collectionName );
		if ( type == null ) {
			throw new RuntimeException( 
					"No type found for collection name: " + collectionName);
		}
        CollectionScope scope = new CollectionScopeImpl( 
                orgScope.getOrganization(), appScope.getOwner(), type );

        EntityIndex eci = getIndex( scope );
        Results results = eci.search( scope, query );
        return results;
    }

    public Entity get( Id id ) {
        CollectionScope scope = new CollectionScopeImpl( 
                orgScope.getOrganization(), appScope.getOwner(), id.getType() );
        EntityCollectionManager ecm = getManager( scope );
        return ecm.load( id ).toBlockingObservable().last();
    }

    public void addToCollection( Entity user, String collectionName, Entity entity ) {
        // basically a no-op except that can now map Entity type to collection name
        typesByCollectionNames.put( collectionName, entity.getId().getType() );
    }

    public Entity getApplicationRef() {
        return new Entity();
    }

    public void update( Entity entity ) {

        String type = entity.getId().getType();

        CollectionScope scope = new CollectionScopeImpl( 
                orgScope.getOrganization(), appScope.getOwner(), type );

        EntityCollectionManager ecm = getManager( scope );
        EntityIndex eci = getIndex( scope );

        final String collectionName;
        if ( type.endsWith("y") ) {
            collectionName = type.substring( 0, type.length() - 1) + "ies";
        } else {
            collectionName = type + "s";
        }
        typesByCollectionNames.put( collectionName, type );
        
        entity = ecm.write( entity ).toBlockingObservable().last();

        eci.index( scope, entity );
    }

	
	public void delete( Entity entity ) {

        String type = entity.getId().getType();

        CollectionScope scope = new CollectionScopeImpl( 
                orgScope.getOrganization(), appScope.getOwner(), type );

        EntityCollectionManager ecm = getManager( scope );
        EntityIndex eci = getIndex( scope );

		eci.deindex( scope, entity );
		ecm.delete( entity.getId() );
	}


	public void setProperty( EntityRef entityRef, String fieldName, double lat, double lon ) {

        String type = entityRef.getId().getType();

        CollectionScope scope = new CollectionScopeImpl( 
                orgScope.getOrganization(), appScope.getOwner(), type );

        EntityCollectionManager ecm = getManager( scope );
        EntityIndex eci = getIndex( scope );

		Entity entity = ecm.load( entityRef.getId() ).toBlockingObservable().last();
		entity.setField( new LocationField( fieldName, new Location( lat, lon )));

        entity = ecm.write(entity).toBlockingObservable().last();
        eci.index( scope, entity);
	}


    public void refreshIndex() {

        CollectionScope scope = new CollectionScopeImpl( 
                orgScope.getOrganization(), appScope.getOwner(), "dummy" );

        EntityCollectionManager ecm = getManager( scope );
        EntityIndex eci = getIndex( scope );
		eci.refresh();
    }

}
