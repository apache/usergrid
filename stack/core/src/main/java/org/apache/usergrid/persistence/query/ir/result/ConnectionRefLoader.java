/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.query.ir.result;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;


/**
 *
 * @author: tnine
 *
 */
public class ConnectionRefLoader implements ResultsLoader {

    private final UUID sourceEntityId;
    private final String sourceType;
    private final String connectionType;
    private final String targetEntityType;


    public ConnectionRefLoader( ConnectionRef connectionRef ) {
        this.sourceType = connectionRef.getConnectingEntity().getType();
        this.sourceEntityId = connectionRef.getConnectingEntity().getUuid();
        this.connectionType = connectionRef.getConnectionType();
        this.targetEntityType = connectionRef.getConnectedEntity().getType();
    }


    @Override
    public Results getResults( List<ScanColumn> entityIds, String type ) throws Exception {


        final EntityRef sourceRef = new SimpleEntityRef( sourceType, sourceEntityId );

        List<ConnectionRef> refs = new ArrayList<ConnectionRef>( entityIds.size() );

        for ( ScanColumn column : entityIds ) {

            SimpleEntityRef targetRef;

            if ( column instanceof ConnectionIndexSliceParser.ConnectionColumn ) {
                final ConnectionIndexSliceParser.ConnectionColumn connectionColumn =
                        ( ConnectionIndexSliceParser.ConnectionColumn ) column;
                targetRef = new SimpleEntityRef( connectionColumn.getTargetType(), connectionColumn.getUUID() );
            }

            else {
                targetRef = new SimpleEntityRef( targetEntityType, column.getUUID() );
            }

            final ConnectionRef ref = new ConnectionRefImpl( sourceRef, connectionType, targetRef );

            refs.add( ref );
        }

        return Results.fromConnections( refs );
    }
}
