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
package org.apache.usergrid.locking.zookeeper;


import java.io.File;
import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;


/** Base test class for ZooKeeper tests. */
public abstract class AbstractZooKeeperTest {

    public static final String ZOO_KEEPER_HOST = "localhost:20181/";

    private static Logger LOG = LoggerFactory.getLogger( AbstractZooKeeperTest.class );


    static class ZKServerMain extends ZooKeeperServerMain {
        @Override
        public void shutdown() {
            super.shutdown();
        }
    }


    protected static ZKServerMain zkServer = new ZKServerMain();

    protected static File tmpDir = new File( "./zk_tmp" );

    protected int clientPort;


    @BeforeClass
    public static void before() throws Exception {
        // we don't call super.setUp
        System.setProperty( "zkHost", ZOO_KEEPER_HOST );
        Thread zooThread = new Thread() {
            @Override
            public void run() {
                ServerConfig config = null;

                config = new ServerConfig() {
                    {
                        clientPortAddress = new InetSocketAddress( "localhost", 20181 );
                        dataDir = tmpDir.getAbsolutePath() + File.separator + "zookeeper/server1/data";
                        dataLogDir = dataDir;
                        // this.maxClientCnxns = 50;
                        // this.tickTime = 2000;
                    }
                };

                try {
                    zkServer.runFromConfig( config );
                    LOG.info( "ZOOKEEPER EXIT" );
                }
                catch ( Throwable e ) {
                    e.printStackTrace();
                    throw new RuntimeException( e );
                }
            }
        };

        zooThread.setDaemon( true );
        zooThread.start();
        Thread.sleep( 500 ); // pause for ZooKeeper to start

        buildZooKeeper();

        LOG.info( "Zookeeper initialized." );
    }


    public static void buildZooKeeper() throws Exception {
        ZooPut zooPut = new ZooPut( ZOO_KEEPER_HOST.substring( 0, ZOO_KEEPER_HOST.indexOf( '/' ) ) );
        // TODO read a system property to get the app root path if
        // needed.
        // zooPut.makePath("/somepath");
        zooPut.close();
    }


    @AfterClass
    public static void after() throws Exception {
        zkServer.shutdown();

        // Remove test data.
        boolean deletedData = recurseDelete( tmpDir );
        if ( !deletedData ) {
            LOG.warn( "Zk testing data was not removed properly. You need to" + "manually remove:" + tmpDir
                    .getAbsolutePath() );
        }
    }


    public static boolean recurseDelete( File f ) {
        if ( f.isDirectory() ) {
            for ( File sub : f.listFiles() ) {
                if ( !recurseDelete( sub ) ) {
                    return false;
                }
            }
        }
        return f.delete();
    }
}
