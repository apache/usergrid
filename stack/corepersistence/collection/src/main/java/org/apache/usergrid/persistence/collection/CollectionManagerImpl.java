package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.beanutils.BeanUtils;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntityImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;


/**
 * Simple implementation.  Should perform
 * @author tnine
 */
public class CollectionManagerImpl implements CollectionManager {

    private static final Logger logger = LoggerFactory.getLogger(CollectionManagerImpl.class);

    private final CollectionContext context;
    private final TimeService timeService;



    public CollectionManagerImpl( final CollectionContext context, final TimeService timeService ) {
        this.context = context;
        this.timeService = timeService;
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
