package org.apache.usergrid.chop.webapp.coordinator;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.api.Constants;
import org.apache.usergrid.chop.api.SshValues;
import org.apache.usergrid.chop.api.store.amazon.InstanceValues;
import org.apache.usergrid.chop.client.ssh.AsyncSsh;
import org.apache.usergrid.chop.client.ssh.Command;
import org.apache.usergrid.chop.client.ssh.ResponseInfo;
import org.apache.usergrid.chop.client.ssh.SCPCommand;
import org.apache.usergrid.chop.client.ssh.SSHCommand;
import org.apache.usergrid.chop.client.ssh.Utils;
import org.apache.usergrid.chop.stack.BasicStack;
import org.apache.usergrid.chop.stack.CoordinatedStack;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;
import org.apache.usergrid.chop.stack.Instance;
import org.apache.usergrid.chop.stack.Stack;

import com.fasterxml.jackson.databind.ObjectMapper;


public class CoordinatorUtils {

    private static final Logger LOG = LoggerFactory.getLogger( CoordinatorUtils.class );


    /**
     * Writes the given input stream to the given file location
     *
     * @param in
     * @param fileLocation
     */
    public static void writeToFile( InputStream in, String fileLocation ) {
        OutputStream out = null;
        try {
            int read;
            byte[] bytes = new byte[ 1024 ];

            out = new FileOutputStream( fileLocation );

            while ( ( read = in.read( bytes ) ) != -1 ) {
                out.write( bytes, 0, read );
            }
            in.close();
            out.flush();
        }
        catch ( IOException e ) {
            LOG.error( "Failed to write out file: " + fileLocation, e );
        }
        finally {
            if ( out != null ) {
                try {
                    out.close();
                }
                catch ( IOException e ) {
                    LOG.error( "Failed while trying to close output stream for {}", fileLocation );
                }
            }
        }
    }


    public static InputStream getResourceAsStreamFromRunnerJar( File runnerJar, String resource ) {
        try {
            // Access the jar file resources after adding it to a new ClassLoader
            URLClassLoader classLoader = new URLClassLoader( new URL[] { runnerJar.toURL() },
                    Thread.currentThread().getContextClassLoader() );

            return classLoader.getResourceAsStream( resource );
        }
        catch ( Exception e ) {
            LOG.warn( "Error while reading {} from runner.jar resources", resource, e );
            return null;
        }
    }


    public static Stack getStackFromRunnerJar( File runnerJar ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream stream = getResourceAsStreamFromRunnerJar( runnerJar, Constants.STACK_JSON );

            return mapper.readValue( stream, BasicStack.class );
        }
        catch ( Exception e ) {
            LOG.warn( "Error while reading stack.json from runner.jar resources", e );
            return null;
        }
    }


    /**
     * File storage scheme:
     *
     * ${base_for_files}/${user}/${groupId}/${artifactId}/${version}/${commitId}/runner.jar
     *
     * @param baseDir   base directory that contains all runner.jar files in above structure
     * @param stack     CoordinatedStack object that is related to the wanted runner.jar
     * @return          runner.jar related to the given stack
     */
    public static File getRunnerJar( String baseDir, CoordinatedStack stack ) {
        File runnerJar = new File( baseDir );
        runnerJar = new File( runnerJar, stack.getUser().getUsername() );
        runnerJar = new File( runnerJar, stack.getModule().getGroupId() );
        runnerJar = new File( runnerJar, stack.getModule().getArtifactId() );
        runnerJar = new File( runnerJar, stack.getModule().getVersion() );
        runnerJar = new File( runnerJar, stack.getCommit().getId() );
        runnerJar = new File( runnerJar, Constants.RUNNER_JAR );

        return runnerJar;
    }


    /**
     * File storage scheme:
     *
     * ${base_for_files}/${user}/${groupId}/${artifactId}/${version}/${commitId}/runner.jar
     *
     * @param baseDir   base directory that contains all runner.jar files in above structure
     * @param user
     * @param groupId
     * @param artifactId
     * @param version
     * @param commitId
     * @return          runner.jar related to the given parameters
     */
    public static File getRunnerJar( String baseDir, String user, String groupId, String artifactId, String version, String commitId ) {
        File runnerJar = new File( baseDir );
        runnerJar = new File( runnerJar, user );
        runnerJar = new File( runnerJar, groupId );
        runnerJar = new File( runnerJar, artifactId );
        runnerJar = new File( runnerJar, version );
        runnerJar = new File( runnerJar, commitId );
        runnerJar = new File( runnerJar, Constants.RUNNER_JAR );

        return runnerJar;
    }


    /**
     * Extracts all scripts from given runner.jar, uploads them to the instances, and executes them in parallel
     *
     * @param cluster   Cluster object that the scripts will be executed on
     * @param runnerJar runner.jar file's path that contains all script files
     * @param keyFile   SSH key file path to be used on ssh operations to instances
     * @return          true if operation fully succeeds
     */
    public static boolean executeSSHCommands( ICoordinatedCluster cluster, File runnerJar, String keyFile ) {
        Collection<SshValues> sshValues = new HashSet<SshValues>( cluster.getSize() );
        StringBuilder sb = new StringBuilder();
        Collection<Command> commands = new ArrayList<Command>();

        LOG.info( "Starting the execution of setup scripts on cluster {}", cluster.getName() );

        // Prepare instance values
        for( Instance instance: cluster.getInstances() ) {
            sshValues.add( new InstanceValues( instance, keyFile ) );
        }

        // Prepare setup environment variables
        for( Object obj: cluster.getInstanceSpec().getScriptEnvironment().keySet() ) {

            String envVar = obj.toString();
            String value = cluster.getInstanceSpec().getScriptEnvironment().getProperty( envVar );

            sb.append( "export " )
              .append( envVar )
              .append( "=\"" )
              .append( value )
              .append( "\";" );
        }
        String exportVars = sb.toString();

        // Prepare SSH and SCP commands
        for( URL scriptFile: cluster.getInstanceSpec().getSetupScripts() ) {
            /** First save file beside runner.jar */
            File file = new File( scriptFile.getPath() );
            File fileToSave = new File( runnerJar.getParentFile(), file.getName() );
            writeToFile( getResourceAsStreamFromRunnerJar( runnerJar, file.getName() ), fileToSave.getPath() );

                /** SCP the script to instance **/
                sb = new StringBuilder();
                sb.append( "/home/" )
                  .append( Utils.DEFAULT_USER )
                  .append( "/" )
                  .append( fileToSave.getName() );

                String destFile = sb.toString();
                commands.add( new SCPCommand( fileToSave.getAbsolutePath(), destFile ) );

                /** calling chmod first just in case **/
                sb = new StringBuilder();
                sb.append( "chmod 0755 " )
                  .append( "/home/" )
                  .append( Utils.DEFAULT_USER )
                  .append( "/" )
                  .append( fileToSave.getName() )
                  .append( ";" );

                /** Run the script command */
                sb.append( exportVars )
                  .append( "sudo -E " )
                  .append( destFile );

                commands.add( new SSHCommand( sb.toString() ) );
        }

        // Execute commands
        Collection<ResponseInfo> responses;
        try {
            AsyncSsh asyncSsh = new AsyncSsh( sshValues, commands );
            responses = asyncSsh.executeAll();
        }
        catch ( InterruptedException e ) {
            LOG.error( "Interrupted while trying to execute SSH command", e );
            return false;
        }
        catch ( ExecutionException e ) {
            LOG.error( "Error while executing ssh commands", e );
            return false;
        }

        for( ResponseInfo response: responses ) {
            if( ! response.isSuccessful() ) {
                return false;
            }
        }
        return true;
    }
}
