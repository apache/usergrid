/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.corepersistence.results;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.cassandra.ConnectedEntityRefImpl;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import static org.apache.usergrid.persistence.SimpleEntityRef.ref;


/**
 * Verifier for creating connections
 */
public class ConnectionRefsVerifier extends VersionVerifier {


    private final EntityRef ownerId;
    private final String connectionType;


    public ConnectionRefsVerifier( final EntityRef ownerId, final String connectionType ) {
        this.ownerId = ownerId;
        this.connectionType = connectionType;
    }

    @Override
    public Results getResults( final Collection<Id> ids ) {
        List<ConnectionRef> refs = new ArrayList<>();
        for ( Id id : ids ) {
            refs.add( new ConnectionRefImpl( ownerId, connectionType, ref(id.getType(), id.getUuid())  ));
        }

        return Results.fromConnections( refs );
    }
}
