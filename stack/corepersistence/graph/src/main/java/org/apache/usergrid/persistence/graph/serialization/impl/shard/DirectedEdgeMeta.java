/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.netflix.astyanax.MutationBatch;


/**
 * A bean to define directed edge meta data.  This is used to encapsulate the meta data around a source or target node,
 * and the types used for grouping them.
 */
public abstract class DirectedEdgeMeta {


    protected final NodeMeta[] nodes;
    protected final String[] types;


    private DirectedEdgeMeta( NodeMeta[] nodes, String[] types ) {
        this.nodes = nodes;
        this.types = types;
    }


    public NodeMeta[] getNodes() {
        return nodes;
    }


    public String[] getTypes() {
        return types;
    }


    /**
     * Inner class to represent node meta dat
     */
    public static class NodeMeta {
        private final Id id;
        private final NodeType nodeType;


        public NodeMeta( final Id id, final NodeType nodeType ) {
            this.id = id;
            this.nodeType = nodeType;
        }


        public Id getId() {
            return id;
        }


        public NodeType getNodeType() {
            return nodeType;
        }


        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( !( o instanceof NodeMeta ) ) {
                return false;
            }

            final NodeMeta nodeMeta = ( NodeMeta ) o;

            if ( !id.equals( nodeMeta.id ) ) {
                return false;
            }
            if ( nodeType != nodeMeta.nodeType ) {
                return false;
            }

            return true;
        }


        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + nodeType.hashCode();
            return result;
        }
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final DirectedEdgeMeta that = ( DirectedEdgeMeta ) o;

        if ( !Arrays.equals( nodes, that.nodes ) ) {
            return false;
        }
        if ( !Arrays.equals( types, that.types ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = Arrays.hashCode( nodes );
        result = 31 * result + Arrays.hashCode( types );
        return result;
    }


    @Override
    public String toString() {
        return "DirectedEdgeMeta{" +
                "nodes=" + Arrays.toString( nodes ) +
                ", types=" + Arrays.toString( types ) +
                '}';
    }


    /**
     * Given the edge serialization, load all shard in the shard group
     */
    public abstract Iterator<MarkedEdge> loadEdges( final ShardedEdgeSerialization serialization,
                                                    final EdgeColumnFamilies edgeColumnFamilies,
                                                    final ApplicationScope scope, final Collection<Shard> shards,
                                                    final long maxValue, final SearchByEdgeType.Order order );


    /**
     * Write the edge for this meta data to the target edge
     * @param shardedEdgeSerialization
     * @param edgeColumnFamilies
     * @param scope
     * @param targetShard
     * @param edge
     * @param timestamp The timestamp on the operation
     * @return
     */
    public abstract MutationBatch writeEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                             final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                             final Shard targetShard, final MarkedEdge edge, final UUID timestamp );


    /**
     * Delete the edge for this meta data from the shard
     * @param shardedEdgeSerialization
     * @param edgeColumnFamilies
     * @param scope
     * @param sourceShard
     * @param edge
     * @param timestamp The timestamp on the operation
     * @return
     */
    public abstract MutationBatch deleteEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                              final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                              final Shard sourceShard, final MarkedEdge edge, final UUID timestamp );


    /**
     * Get the type of this directed edge
     */
    public abstract MetaType getType();


    public enum MetaType {
        SOURCE( 0 ),
        SOURCETARGET( 1 ),
        TARGET( 2 ),
        TARGETSOURCE( 3 ),
        VERSIONS( 4 );

        private final int storageValue;


        MetaType( final int storageValue ) {this.storageValue = storageValue;}


        public int getStorageValue() {
            return storageValue;
        }


        /**
         * Get value from storageValue
         */
        public static MetaType fromStorage( final int ordinal ) {
            return mappings.get( ordinal );
        }


        private static Map<Integer, MetaType> mappings = new HashMap<Integer, MetaType>();


        static {

            for ( MetaType meta : MetaType.values() ) {
                mappings.put( meta.storageValue, meta );
            }
        }
    }


    /**
     * Created directed edge meta data from source node
     */
    public static DirectedEdgeMeta fromSourceNode( final Id sourceId, final String edgeType ) {
        return fromSourceNode(
                new DirectedEdgeMeta.NodeMeta[] { new DirectedEdgeMeta.NodeMeta( sourceId, NodeType.SOURCE ) },
                new String[] { edgeType } );
    }


    /**
     * Return meta data from the source node by edge type
     */
    private static DirectedEdgeMeta fromSourceNode( final NodeMeta[] nodes, final String[] types ) {

        return new DirectedEdgeMeta( nodes, types ) {

            @Override
            public Iterator<MarkedEdge> loadEdges( final ShardedEdgeSerialization serialization,
                                                   final EdgeColumnFamilies edgeColumnFamilies,
                                                   final ApplicationScope scope, final Collection<Shard> shards,
                                                   final long maxValue, final SearchByEdgeType.Order order ) {

                final Id sourceId = nodes[0].id;
                final String edgeType = types[0];

                final SearchByEdgeType search = new SimpleSearchByEdgeType( sourceId, edgeType, maxValue, order,
                    Optional.<Edge>absent());

                return serialization.getEdgesFromSource( edgeColumnFamilies, scope, search, shards );
            }


            @Override
            public MutationBatch writeEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                            final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                            final Shard targetShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization
                        .writeEdgeFromSource( edgeColumnFamilies, scope, edge, Collections.singleton( targetShard ),
                                this, timestamp );
            }


            @Override
            public MutationBatch deleteEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                             final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                             final Shard sourceShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization
                        .deleteEdgeFromSource( edgeColumnFamilies, scope, edge, Collections.singleton( sourceShard ),
                                this, timestamp );
            }


            @Override
            public MetaType getType() {
                return MetaType.SOURCE;
            }
        };
    }


    /**
     * Return meta data that represents a source node with edge type and target type
     */
    public static DirectedEdgeMeta fromSourceNodeTargetType( final Id sourceId, final String edgeType,
                                                             final String targetType ) {
        return fromSourceNodeTargetType(
                new DirectedEdgeMeta.NodeMeta[] { new DirectedEdgeMeta.NodeMeta( sourceId, NodeType.SOURCE ) },
                new String[] { edgeType, targetType } );
    }


    /**
     * Return meta data that represents a source node with edge type and target type
     */
    private static DirectedEdgeMeta fromSourceNodeTargetType( NodeMeta[] nodes, String[] types ) {
        return new DirectedEdgeMeta( nodes, types ) {

            @Override
            public Iterator<MarkedEdge> loadEdges( final ShardedEdgeSerialization serialization,
                                                             final EdgeColumnFamilies edgeColumnFamilies,
                                                             final ApplicationScope scope, final Collection<Shard> shards,
                                                             final long maxValue, final SearchByEdgeType.Order order ) {
                //
                final Id sourceId = nodes[0].id;
                final String edgeType = types[0];
                final String targetType = types[1];

                final SearchByIdType search =
                        new SimpleSearchByIdType( sourceId, edgeType, maxValue, order, targetType,  Optional.<Edge>absent() );

                return serialization.getEdgesFromSourceByTargetType( edgeColumnFamilies, scope, search, shards );
            }





            @Override
            public MutationBatch writeEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                            final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                            final Shard targetShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization.writeEdgeFromSourceWithTargetType( edgeColumnFamilies, scope, edge,
                        Collections.singleton( targetShard ), this, timestamp );
            }


            @Override
            public MutationBatch deleteEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                             final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                             final Shard sourceShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization.deleteEdgeFromSourceWithTargetType( edgeColumnFamilies, scope, edge,
                        Collections.singleton( sourceShard ), this, timestamp );
            }


            @Override
            public MetaType getType() {
                return MetaType.SOURCETARGET;
            }
        };
    }


    public static DirectedEdgeMeta fromTargetNode( final Id targetId, final String edgeType ) {
        return fromTargetNode(
                new DirectedEdgeMeta.NodeMeta[] { new DirectedEdgeMeta.NodeMeta( targetId, NodeType.TARGET ) },
                new String[] { edgeType } );
    }


    /**
     * Return meta data that represents from a target node by edge type
     */
    private static DirectedEdgeMeta fromTargetNode( final NodeMeta[] nodes, final String[] types ) {
        return new DirectedEdgeMeta( nodes, types ) {

            @Override
            public Iterator<MarkedEdge> loadEdges( final ShardedEdgeSerialization serialization,
                                                   final EdgeColumnFamilies edgeColumnFamilies,
                                                   final ApplicationScope scope, final Collection<Shard> shards,
                                                   final long maxValue, final SearchByEdgeType.Order order ) {


                final Id targetId = nodes[0].id;
                final String edgeType = types[0];

                final SearchByEdgeType search = new SimpleSearchByEdgeType( targetId, edgeType, maxValue, order,  Optional.<Edge>absent());

                return serialization.getEdgesToTarget( edgeColumnFamilies, scope, search, shards );
            }


            @Override
            public MutationBatch writeEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                            final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                            final Shard targetShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization
                        .writeEdgeToTarget( edgeColumnFamilies, scope, edge, Collections.singleton( targetShard ),
                                this, timestamp );
            }


            @Override
            public MutationBatch deleteEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                             final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                             final Shard sourceShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization
                        .deleteEdgeToTarget( edgeColumnFamilies, scope, edge, Collections.singleton( sourceShard ),
                                this, timestamp );
            }


            @Override
            public MetaType getType() {
                return MetaType.TARGET;
            }
        };
    }


    public static DirectedEdgeMeta fromTargetNodeSourceType( final Id targetId, final String edgeType,
                                                             final String sourceType ) {
        return fromTargetNodeSourceType(
                new DirectedEdgeMeta.NodeMeta[] { new DirectedEdgeMeta.NodeMeta( targetId, NodeType.TARGET ) },
                new String[] { edgeType, sourceType } );
    }


    /**
     * Return meta data that represents a target node and a source node type
     */
    private static DirectedEdgeMeta fromTargetNodeSourceType( final NodeMeta[] nodes, final String[] types ) {
        return new DirectedEdgeMeta( nodes, types ) {


            @Override
            public Iterator<MarkedEdge> loadEdges( final ShardedEdgeSerialization serialization,
                                                   final EdgeColumnFamilies edgeColumnFamilies,
                                                   final ApplicationScope scope, final Collection<Shard> shards,
                                                   final long maxValue, final SearchByEdgeType.Order order ) {


                final Id targetId = nodes[0].id;
                final String edgeType = types[0];
                final String sourceType = types[1];


                final SearchByIdType search =
                        new SimpleSearchByIdType( targetId, edgeType, maxValue, order, sourceType,  null );

                return serialization.getEdgesToTargetBySourceType( edgeColumnFamilies, scope, search, shards );
            }


            @Override
            public MutationBatch writeEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                            final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                            final Shard targetShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization.writeEdgeToTargetWithSourceType( edgeColumnFamilies, scope, edge,
                        Collections.singleton( targetShard ), this, timestamp );
            }


            @Override
            public MutationBatch deleteEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                             final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                             final Shard sourceShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization.deleteEdgeToTargetWithSourceType( edgeColumnFamilies, scope, edge,
                        Collections.singleton( sourceShard ), this, timestamp );
            }


            @Override
            public MetaType getType() {
                return MetaType.TARGETSOURCE;
            }
        };
    }


    /**
     * Return meta data that represents an entire edge
     */
    public static DirectedEdgeMeta fromEdge( final Id sourceId, final Id targetId, final String edgeType ) {
        return fromEdge( new DirectedEdgeMeta.NodeMeta[] {
                new DirectedEdgeMeta.NodeMeta( sourceId, NodeType.SOURCE ),
                new DirectedEdgeMeta.NodeMeta( targetId, NodeType.TARGET )
        }, new String[] { edgeType } );
    }


    /**
     * Return meta data that represents an entire edge
     */
    private static DirectedEdgeMeta fromEdge( final NodeMeta[] nodes, final String[] types ) {
        return new DirectedEdgeMeta( nodes, types ) {

            @Override
            public Iterator<MarkedEdge> loadEdges( final ShardedEdgeSerialization serialization,
                                                   final EdgeColumnFamilies edgeColumnFamilies,
                                                   final ApplicationScope scope, final Collection<Shard> shards,
                                                   final long maxValue, final SearchByEdgeType.Order order ) {

                final Id sourceId = nodes[0].id;
                final Id targetId = nodes[1].id;
                final String edgeType = types[0];

                final SimpleSearchByEdge search =
                        new SimpleSearchByEdge( sourceId, edgeType, targetId, maxValue, order, null );

                return serialization.getEdgeVersions( edgeColumnFamilies, scope, search, shards );
            }


            @Override
            public MutationBatch writeEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                            final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                            final Shard targetShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization
                        .writeEdgeVersions( edgeColumnFamilies, scope, edge, Collections.singleton( targetShard ),
                                this, timestamp );
            }


            @Override
            public MutationBatch deleteEdge( final ShardedEdgeSerialization shardedEdgeSerialization,
                                             final EdgeColumnFamilies edgeColumnFamilies, final ApplicationScope scope,
                                             final Shard sourceShard, final MarkedEdge edge, final UUID timestamp ) {
                return shardedEdgeSerialization
                        .deleteEdgeVersions( edgeColumnFamilies, scope, edge, Collections.singleton( sourceShard ),
                                this, timestamp );
            }


            @Override
            public MetaType getType() {
                return MetaType.VERSIONS;
            }
        };
    }


    /**
     * Create a directed edge from the stored meta data
     *
     * @param metaType The meta type stored
     * @param nodes The metadata of the nodes
     * @param types The types in the meta data
     */
    public static DirectedEdgeMeta fromStorage( final MetaType metaType, final NodeMeta[] nodes,
                                                final String[] types ) {
        switch ( metaType ) {
            case SOURCE:
                return fromSourceNode( nodes, types );
            case SOURCETARGET:
                return fromSourceNodeTargetType( nodes, types );
            case TARGET:
                return fromTargetNode( nodes, types );
            case TARGETSOURCE:
                return fromTargetNodeSourceType( nodes, types );
            case VERSIONS:
                return fromEdge( nodes, types );
            default:
                throw new UnsupportedOperationException( "No supported meta type found" );
        }
    }
}
