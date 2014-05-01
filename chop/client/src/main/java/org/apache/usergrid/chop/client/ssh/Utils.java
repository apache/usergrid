package org.apache.usergrid.chop.client.ssh;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;


public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger( Utils.class );

    private static final int SESSION_CONNECT_TIMEOUT = 50000;

    public static final String DEFAULT_USER = "ubuntu";


    public static synchronized Session getSession( String hostURL, String keyFile ) {
        JSch ssh;
        Session session = null;

        boolean successful = waitActive( hostURL, 22, SESSION_CONNECT_TIMEOUT );
        if( ! successful ) {
            LOG.warn( "Can't reach ssh port of host {}", hostURL );
        }

        // try to open ssh session
        try {
            ssh = new JSch();
            ssh.addIdentity( keyFile );
            session = ssh.getSession( DEFAULT_USER, hostURL );
            session.setConfig( "StrictHostKeyChecking", "no" );
            session.connect();

            // should be successful, so we can continue
            return session;
        }
        catch ( Exception e ) {
            LOG.error( "Error while connecting to ssh session of " + hostURL, e );
        }
        finally {
            try {
                if( session != null ) {
                    session.disconnect();
                }
            }
            catch ( Exception ee ) { }
        }
        return null;
    }


    public static boolean waitActive( String hostURL, int port, int timeout ) {
        LOG.info( "Waiting maximum {} msecs for SSH port of {} to get active", timeout, hostURL );
        long startTime = System.currentTimeMillis();

        while ( System.currentTimeMillis() - startTime < timeout ) {
            Socket s = null;
            try {
                s = new Socket();
                s.setReuseAddress( true );
                SocketAddress sa = new InetSocketAddress( hostURL, port );
                s.connect( sa, 2000 );
                return true;
            }
            catch ( Exception e ) {
            }
            finally {
                if ( s != null ) {
                    try {
                        s.close();
                    }
                    catch ( IOException e ) {
                    }
                }
            }
        }
        return false;
    }


    public static String checkAck( InputStream in ) throws IOException {
        int b = in.read();

        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if( b == 0 || b == -1 ) {
            return null;
        }

        if( b == 1 || b == 2 ) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c=in.read();
                sb.append((char)c);
            }
            while ( c != '\n' );

            return sb.toString();
        }
        throw new RuntimeException( "Invalid value, this shouldn't have gotten here" );
    }


    public static String convertToNumericalForm( String fileMode ) {

        if ( fileMode.length() != 9 ) {
            throw new RuntimeException( "File mode string should be 9 characters long: " + fileMode );
        }

        int[] permissions = new int[3];

        for( int i = 0; i < 3; i++ ) {
            if( fileMode.charAt( i * 3 ) == 'r' ) {
                permissions[ i ] += 4;
            }
            if( fileMode.charAt( i * 3 + 1 ) == 'w' ) {
                permissions[ i ] += 2;
            }
            if( fileMode.charAt( i * 3 + 2 ) == 'x' ) {
                permissions[ i ] += 1;
            }
        }

        StringBuilder sb = new StringBuilder( 3 );
        return sb.append( permissions[ 0 ] )
                 .append( permissions[1] )
                 .append( permissions[2] )
                 .toString();
    }
}
