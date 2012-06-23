package org.usergrid.services.users.following;

import org.usergrid.persistence.ConnectionRef;
import org.usergrid.persistence.EntityRef;
import org.usergrid.services.AbstractConnectionsService;

public class FollowingService extends AbstractConnectionsService {

	@Override
	public ConnectionRef createConnection(EntityRef connectingEntity,
			String connectionType, EntityRef connectedEntityRef)
			throws Exception {
		return em.createConnection(connectingEntity, connectionType,
				connectedEntityRef);
	}

	@Override
	public void deleteConnection(ConnectionRef connectionRef) throws Exception {
		em.deleteConnection(connectionRef);
	}

}
