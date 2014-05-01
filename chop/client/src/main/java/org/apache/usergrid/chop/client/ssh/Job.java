package org.apache.usergrid.chop.client.ssh;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class Job implements Callable<ResponseInfo> {

    private static final Logger LOG = LoggerFactory.getLogger( Job.class );

    private Collection<Command> commands;
    private SshValues value;
    private Session session = null;


    public Job( Collection<Command> commands, SshValues value ) {
        this.commands = commands;
        this.value = value;
        setSession();
    }


    private void setSession() {
        JSch ssh;

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
        }
    }


    @Override
    public ResponseInfo call() throws Exception {
        ResponseInfo response = new ResponseInfo( value.getPublicIpAddress() );
        String message;
        if( session == null ) {
            message = "Could not open ssh session for " + value.getPublicIpAddress();
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
            BufferedReader reader = new BufferedReader( new InputStreamReader( channel.getInputStream() ) );

            while ( ( message = reader.readLine() ) != null ) {
                response.addMessage( message );
            }
            reader.close();

            reader = new BufferedReader( new InputStreamReader( ( ( ChannelExec ) channel ).getErrStream() ) );

            while ( ( message = reader.readLine() ) != null ) {
                response.addErrorMessage( message );
            }
            reader.close();
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
