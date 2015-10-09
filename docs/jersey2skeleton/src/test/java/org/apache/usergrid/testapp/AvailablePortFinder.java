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
package org.apache.usergrid.testapp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;


/**
 * Finds currently available server ports.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @see <a href="http://www.iana.org/assignments/port-numbers">IANA.org</a>
 */
public class AvailablePortFinder {
	
	private static final Logger LOG = LoggerFactory.getLogger( AvailablePortFinder.class );
    /** The minimum number of server port number. */
    public static final int MIN_PORT_NUMBER = 1;

    /** The maximum number of server port number. */
    public static final int MAX_PORT_NUMBER = 49151;


    /** Creates a new instance. */
    private AvailablePortFinder() {
        // Do nothing
    }


    /**
     * Returns the {@link Set} of currently available port numbers ({@link Integer}).  This method is identical to
     * <code>getAvailablePorts(MIN_PORT_NUMBER, MAX_PORT_NUMBER)</code>.
     * <p/>
     * WARNING: this can take a very long time.
     */
    public static Set<Integer> getAvailablePorts() {
        return getAvailablePorts( MIN_PORT_NUMBER, MAX_PORT_NUMBER );
    }


    /**
     * Gets an available port, selected by the system.
     *
     * @throws NoSuchElementException if there are no ports available
     */
    public static int getNextAvailable() {
        ServerSocket serverSocket = null;

        try {
            // Here, we simply return an available port found by the system
            serverSocket = new ServerSocket( 0 );
            int port = serverSocket.getLocalPort();

            // Don't forget to close the socket...
            serverSocket.close();

            return port;
        }
        catch ( IOException ioe ) {
            throw new NoSuchElementException( ioe.getMessage() );
        }
    }


    /**
     * Gets the next available port starting at a port.
     *
     * @param fromPort the port to scan for availability
     *
     * @throws NoSuchElementException if there are no ports available
     */
    public static int getNextAvailable( int fromPort ) {
        if ( fromPort < MIN_PORT_NUMBER || fromPort > MAX_PORT_NUMBER ) {
            throw new IllegalArgumentException( "Invalid start port: " + fromPort );
        }

        for ( int i = fromPort; i <= MAX_PORT_NUMBER; i++ ) {
            if ( available( i ) ) {
                return i;
            }
        }

        throw new NoSuchElementException( "Could not find an available port " + "above " + fromPort );
    }


    /**
     * Checks to see if a specific port is available.
     *
     * @param port the port to check for availability
     */
    public static boolean available( int port ) {
        if ( port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER ) {
            throw new IllegalArgumentException( "Invalid start port: " + port );
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;

        try {
			// Jackson: It seems like the code below intends to
			// setReuseAddress(true), but that needs to be set before the bind.
			// The constructor for the ServerSocket(int) will bind, so not sure
			// how it would have been working as intended previously. 
        	
			// Changing ServerSocket constructor to use default constructor,
			// this would be unbound, then set the socket reuse, and
			// call the bind separately
        	
            //ss = new ServerSocket( port );
        	ss = new ServerSocket();
            ss.setReuseAddress( true );
            ss.bind(new InetSocketAddress((InetAddress) null, port), 0);
            
			// Unlike ServerSocket, the default constructor of DatagramSocket
			// will bound. To create an unbound DatagramSocket, use null address 
            //ds = new DatagramSocket( port );
            ds = new DatagramSocket(null);
            ds.setReuseAddress( true );
            ds.bind(new InetSocketAddress((InetAddress) null, port));
            LOG.info("port {} available", port);
            return true;
        }
        catch ( IOException e ) {
            // Do nothing
        }
        finally {
            if ( ds != null ) {
                ds.close();
            }

            if ( ss != null ) {
                try {
                    ss.close();
                }
                catch ( IOException e ) {
                    /* should not be thrown */
                }
            }
        }
        LOG.info("port {} unavailable", port);
        return false;
    }


    /**
     * Returns the {@link Set} of currently avaliable port numbers ({@link Integer}) between the specified port range.
     *
     * @throws IllegalArgumentException if port range is not between {@link #MIN_PORT_NUMBER} and {@link
     * #MAX_PORT_NUMBER} or <code>fromPort</code> if greater than <code>toPort</code>.
     */
    public static Set<Integer> getAvailablePorts( int fromPort, int toPort ) {
        if ( fromPort < MIN_PORT_NUMBER || toPort > MAX_PORT_NUMBER || fromPort > toPort ) {
            throw new IllegalArgumentException( "Invalid port range: " + fromPort + " ~ " + toPort );
        }

        Set<Integer> result = new TreeSet<Integer>();

        for ( int i = fromPort; i <= toPort; i++ ) {
            ServerSocket s = null;

            try {
                s = new ServerSocket( i );
                result.add(i);
            }
            catch ( IOException e ) {
                // Do nothing
            }
            finally {
                if ( s != null ) {
                    try {
                        s.close();
                    }
                    catch ( IOException e ) {
                        /* should not be thrown */
                    }
                }
            }
        }

        return result;
    }
}
