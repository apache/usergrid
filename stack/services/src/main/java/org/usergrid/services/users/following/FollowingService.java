package org.usergrid.services.users.following;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.ConnectionRef;
import org.usergrid.persistence.EntityRef;
import org.usergrid.services.AbstractConnectionsService;

public class FollowingService extends AbstractConnectionsService {

	private static final Logger logger = LoggerFactory
			.getLogger(FollowingService.class);

	@Override
	public ConnectionRef createConnection(EntityRef connectingEntity,
			String connectionType, EntityRef connectedEntityRef)
			throws Exception {
		copyActivityFeed(connectingEntity, connectedEntityRef);
		return em.createConnection(connectingEntity, connectionType,
				connectedEntityRef);
	}

	@Override
	public void deleteConnection(ConnectionRef connectionRef) throws Exception {
		em.deleteConnection(connectionRef);
	}

	public void copyActivityFeed(EntityRef connectingEntity,
			EntityRef connectedEntityRef) {
		logger.info("Copying activities...");
	}

}
