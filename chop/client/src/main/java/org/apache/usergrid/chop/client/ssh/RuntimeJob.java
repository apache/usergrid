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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.api.SshValues;


/**
 * Works similar to <code>Job</code> and satisfies the same functionality.
 * However, this uses runtime execution, so might be platform dependant.
 */
public class RuntimeJob extends Job {

    private static final Logger LOG = LoggerFactory.getLogger( RuntimeJob.class );


    public RuntimeJob( Collection<Command> commands, SshValues value ) {
        this.commands = commands;
        this.value = value;
    }


    @Override
    public ResponseInfo call() throws Exception {
        ResponseInfo response = new ResponseInfo( value.getPublicIpAddress() );
        boolean success = waitActive( SESSION_CONNECT_TIMEOUT );
        if( ! success ) {
            LOG.warn( "Port 22 of {} did not open in time", value.getPublicIpAddress() );
        }

        for( Command command: commands ) {
            String cmdString = getCommand( command );
            LOG.info( "Executing {} on {}", cmdString, value.getPublicIpAddress() );
            execute( cmdString, response );
        }
        return response;
    }


    private void execute( String command, ResponseInfo response ) {
        String message;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec( command );
            process.getOutputStream();

            BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
            while ( ( message = reader.readLine() ) != null ) {
                response.addMessage( message );
                LOG.info( "Message: {} at: {}", message, value.getPublicIpAddress() );
            }
            reader.close();

            reader = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
            while ( ( message = reader.readLine() ) != null ) {
                // TODO warnings are also in the stderr, so for now error messages are only for exceptions
                response.addMessage( message );
                LOG.info( "ErrMessage: {} at: {}", message, value.getPublicIpAddress() );
            }
            reader.close();

        }
        catch ( IOException e ) {
            message = "Error while sending ssh command to " + value.getPublicIpAddress();
            LOG.warn( message, e );
            response.addErrorMessage( message );
        }
        finally {
            if( process != null ) {
                process.destroy();
            }
        }
    }


    private String getCommand( Command command ) {
        StringBuilder execution = new StringBuilder();
        if( command instanceof SSHCommand ) {
            execution.append( "/usr/bin/ssh -i " )
                     .append( value.getSshKeyFile() )
                     .append( " -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null " )
                     .append( Utils.DEFAULT_USER )
                     .append( "@" )
                     .append( value.getPublicIpAddress() )
                     .append( " " )
                     .append( ( ( SSHCommand ) command ).getCommand() );
        }
        else if( command instanceof SCPCommand ) {
            execution.append( "/usr/bin/scp -i " )
                     .append( value.getSshKeyFile() )
                     .append( " -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null " )
                     .append( ( ( SCPCommand ) command ).getSourceFilePath() )
                     .append( " " )
                     .append( Utils.DEFAULT_USER )
                     .append( "@" )
                     .append( value.getPublicIpAddress() )
                     .append( ":" )
                     .append( ( ( SCPCommand ) command ).getDestinationFilePath() );
        }
        return execution.toString();
    }
}
