package org.usergrid.persistence.query.ir.result;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.usergrid.persistence.ConnectionRef;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.ConnectionRefImpl;


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
    public Results getResults( List<ScanColumn> entityIds ) throws Exception {


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
