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
package org.apache.usergrid.persistence.graph.impl;


import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;


/**
 *
 *
 */
public class SimpleSearchEdgeType implements SearchEdgeType {

    private final Id node;
    private final Optional<String> prefix;
    private final Optional<String> last;


    /**
     * The node's id, the prefix of the string, the last value returned
     * @param node The node to search from (required)
     * @param prefix The optional prefix
     * @param last The optional last
     */
    public SimpleSearchEdgeType( final Id node, final String prefix, final Optional<String> last ) {
        ValidationUtils.verifyIdentity( node );
        this.node = node;
        this.prefix =  Optional.fromNullable( prefix );
        this.last = last == null ? Optional.<String>absent() : last;
    }


    @Override
    public Id getNode() {
        return node;
    }


    @Override
    public Optional<String> prefix() {
        return prefix;
    }


    @Override
    public Optional<String> getLast() {
        return last;
    }
}
