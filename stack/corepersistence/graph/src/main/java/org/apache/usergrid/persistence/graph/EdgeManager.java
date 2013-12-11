package org.apache.usergrid.persistence.graph;


import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 * Represents operations that can be performed on edges
 *
 * @author tnine
 */
public interface EdgeManager {


    /**
     * Create or update an edge.  Note that the implementation should also create incoming (reversed) edges for this edge
     * automatically
     */
    void writeEdge( Edge e );


    /** Delete the edge. Implementation should also delete the reversed edge */
    void deleteEdge( Edge e );

    /**
     * Return an observable that emits edges with the given source node and the given edge type
     *
     * @param source The id of the source node in the graph
     * @param edgeType The type of the edge to return.
     *
     * @return An observable that emits Edges.  Note that all target types in this edge type will be returned
     */
    Observable<Edge> loadEdges( Id source, String edgeType);

    /**
     * Return an observable that emits edges with the given source node and the given edge type, and target type
     *
     * @param source The id of the source node in the graph
     * @param edgeType The type of the edge to return.
     * @param targetType The type of the target on the edge to return
     *
     * @return An observable that emits Edges.  Note that only the target type in this edge type will be returned
     * It is up to the caller to end the subscription to this observable when the desired size is reached
     */
    Observable<Edge> loadEdges( Id source, String edgeType, String targetType);

    /**
     * Search the edges from the source node, with the edge type and targetType specified.
     * @param source The id of the source node in the graph
     * @param edgeType The type of edge to execute the query on
     * @param targetType The target type to execute the query on
     * @param query The query containing tree expression to execute
     *
     * @return An observable that emits a matching edge.
     */
    Observable<Edge> searchEdges(Id source, String edgeType, String targetType, Query query);
}
