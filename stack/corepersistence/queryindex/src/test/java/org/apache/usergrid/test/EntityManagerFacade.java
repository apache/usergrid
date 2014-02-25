/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.usergrid.test;

import java.util.Map;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.query.Query;
import org.apache.usergrid.persistence.query.Results;

/**
 *
 * @author ApigeeCorporation
 */
public class EntityManagerFacade {
    private final EntityCollectionManagerFactory factory;
    private final EntityCollectionManagerSync ecm;
    private final CollectionScope scope;
    
    public EntityManagerFacade( EntityCollectionManagerFactory factory, CollectionScope scope ) {
        this.factory = factory;
        this.ecm = factory.createCollectionManagerSync( scope );
        this.scope = scope;
    }

    public Entity create( String type, Map<String, Object> properties ) {
        if ( type.equals( scope.getName() )) { 
            throw new RuntimeException("Incorrect type [" + type + "] for scope: " + scope.getName());
        }
        Entity entity = new Entity( new SimpleId( type ) );
        EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );
        return ecm.write( entity );
    }

    public Entity get( UUID id ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void addToCollection( Entity user, String collection, Entity item ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Results searchCollection( Entity user, String collection, Query query ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Entity getApplicationRef() {
        return null; 
    }
    
}
