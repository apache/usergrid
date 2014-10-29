package org.apache.usergrid.corepersistence.events;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.CpEntityManager;
import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.corepersistence.HybridEntityManagerFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Created by ApigeeCorporation on 10/24/14.
 */
public class EntityVersionCreatedImpl implements EntityVersionCreated{

    private static final Logger logger = LoggerFactory.getLogger( EntityVersionCreatedImpl.class );

    public EntityVersionCreatedImpl() {
        logger.debug("EntityVersionCreated");
    }

    @Override
    public void versionCreated( final CollectionScope scope, final Entity entity ) {
        logger.debug("Entering deleted for entity {}:{} v {} "
                        + "scope\n   name: {}\n   owner: {}\n   app: {}",
                new Object[] { entity.getId().getType(), entity.getId().getUuid(), entity.getVersion(),
                        scope.getName(), scope.getOwner(), scope.getApplication()});

        HybridEntityManagerFactory hemf = (HybridEntityManagerFactory)CpSetup.getEntityManagerFactory();
        CpEntityManagerFactory cpemf = (CpEntityManagerFactory)hemf.getImplementation();

//        CpEntityManagerFactory emf = (CpEntityManagerFactory)
//                CpSetup.getInjector().getInstance( EntityManagerFactory.class );

        final EntityIndex ei = cpemf.getManagerCache().getEntityIndex(scope);

        EntityIndexBatch batch = ei.createBatch();

        batch.deindexPreviousVersions( entity );
        batch.execute();
    }
}
