package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.apache.usergrid.corepersistence.CpManagerCache;
import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.model.entity.Entity;

import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.utils.UUIDUtils;

import com.google.inject.Injector;


/**
 * Created by ApigeeCorporation on 9/27/14.
 */
public class EntityVersionCreatedImpl implements EntityVersionCreated {
    @Override
    /**
     * 1) ensure the edge in the graph is written from owner->entity
     2) Read all edge types TO the entity node
     3) For each edge type, get all edges, and index the document in it's index
     Us RX and streaming for that, once you get it working, we can do parallel per edge type, so it'll be super fucking fast
     once that's done, repeat for version deleted without step 1)
     */
    public void versionCreated( final CollectionScope scope, final Entity entity ) {


        UUID timeStampUuid =   entity.getId().getUuid() != null && UUIDUtils.isTimeBased( entity.getId().getUuid()) ?  entity.getId().getUuid() : UUIDUtils.newTimeUUID();
        long uuidHash =  UUIDUtils.getUUIDLong(timeStampUuid);

        CpManagerCache managerCache = getManagerCache();
        GraphManager graphManager = managerCache.getGraphManager(scope);

        Edge edge = new SimpleEdge(
                scope.getOwner(),//cpHeadEntity.getId(),
                scope.getName(),//edgeType,
                entity.getId(),//memberEntity.getId(),
                uuidHash);

        //SearchEdgeType derp =
        rx.Observable<String> derp = graphManager.getEdgeTypesToTarget(
                new SimpleSearchEdgeType( entity.getId(), scope.getName(), null ) );



    }

    public CpManagerCache getManagerCache() {

       // if ( managerCache == null ) {

            // TODO: better solution for getting injector?
            Injector injector = CpSetup.getInjector();

            EntityCollectionManagerFactory ecmf;
            EntityIndexFactory eif;
            GraphManagerFactory gmf;

            ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
            eif = injector.getInstance( EntityIndexFactory.class );
            gmf = injector.getInstance( GraphManagerFactory.class );

            return new CpManagerCache( ecmf, eif, gmf );
        //}
    }
}
