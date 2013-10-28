package org.usergrid.persistence.query.ir.result;

import org.usergrid.persistence.*;
import org.usergrid.persistence.cassandra.ConnectionRefImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

  public ConnectionRefLoader(ConnectionRef connectionRef) {
    this.sourceType = connectionRef.getConnectingEntity().getType();
    this.sourceEntityId = connectionRef.getConnectingEntity().getUuid();
    this.connectionType = connectionRef.getConnectionType();
    this.targetEntityType = connectionRef.getConnectedEntity().getType();
  }

  @Override
  public Results getResults(List<UUID> entityIds) throws Exception {


    final EntityRef sourceRef = new SimpleEntityRef(sourceType, sourceEntityId);

    List<ConnectionRef> refs = new ArrayList<ConnectionRef>(entityIds.size());
    for (UUID id : entityIds) {

      final EntityRef targetRef = new SimpleEntityRef(targetEntityType, id);

      final ConnectionRef ref = new ConnectionRefImpl(sourceRef, connectionType, targetRef);

      refs.add(ref);
    }

    return Results.fromConnections(refs);
  }
}
