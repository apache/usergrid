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
package org.apache.usergrid.rest.utils;


import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;


/* Note: At one point there was a special case in this code that would
   convert an origin header sent with a string containing the contents
   "null" to an actual null and dealing with that case as if the header
   had not been sent at all. This, however, caused problems with the
   Firefox and Chrome browsers that intend that the "null" be the actual
   origin when the file:// protocol is used and specifically look for it
   in the response. By removing this special case and instead allowing
   the normal processing (ie. allowing "null" to be a valid origin), it
   removed the issue for those browsers. Safari works regardless as it
   actually leaves off the header (a true null). IE still doesn't work
   on file:// regardless for reasons unknown.
*/
public class CORSUtils {

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_REQUEST_METHOD = "access-control-request-method";
    private static final String ACCESS_CONTROL_REQUEST_HEADERS = "access-control-request-headers";
    private static final String ORIGIN_HEADER = "origin";
    private static final String REFERER_HEADER = "referer";


    public static void allowAllOrigins( HttpServletRequest request, HttpServletResponse response ) {

        if ( request.getHeader( ACCESS_CONTROL_REQUEST_METHOD ) != null ) {
            @SuppressWarnings("unchecked") Enumeration<String> e = request.getHeaders( ACCESS_CONTROL_REQUEST_METHOD );
            while ( e.hasMoreElements() ) {
                String value = e.nextElement();
                response.addHeader( ACCESS_CONTROL_ALLOW_METHODS, value );
            }
        }

        if ( request.getHeader( ACCESS_CONTROL_REQUEST_HEADERS ) != null ) {
            @SuppressWarnings("unchecked") Enumeration<String> e = request.getHeaders( ACCESS_CONTROL_REQUEST_HEADERS );
            while ( e.hasMoreElements() ) {
                String value = e.nextElement();
                response.addHeader( ACCESS_CONTROL_ALLOW_HEADERS, value );
            }
        }

        boolean origin_sent = false;
        if ( request.getHeader( ORIGIN_HEADER ) != null ) {
            @SuppressWarnings("unchecked") Enumeration<String> e = request.getHeaders( ORIGIN_HEADER );
            while ( e.hasMoreElements() ) {
                String value = e.nextElement();
                if ( value != null ) {
                    origin_sent = true;
                    response.addHeader( ACCESS_CONTROL_ALLOW_ORIGIN, value );
                }
            }
        }

        if ( !origin_sent ) {
            String origin = getOrigin( request );
            if ( origin != null ) {
                response.addHeader( ACCESS_CONTROL_ALLOW_CREDENTIALS, "true" );
                response.addHeader( ACCESS_CONTROL_ALLOW_ORIGIN, origin );
            }
            else {
                response.addHeader( ACCESS_CONTROL_ALLOW_ORIGIN, "*" );
            }
        }
        else {
            response.addHeader( ACCESS_CONTROL_ALLOW_CREDENTIALS, "true" );
        }
    }


    public static ContainerResponseContext allowAllOrigins( ContainerRequestContext request, ContainerResponseContext response ) {

        if ( request.getHeaders().containsKey( ACCESS_CONTROL_REQUEST_METHOD ) ) {

            for ( String value : request.getHeaders().get( ACCESS_CONTROL_REQUEST_METHOD ) ) {
                response.getHeaders().add( ACCESS_CONTROL_ALLOW_METHODS, value );
            }
        }

        if ( request.getHeaders().containsKey( ACCESS_CONTROL_REQUEST_HEADERS ) ) {
            for ( String value : request.getHeaders().get( ACCESS_CONTROL_REQUEST_HEADERS ) ) {
                response.getHeaders().add( ACCESS_CONTROL_ALLOW_HEADERS, value );
            }
        }

        boolean origin_sent = false;
        if ( request.getHeaders().containsKey( ORIGIN_HEADER ) ) {
            for ( String value : request.getHeaders().get( ORIGIN_HEADER ) ) {
                if ( value != null ) {
                    origin_sent = true;
                    response.getHeaders().add( ACCESS_CONTROL_ALLOW_ORIGIN, value );
                }
            }
        }

        if ( !origin_sent ) {
            String origin = getOrigin( request );
            if ( origin != null ) {
                response.getHeaders().add( ACCESS_CONTROL_ALLOW_CREDENTIALS, "true" );
                response.getHeaders().add( ACCESS_CONTROL_ALLOW_ORIGIN, origin );
            }
            else {
                response.getHeaders().add( ACCESS_CONTROL_ALLOW_ORIGIN, "*" );
            }
        }
        else {
            response.getHeaders().add( ACCESS_CONTROL_ALLOW_CREDENTIALS, "true" );
        }

        return response;
    }


    public static String getOrigin( String origin, String referer ) {
        if ( ( origin != null ) && ( !"null".equalsIgnoreCase( origin ) ) ) {
            return origin;
        }
        if ( ( referer != null ) && ( referer.startsWith( "http" ) ) ) {
            int i = referer.indexOf( "//" );
            if ( i != -1 ) {
                i = referer.indexOf( '/', i + 2 );
                if ( i != -1 ) {
                    return referer.substring( 0, i );
                }
                else {
                    return referer;
                }
            }
        }
        return null;
    }


    public static String getOrigin( HttpServletRequest request ) {
        String origin = request.getHeader( ORIGIN_HEADER );
        String referer = request.getHeader( REFERER_HEADER );
        return getOrigin( origin, referer );
    }


    public static String getOrigin( ContainerRequestContext request ) {
        String origin = request.getHeaders().getFirst( ORIGIN_HEADER );
        String referer = request.getHeaders().getFirst( REFERER_HEADER );
        return getOrigin( origin, referer );
    }
}
