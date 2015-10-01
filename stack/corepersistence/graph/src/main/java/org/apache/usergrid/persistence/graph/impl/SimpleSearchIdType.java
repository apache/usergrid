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


import com.google.common.base.Optional;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 *
 */
public class SimpleSearchIdType extends SimpleSearchEdgeType implements SearchIdType {

    private final String edgeType;


    public SimpleSearchIdType( final Id node, final String edgeType, final String prefix, final String last ) {
        super(node, prefix, Optional.fromNullable(last));

        ValidationUtils.verifyString( edgeType, "edgeType" );
        this.edgeType = edgeType;
    }


    @Override
    public String getEdgeType() {
       return edgeType;
    }
}
