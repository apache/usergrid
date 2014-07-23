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

import org.apache.usergrid.chop.stack.Cluster;
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
import org.apache.usergrid.chop.stack.ICoordinatedStack;
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


    /**
     *
     * @param runnerJar
     * @param resource
     * @return
     */
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


    /**
     *
     * @param runnerJar
     * @return
     */
    public static Stack getStackFromRunnerJar( File runnerJar ) {
        InputStream stream = null;
        URLClassLoader classLoader = null;
        try {
            // Access the jar file resources after adding it to a new ClassLoader
            classLoader = new URLClassLoader( new URL[] { runnerJar.toURL() },
                    Thread.currentThread().getContextClassLoader() );

            ObjectMapper mapper = new ObjectMapper();
            stream = classLoader.getResourceAsStream( Constants.STACK_JSON );

            BasicStack stack = mapper.readValue( stream, BasicStack.class );
            return stack;
        }
        catch ( Exception e ) {
            LOG.warn( "Error while reading stack.json from runner.jar resources", e );
            return null;
        }
        finally {
            if( stream != null ) {
                try {
                    stream.close();
                }
                catch ( Exception e ) {
                    LOG.debug( "Could not close stack json stream", e );
                }
            }
            if( classLoader != null ) {
                try {
                    classLoader.close();
                }
                catch ( Exception e ) {
                    LOG.debug( "Could not close class loader for loading stack.json", e );
                }
            }
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
    public static boolean executeClusterSSHCommands( ICoordinatedCluster cluster, File runnerJar, String keyFile ) {
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

        /*
         * Export instance IPs and host names as a space separated list
         * with ClusterName suffixed by _HOSTS and _ADDRS
         */
        StringBuilder ipList = new StringBuilder();
        StringBuilder privateIpList = new StringBuilder();
        StringBuilder hostList = new StringBuilder();
        StringBuilder privateHostList = new StringBuilder();
        for ( Instance temp : cluster.getInstances() ) {
            ipList.append( temp.getPublicIpAddress() )
                    .append( " " );
            privateIpList.append( temp.getPrivateIpAddress() )
                    .append( " " );
            hostList.append( temp.getPublicDnsName() )
                    .append( " " );
            privateHostList.append( temp.getPrivateDnsName() )
                    .append( " " );
        }

        sb.append( "export " )
                .append( cluster.getName().toUpperCase() )
                .append( "_ADDRS=\"" )
                .append( ipList.substring( 0, ipList.toString().length() - 1 ) )
                .append( "\";" );

        sb.append( "export " )
                .append( cluster.getName().toUpperCase() )
                .append( "_PRIVATE_ADDRS=\"" )
                .append( privateIpList.substring( 0, privateIpList.toString().length() - 1 ) )
                .append( "\";" );

        sb.append( "export " )
                .append( cluster.getName().toUpperCase() )
                .append( "_HOSTS=\"" )
                .append( hostList.substring( 0, hostList.toString().length() - 1 ) )
                .append( "\";" );

        sb.append( "export " )
                .append( cluster.getName().toUpperCase() )
                .append( "_PRIVATE_HOSTS=\"" )
                .append( privateHostList.substring( 0, privateHostList.toString().length() - 1 ) )
                .append( "\";" );

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

        return executeSSHCommands( sshValues, commands );
    }


    /**
     * Deploys and starts runner.jar on instances
     *
     * @param stack
     * @param runnerJar
     * @param keyFile
     * @return
     */
    public static boolean executeRunnerSSHCommands( ICoordinatedStack stack, File runnerJar, String keyFile ) {
        Collection<SshValues> sshValues = new HashSet<SshValues>( stack.getRunnerCount() );
        StringBuilder sb = new StringBuilder();
        Collection<Command> commands = new ArrayList<Command>();

        LOG.info( "Deploying and starting runner.jar to runner instances of {}", stack.getName() );

        /** Prepare instance values */
        for( Instance instance: stack.getRunnerInstances() ) {
            sshValues.add( new InstanceValues( instance, keyFile ) );
        }

        /** SCP the runner.jar to instance **/
        sb.append( "/home/" )
          .append( Utils.DEFAULT_USER )
          .append( "/" )
          .append( runnerJar.getName() );

        String destFile = sb.toString();
        commands.add( new SCPCommand( runnerJar.getAbsolutePath(), destFile ) );

        sb = new StringBuilder();

        /** Get runner scripts out of the jar file and prepare ssh & scp commands */
        for ( Cluster cluster : stack.getClusters() ){

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
            for( URL scriptFile : cluster.getInstanceSpec().getRunnerScripts() ) {
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

                String destinationFile = sb.toString();
                commands.add( new SCPCommand( fileToSave.getAbsolutePath(), destinationFile ) );

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
                        .append( destinationFile );


                commands.add( new SSHCommand( sb.toString() ) );
            }
        }


        /**
         * Start the runner.jar on instance.
         * This assumes an appropriate java is existing at /usr/bin/java on given instances,
         * so imageId for runners should be selected accordingly.
         */
        sb = new StringBuilder();
        sb.append( "sudo su -c \"nohup /usr/bin/java -Darchaius.deployment.environment=CHOP -jar " )
          .append( destFile )
          .append( " > /var/log/chop-runner.log 2>&1 &\"" );

        commands.add( new SSHCommand( sb.toString() ) );

        return executeSSHCommands( sshValues, commands );
    }


    /**
     *
     * @param sshValues
     * @param commands
     * @return          true if all commands on all instances succeed
     */
    public static boolean executeSSHCommands( Collection<SshValues> sshValues, Collection<Command> commands ) {
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
