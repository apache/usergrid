/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.usergrid.clustering.hazelcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Instance;
import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

public class HazelcastLifecycleMonitor implements InstanceListener,
		MembershipListener {

	private static final Logger logger = LoggerFactory
			.getLogger(HazelcastLifecycleMonitor.class);

	public HazelcastLifecycleMonitor() {
	}

	public void init() {
		logger.info("HazelcastLifecycleMonitor initializing...");
		Hazelcast.addInstanceListener(this);
		Hazelcast.getCluster().addMembershipListener(this);
		logger.info("HazelcastLifecycleMonitor initialized");
	}

	public void destroy() {
		logger.info("Shutting down Hazelcast");
		Hazelcast.shutdownAll();
		logger.info("Hazelcast shutdown");
	}

	@Override
	public void instanceCreated(InstanceEvent event) {
		Instance instance = event.getInstance();
		logger.info("Created instance ID: [" + instance.getId() + "] Type: ["
				+ instance.getInstanceType() + "]");
	}

	@Override
	public void instanceDestroyed(InstanceEvent event) {
		Instance instance = event.getInstance();
		logger.info("Destroyed isntance ID: [" + instance.getId() + "] Type: ["
				+ instance.getInstanceType() + "]");

	}

	@Override
	public void memberAdded(MembershipEvent membersipEvent) {
		logger.info("MemberAdded " + membersipEvent);
	}

	@Override
	public void memberRemoved(MembershipEvent membersipEvent) {
		logger.info("MemberRemoved " + membersipEvent);
	}

}
