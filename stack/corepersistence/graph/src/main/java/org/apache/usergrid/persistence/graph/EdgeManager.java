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
     * Create or update an edge.  Note that the implementation should also create incoming (reversed) edges for this
     * edge automatically
     */
    void writeEdge( Edge e );


    /**
     * Delete the edge. Implementation should also delete the reversed edge
     */
    void deleteEdge( Edge e );

    /**
     * Load all edges where the specified node is the source.  The edges will match the search criteria
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges.  The node specified in the search will be on the source end of the edge.
     * The observer will need to unsubscribe when it has completed consumption.
     */
    Observable<Edge> loadSourceEdges( SearchByEdgeType search );

    /**
     * Load all edges where the node specified is the target node
     * @param search  The search criteria
     *
     * @return An observable that emits Edges.  The node specified in search will be on the target end of the edge
     * The observer will need to unsubscribe when it has completed consumption.
     */
    Observable<Edge> loadTargetEdges( SearchByEdgeType search );


    /**
     * Return an observable that emits edges where the passed search node is the source node
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges.  Note that only the target type in this edge type will be returned It is
     *         up to the caller to end the subscription to this observable when the desired size is reached
     */
    Observable<Edge> loadSourceEdges( SearchByIdType search );


    /**
     * Return an observable that emits edges where the passed search node is the target node
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges.  Note that only the target type in this edge type will be returned It
     *         is up to the caller to end the subscription to this observable when the desired size is reached
     */
    Observable<Edge> loadTargetEdges( SearchByIdType search );

    /**
     * Get all edge types to this node.  The node provided by search is the target type
     *
     * @param search The search
     *
     * @return An observable that emits strings for edge types
     */
    Observable<String> getSourceEdgeTypes(SearchEdgeTypes search );


    /**
     * Get all the types of all sources with the specified edge type into this node.  The node in the search
     * is the target node
     *
     * @param search The search criteria
     * @return   An observable of all source id types
     */
    Observable<String> getSourceEdgeIdTypes(SearchEdgeIdTypes search);


    /**
     * Get all edges where the search criteria is the source node
     *
     * @param search The search parameters
     * @return  An observable of all edges types the source node
     */
    Observable<String> getTargetEdgeTypes(SearchEdgeTypes search );


    /**
     * Get the types of all targets where the search criteria is the source node
     *
     * @param search
     * @return An observable of all target id types
     */
    Observable<String> getTargetEdgeIdTypes( SearchEdgeIdTypes search);
}
