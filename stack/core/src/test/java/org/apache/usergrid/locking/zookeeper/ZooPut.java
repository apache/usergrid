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


import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;


/** Util for uploading and updating files in ZooKeeper. */
public class ZooPut implements Watcher {

    private ZooKeeper keeper;

    private boolean closeKeeper = true;

    private boolean connected = false;


    public ZooPut( String host ) throws IOException {
        keeper = new ZooKeeper( host, 10000, this );
        // TODO: nocommit: this is asynchronous - think about how to deal with
        // connection
        // lost, and other failures
        synchronized ( this ) {
            while ( !connected ) {
                try {
                    this.wait();
                }
                catch ( InterruptedException e ) {
                    // nocommit
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }


    public ZooPut( ZooKeeper keeper ) throws IOException {
        this.closeKeeper = false;
        this.keeper = keeper;
    }


    public void close() throws InterruptedException {
        if ( closeKeeper ) {
            keeper.close();
        }
    }


    public void makePath( String path ) throws KeeperException, InterruptedException {
        makePath( path, CreateMode.PERSISTENT );
    }


    public void makePath( String path, CreateMode createMode ) throws KeeperException, InterruptedException {
        // nocommit
        System.out.println( "make:" + path );

        if ( path.startsWith( "/" ) ) {
            path = path.substring( 1, path.length() );
        }
        String[] paths = path.split( "/" );
        StringBuilder sbPath = new StringBuilder();
        for ( int i = 0; i < paths.length; i++ ) {
            String pathPiece = paths[i];
            sbPath.append( "/" + pathPiece );
            String currentPath = sbPath.toString();
            Object exists = keeper.exists( currentPath, null );
            if ( exists == null ) {
                CreateMode mode = CreateMode.PERSISTENT;
                if ( i == paths.length - 1 ) {
                    mode = createMode;
                }
                keeper.create( currentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode );
            }
        }
    }


    @Override
    public void process( WatchedEvent event ) {
        // nocommit: consider how we want to accomplish this
        if ( event.getState() == KeeperState.SyncConnected ) {
            synchronized ( this ) {
                connected = true;
                this.notify();
            }
        }
    }
}
