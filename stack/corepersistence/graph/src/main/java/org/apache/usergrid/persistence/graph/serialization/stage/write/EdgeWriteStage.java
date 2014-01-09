package org.apache.usergrid.persistence.graph.serialization.stage.write;


import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.stage.GraphIoEvent;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.util.functions.Action1;


/**
 * Stage for writing the edge data
 */
@Singleton
public class EdgeWriteStage implements Action1<GraphIoEvent<Edge>> {


    private final EdgeSerialization edgeSerialization;
    private final EdgeMetadataSerialization edgeMetadata;


    @Inject
    public EdgeWriteStage( final EdgeSerialization edgeSerialization, final EdgeMetadataSerialization edgeMetadata ) {
        Preconditions.checkNotNull( edgeSerialization, "edgeSerialization is required" );
        Preconditions.checkNotNull( edgeMetadata, "edgeMetaData is required" );

        this.edgeMetadata = edgeMetadata;

        this.edgeSerialization = edgeSerialization;
    }


    @Override
    public void call( final GraphIoEvent<Edge> edgeGraphIoEvent ) {
        final MutationBatch mutation =
                edgeMetadata.writeEdge( edgeGraphIoEvent.getOrganization(), edgeGraphIoEvent.getEvent() );

        final MutationBatch edgeMutation =
                edgeSerialization.writeEdge( edgeGraphIoEvent.getOrganization(), edgeGraphIoEvent.getEvent() );

        mutation.mergeShallow( edgeMutation );

        try {
            mutation.execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }
    }
}
