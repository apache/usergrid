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

import java.io.File;
import java.net.InetSocketAddress;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test class for ZooKeeper tests.
 */
public abstract class AbstractZooKeeperTest {

	public static final String ZOO_KEEPER_HOST = "localhost:2181/";

	protected static Logger logger = LoggerFactory
			.getLogger(AbstractZooKeeperTest.class);

	static class ZKServerMain extends ZooKeeperServerMain {
		@Override
		public void shutdown() {
			super.shutdown();
		}
	}

	protected static ZKServerMain zkServer = new ZKServerMain();

	protected static File tmpDir = new File("./tmp");

	protected int clientPort;

	@BeforeClass
	public static void before() throws Exception {
		// we don't call super.setUp
		System.setProperty("zkHost", ZOO_KEEPER_HOST);
		Thread zooThread = new Thread() {
			@Override
			public void run() {
				ServerConfig config = null;

				config = new ServerConfig() {
					{
						clientPortAddress = new InetSocketAddress("localhost",
								2181);
						dataDir = tmpDir.getAbsolutePath() + File.separator
								+ "zookeeper/server1/data";
						dataLogDir = dataDir;
						// this.maxClientCnxns = 50;
						// this.tickTime = 2000;
					}
				};

				try {
					zkServer.runFromConfig(config);
					logger.info("ZOOKEEPER EXIT");
				} catch (Throwable e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		};

		zooThread.setDaemon(true);
		zooThread.start();
		Thread.sleep(500); // pause for ZooKeeper to start

		buildZooKeeper();

		logger.info("Zookeeper initialized.");
	}

	public static void buildZooKeeper() throws Exception {
		ZooPut zooPut = new ZooPut(ZOO_KEEPER_HOST.substring(0,
				ZOO_KEEPER_HOST.indexOf('/')));
		// TODO read a system property to get the app root path if
		// needed.
		// zooPut.makePath("/somepath");
		zooPut.close();
	}

	@AfterClass
	public static void after() throws Exception {
		zkServer.shutdown();

		// Remove test data.
		boolean deletedData = recurseDelete(tmpDir);
		if (!deletedData) {
			logger.warn("Zk testing data was not removed properly. You need to"
					+ "manually remove:" + tmpDir.getAbsolutePath());
		}
	}

	public static boolean recurseDelete(File f) {
		if (f.isDirectory()) {
			for (File sub : f.listFiles()) {
				if (!recurseDelete(sub)) {
					return false;
				}
			}
		}
		return f.delete();
	}
}
