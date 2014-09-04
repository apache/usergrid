/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.HashMap;


/**
 * The node type of the source or target
 */
public enum NodeType {
    SOURCE( 0 ),
    TARGET( 1 );

    private final int ordinal;


    private NodeType( final int ordinal ) {this.ordinal = ordinal;}


    public int getStorageValue() {
        return ordinal;
    }


    /**
     * Get the type from the storageValue value
     * @param storageValue
     * @return
     */
    public static NodeType get(final int storageValue){
     return types.get( storageValue );
    }


    /**
     * Internal map and initialization for fast access
     */
    private static final HashMap<Integer, NodeType> types;


    static{
        types = new HashMap<>();

        for(NodeType type: NodeType.values()){
            types.put( type.getStorageValue(), type );
        }
    }
}
