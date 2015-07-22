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
package org.apache.usergrid.tools;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceParameter;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.ServiceRequest;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.utils.HttpUtils;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.persistence.index.exceptions.QueryParseException;


public class Cli extends ToolBase {

    public static final int MAX_ENTITY_FETCH = 100;

    private static final Logger logger = LoggerFactory.getLogger( Cli.class );

    JsonFactory jsonFactory = new JsonFactory();


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().withDescription( "Cassandra host" ).create( "host" );

        Option remoteOption = OptionBuilder.withDescription( "Use remote Cassandra instance" ).create( "remote" );

        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( remoteOption );

        return options;
    }


    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();
        handleInput();
    }


    public void handleInput() throws QueryParseException {
        BufferedReader d = new BufferedReader( new InputStreamReader( System.in ) );

        UUID applicationId = null;

        while ( true ) {
            System.out.println();
            System.out.print( ">" );
            String s = null;
            try {
                s = d.readLine();
            }
            catch ( IOException e ) {
            }
            if ( s == null ) {
                System.exit( 0 );
            }

            s = s.trim().toLowerCase();
            if ( s.startsWith( "use " ) ) {
                s = s.substring( 4 );
                applicationId = UUIDUtils.tryExtractUUID( s );
                if ( applicationId == null ) {
                    try {
                        applicationId = emf.lookupApplication( s.trim() ).get();
                    }
                    catch ( Exception e ) {
                    }
                }
                if ( applicationId == null ) {
                    System.out.print("Cannot find application: " + s.trim() );
                }
                System.out.println( "Using application " + applicationId );
                continue;
            }

            ServiceAction action = ServiceAction.GET;
            if ( s.startsWith( "get " ) ) {
                s = s.substring( "get ".length() ).trim();
            }
            else if ( s.startsWith( "post " ) ) {
                s = s.substring( "post ".length() ).trim();
                action = ServiceAction.POST;
            }
            else if ( s.startsWith( "put " ) ) {
                s = s.substring( "put".length() ).trim();
                action = ServiceAction.PUT;
            }
            else if ( s.startsWith( "delete " ) ) {
                s = s.substring( "delete ".length() ).trim();
                action = ServiceAction.DELETE;
            }
            else if ( s.startsWith( "quit" ) ) {
                return;
            }

            List<ServiceParameter> parameters = new ArrayList<ServiceParameter>();
            int i = 0;
            boolean next_is_query = false;
            boolean next_is_payload = false;
            boolean next_is_json = false;
            ServicePayload payload = null;
            while ( i < s.length() ) {
                boolean is_query = next_is_query;
                if ( next_is_payload ) {
                    String str = s.substring( i );
                    payload = ServicePayload.stringPayload( str );
                    break;
                }
                else if ( next_is_json ) {
                    next_is_json = false;
                    int start = i - 1;
                    int bracket_count = 1;
                    while ( i < s.length() ) {
                        char c = s.charAt( i );
                        if ( c == '{' ) {
                            bracket_count++;
                        }
                        else if ( c == '}' ) {
                            bracket_count--;
                        }
                        if ( bracket_count == 0 ) {
                            i++;
                            String json = s.substring( start, i );
                            Query query = Query.fromJsonString( json );
                            ServiceParameter.addParameter( parameters, query );
                            if ( ( i < s.length() ) && ( s.charAt( i ) == '/' ) ) {
                                i++;
                            }
                            break;
                        }
                        i++;
                    }
                    continue;
                }

                next_is_query = false;
                next_is_payload = false;
                next_is_json = false;
                int slash = s.indexOf( '/', i );
                int semicolon = s.indexOf( ';', i );
                int question = s.indexOf( '?', i );
                int space = s.indexOf( ' ', i );
                int bracket = s.indexOf( '{', i );
                int j = s.length();

                if ( ( slash >= 0 ) && ( slash < j ) ) {
                    j = slash;
                }

                if ( ( space >= 0 ) && ( space < j ) ) {
                    j = space;
                    next_is_payload = true;
                }

                if ( ( semicolon >= 0 ) && ( semicolon < j ) ) {
                    next_is_query = true;
                    next_is_payload = false;
                    j = semicolon;
                }

                if ( ( question >= 0 ) && ( question < j ) ) {
                    next_is_query = true;
                    next_is_payload = false;
                    j = question;
                }

                if ( ( bracket >= 0 ) && ( bracket < j ) ) {
                    next_is_query = false;
                    next_is_payload = false;
                    next_is_json = true;
                    j = bracket;
                }

                String segment = s.substring( i, j );
                if ( segment.length() > 0 ) {
                    if ( is_query ) {
                        Map<String, List<String>> params = HttpUtils.parseQueryString( segment );
                        Query query = Query.fromQueryParams( params );
                        ServiceParameter.addParameter( parameters, query );
                    }
                    else {
                        UUID uuid = UUIDUtils.tryGetUUID( segment );
                        if ( uuid != null ) {
                            ServiceParameter.addParameter( parameters, uuid );
                        }
                        else {
                            ServiceParameter.addParameter( parameters, segment );
                        }
                    }
                }

                i = j + 1;
            }

            if ( parameters.size() == 0 ) {
                continue;
            }

            System.out.println( action + " " + parameters + " " + payload );
            ServiceManager services = smf.getServiceManager( applicationId );
            ServiceRequest r = null;
            try {
                r = services.newRequest( action, parameters, payload );
            }
            catch ( Exception e ) {
                logger.error( "Error", e );
            }
            ServiceResults results = null;
            try {
                results = r.execute();
            }
            catch ( Exception e ) {
                logger.error( "Error", e );
            }
            if ( results != null ) {
                if ( results.hasData() ) {
                    System.out.println( JsonUtils.mapToFormattedJsonString( results.getData() ) );
                }
                if ( results.getServiceMetadata() != null ) {
                    System.out.println( JsonUtils.mapToFormattedJsonString( results.getServiceMetadata() ) );
                }
                System.out.println( JsonUtils.mapToFormattedJsonString( results.getEntities() ) );
            }
        }
    }
}
