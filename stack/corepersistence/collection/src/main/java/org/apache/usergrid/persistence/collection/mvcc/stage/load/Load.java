package org.apache.usergrid.persistence.collection.mvcc.stage.load;


import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.util.functions.Func1;


/**
 * This stage is a load stage to load a single entity
 */
@Singleton
public class Load implements Func1<CollectionIoEvent<Id>, Entity> {


    private static final Logger LOG = LoggerFactory.getLogger( Load.class );

    private final UUIDService uuidService;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;


    @Inject
    public Load( final UUIDService uuidService, final MvccEntitySerializationStrategy entitySerializationStrategy ) {
        Preconditions.checkNotNull( entitySerializationStrategy, "entitySerializationStrategy is required" );
        Preconditions.checkNotNull( uuidService, "uuidService is required" );


        this.uuidService = uuidService;
        this.entitySerializationStrategy = entitySerializationStrategy;
    }


    @Override
    public Entity call( final CollectionIoEvent<Id> idIoEvent ) {
        final Id entityId = idIoEvent.getEvent();

        ValidationUtils.verifyIdentity( entityId );


        final CollectionScope collectionScope = idIoEvent.getEntityCollection();

        //generate  a version that represents now
        final UUID versionMax = uuidService.newTimeUUID();

        List<MvccEntity> results = entitySerializationStrategy.load( collectionScope, entityId, versionMax, 1 );

        //nothing to do, we didn't get a result back
        if ( results.size() != 1 ) {
            return null;
        }

        final Optional<Entity> targetVersion = results.get( 0 ).getEntity();

        //this entity has been marked as cleared.  The version exists, but does not have entity data
        if ( !targetVersion.isPresent() ) {

            //TODO, a lazy async repair/cleanup here?

            return null;
        }


        return targetVersion.get();
    }
}
