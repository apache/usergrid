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
package org.usergrid.locking.zookeeper;

import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.KeeperState;

/**
 * Util for uploading and updating files in ZooKeeper.
 *
 */
public class ZooPut implements Watcher {

	private ZooKeeper keeper;

	private boolean closeKeeper = true;

	private boolean connected = false;

	public ZooPut(String host) throws IOException {
		keeper = new ZooKeeper(host, 10000, this);
		// TODO: nocommit: this is asynchronous - think about how to deal with
		// connection
		// lost, and other failures
		synchronized (this) {
			while (!connected) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					// nocommit
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public ZooPut(ZooKeeper keeper) throws IOException {
		this.closeKeeper = false;
		this.keeper = keeper;
	}

	public void close() throws InterruptedException {
		if (closeKeeper) {
			keeper.close();
		}
	}

	public void makePath(String path) throws KeeperException,
			InterruptedException {
		makePath(path, CreateMode.PERSISTENT);
	}

	public void makePath(String path, CreateMode createMode)
			throws KeeperException, InterruptedException {
		// nocommit
		System.out.println("make:" + path);

		if (path.startsWith("/")) {
			path = path.substring(1, path.length());
		}
		String[] paths = path.split("/");
		StringBuilder sbPath = new StringBuilder();
		for (int i = 0; i < paths.length; i++) {
			String pathPiece = paths[i];
			sbPath.append("/" + pathPiece);
			String currentPath = sbPath.toString();
			Object exists = keeper.exists(currentPath, null);
			if (exists == null) {
				CreateMode mode = CreateMode.PERSISTENT;
				if (i == paths.length - 1) {
					mode = createMode;
				}
				keeper.create(currentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE,
						mode);
			}
		}
	}


	@Override
	public void process(WatchedEvent event) {
		// nocommit: consider how we want to accomplish this
		if (event.getState() == KeeperState.SyncConnected) {
			synchronized (this) {
				connected = true;
				this.notify();
			}
		}

	}

}
