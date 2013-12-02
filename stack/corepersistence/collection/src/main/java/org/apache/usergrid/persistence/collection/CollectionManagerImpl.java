package org.apache.usergrid.persistence.collection;


import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.beanutils.BeanUtils;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.Commit;
import org.apache.usergrid.persistence.collection.mvcc.stage.Start;
import org.apache.usergrid.persistence.collection.mvcc.stage.Write;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;


/**
 * Simple implementation.  Should perform
 * @author tnine
 */
public class CollectionManagerImpl implements CollectionManager {

    private static final Logger logger = LoggerFactory.getLogger(CollectionManagerImpl.class);

    private final CollectionContext context;
    private final TimeService timeService;
    private final Start startStage;
    private final Write writeStage;
    private final Commit commitStage;


    @Inject
    public CollectionManagerImpl( final CollectionContext context, final TimeService timeService, final Start startStage, final Write writeStage,
                                  final Commit commitStage ) {
        this.context = context;
        this.timeService = timeService;
        this.startStage = startStage;
        this.writeStage = writeStage;
        this.commitStage = commitStage;
    }


    @Override
    public void create( final Entity entity ) {

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = entityId;
        final long created = timeService.getTime();


        try {
            BeanUtils.setProperty(entity, "uuid", entityId);
        }
        catch ( Throwable t ) {
            logger.error( "Unable to set uuid.  See nested exception", t );
            throw new RuntimeException( "Unable to set uuid.  See nested exception", t );
        }

        entity.setVersion( version );
        entity.setCreated( created );
        entity.setUpdated( created );

        MvccEntityImpl mvccEntity = new MvccEntityImpl(context, entityId, version, entity   );


        MutationBatch mutation = startStage.performStage(  mvccEntity );
        writeStage.performStage( mvccEntity );
        commitStage.performStage( mvccEntity );
    }


    @Override
    public void update( final Entity entity ) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public void delete( final UUID entityId ) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Entity load( final UUID entityId ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
