package org.apache.usergrid.persistence.collection.mvcc.stage.impl.read;


import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionStage;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;


/**
 * This stage is a load stage to load a single entity
 */
public class Load implements ExecutionStage {


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
    public void performStage( final ExecutionContext executionContext ) {
        final UUID entityId = executionContext.getMessage( UUID.class );

        Preconditions.checkNotNull( entityId, "Entity id required in the read stage" );


        final CollectionContext collectionContext = executionContext.getCollectionContext();

        //generate  a version that represents now
        final UUID versionMax = uuidService.newTimeUUID();

        List<MvccEntity> results = entitySerializationStrategy.load( collectionContext, entityId, versionMax, 1 );

        //nothing to do, we didn't get a result back
        if(results.size() != 1){
            executionContext.setMessage( null );
            executionContext.proceed();
            return;
        }

        final Optional<Entity> targetVersion = results.get(0).getEntity();

        //this entity has been marked as cleared.  The version exists, but does not have entity data
        if(!targetVersion.isPresent()){

            //TODO, a lazy async repair/cleanup here?

            executionContext.setMessage( null );
            executionContext.proceed();
            return;
        }

        executionContext.setMessage( targetVersion.get() );
        executionContext.proceed();
    }
}
