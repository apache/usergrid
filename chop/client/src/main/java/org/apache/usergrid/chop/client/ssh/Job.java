/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.client.ssh;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.api.SshValues;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.apache.commons.lang.NotImplementedException;


public class Job implements Callable<ResponseInfo> {

    private static final Logger LOG = LoggerFactory.getLogger( Job.class );

    protected static final int SESSION_CONNECT_TIMEOUT = 100000;

    protected Collection<Command> commands;
    protected SshValues value;
    private Session session = null;


    protected Job() {

    }


    public Job( Collection<Command> commands, SshValues value ) {
        this.commands = commands;
        this.value = value;
    }


    private void setSession() {
        JSch ssh;
        // wait until SSH port of remote end comes up
        boolean success = waitActive( SESSION_CONNECT_TIMEOUT );
        if( ! success ) {
            LOG.warn( "Port 22 of {} did not open in time", value.getPublicIpAddress() );
        }

        // try to open ssh session
        try {
            Thread.sleep( 30000 );
            ssh = new JSch();
            ssh.addIdentity( value.getSshKeyFile() );
            session = ssh.getSession( Utils.DEFAULT_USER, value.getPublicIpAddress() );
            session.setConfig( "StrictHostKeyChecking", "no" );
            session.connect();
        }
        catch ( Exception e ) {
            LOG.error( "Error while connecting to ssh session of " + value.getPublicIpAddress(), e );
            session = null;
        }
    }


    @Override
    public ResponseInfo call() throws Exception {
        ResponseInfo response = new ResponseInfo( value.getPublicIpAddress() );
        setSession();
        if( session == null ) {
            String message = "Could not open ssh session for " + value.getPublicIpAddress();
            response.addErrorMessage( message );
            return response;
        }

        for( Command command: commands ) {
            if( command instanceof SCPCommand ) {
                executeScp( ( SCPCommand ) command, session, response );
            }
            else if( command instanceof SSHCommand ) {
                executeSsh( ( SSHCommand ) command, session, response );
            }
        }
        return response;
    }


    private void executeSsh( SSHCommand command, Session session, ResponseInfo response ) {
        Channel channel = null;
        String message;
        try {
            channel = session.openChannel( "exec" );
            ( ( ChannelExec ) channel ).setCommand( command.getCommand() );
            channel.connect();

            BufferedReader inputReader = new BufferedReader( new InputStreamReader( channel.getInputStream() ) );
            BufferedReader errorReader = new BufferedReader( new InputStreamReader(
                    ( ( ChannelExec ) channel ).getErrStream() ) );

            while ( ( message = inputReader.readLine() ) != null ) {
                response.addMessage( message );
                LOG.info( "SSH command response: {}", message );
            }
            while ( ( message = errorReader.readLine() ) != null ) {
                response.addMessage( message );
                LOG.info( "Error in ssh command: {}", message );
            }

            inputReader.close();
            errorReader.close();
        }
        catch ( Exception e ) {
            message = "Error while sending ssh command to " + value.getPublicIpAddress();
            LOG.warn( message, e );
            response.addErrorMessage( message );
        }
        finally {
            try {
                if ( channel != null ) {
                    channel.disconnect();
                }
            }
            catch ( Exception e ) { }
        }
    }


    private void executeScp( SCPCommand command, Session session, ResponseInfo response ) {
        Channel channel = null;
        FileInputStream fis = null;
        OutputStream out = null;
        InputStream in = null;
        String message;
        try {
            // exec 'scp -t destFile' remotely
            String exec = "scp -t " + command.getDestinationFilePath();
            channel = session.openChannel( "exec" );
            ( ( ChannelExec ) channel ).setCommand( exec );

            // get I/O streams for remote scp
            out = channel.getOutputStream();
            in = channel.getInputStream();

            channel.connect();

            if( ( message = Utils.checkAck( in ) ) != null ) {
                response.addErrorMessage( message );
                return;
            }

            File srcFile = new File( command.getSourceFilePath() );

            // send "C0<filemode> filesize filename", where filename should not include '/'
            StringBuilder sb = new StringBuilder();
            String fileMode = PosixFilePermissions.toString( Files.getPosixFilePermissions( srcFile.toPath() ) );
            long filesize = srcFile.length();
            exec = sb.append( "C0" )
                    .append( Utils.convertToNumericalForm( fileMode ) )
                    .append( " " )
                    .append( filesize )
                    .append( " " )
                    .append( srcFile.getName() )
                    .append( "\n" )
                    .toString();

            out.write( exec.getBytes() );
            out.flush();

            if( ( message = Utils.checkAck( in ) ) != null ) {
                response.addErrorMessage( message );
                return;
            }

            // send the content of source file
            fis = new FileInputStream( command.getSourceFilePath() );
            byte[] buf = new byte[ 1024 ];
            while( true ) {
                int len = fis.read( buf, 0, buf.length );
                if( len <= 0 ) {
                    break;
                }
                out.write( buf, 0, len );
            }

            // send '\0'
            buf[ 0 ] = 0;
            out.write( buf, 0, 1 );
            out.flush();

            if( ( message = Utils.checkAck( in ) ) != null ) {
                response.addErrorMessage( message );
            }
        }
        catch ( Exception e ) {
            message = "Error while sending file to " + value.getPublicIpAddress();
            LOG.warn( message, e );
            response.addErrorMessage( message );
        }
        finally {
            try {
                if ( in != null ) {
                    in.close();
                }
            }
            catch ( Exception e ) { }
            try {
                if ( out != null ) {
                    out.close();
                }
            }
            catch ( Exception e ) { }
            try {
                if ( fis != null ) {
                    fis.close();
                }
            }
            catch ( Exception e ) { }
            try {
                if ( channel != null ) {
                    channel.disconnect();
                }
            }
            catch ( Exception e ) { }
        }
    }


    protected boolean waitActive( int timeout ) {
        LOG.info( "Waiting maximum {} msecs for SSH port of {} to get active", timeout, value.getPublicIpAddress() );
        long startTime = System.currentTimeMillis();

        while ( System.currentTimeMillis() - startTime < timeout ) {
            Socket s = null;
            try {
                s = new Socket();
                s.setReuseAddress( true );
                SocketAddress sa = new InetSocketAddress( value.getPublicIpAddress(), 22 );
                s.connect( sa, 2000 );
                LOG.info( "Port 22 of {} got opened", value.getPublicIpAddress() );
                return true;
            }
            catch ( Exception e ) {
                try {
                    Thread.sleep( 1000 );
                }
                catch ( InterruptedException ee ) {
                }
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


    /**
     * Concatenates each block of SSH commands between SCP commands.
     * <p>
     * Contiguous SSH commands are converted to a single SSH command, separated by semicolons.
     * That way, more commands can be executed using a single <code>Channel</code> connection.
     *
     * @return  the compressed collection of <code>Command</code>s.
     */
    public static Collection<Command> compressCommands( Collection<Command> commands ) {
        // TODO to be implemented
        throw new NotImplementedException();
    }
}
