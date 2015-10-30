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
package org.apache.usergrid.rest.filters;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;


/**
 * Filter for setting default accept and Content-Type as application/json when undefined by client
 *
 * @author tnine
 */
public class ContentTypeFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger( ContentTypeFilter.class );


    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init( FilterConfig filterConfig ) throws ServletException {
        logger.info( "Starting content type filter" );
    }


    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
            throws IOException, ServletException {

        if ( !( request instanceof HttpServletRequest ) ) {
            chain.doFilter( request, response );
            return;
        }

        HttpServletRequest origRequest = ( HttpServletRequest ) request;

        HeaderWrapperRequest newRequest = new HeaderWrapperRequest( origRequest );
        newRequest.adapt();

        try {
            chain.doFilter( newRequest, response );
        } catch ( Exception e ) {
            logger.debug("Error in filter", e);
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }


    private class HeaderWrapperRequest extends HttpServletRequestWrapper {
        private PushbackInputStream inputStream = null;
        private ServletInputStream servletInputStream = null;
        private HttpServletRequest origRequest = null;
        private BufferedReader reader = null;

        private final Map<String, String> newHeaders = new HashMap<String, String>();


        /**
         * @param request
         * @throws IOException
         */
        public HeaderWrapperRequest( HttpServletRequest request ) throws IOException {
            super( request );
            origRequest = request;
            inputStream = new PushbackInputStream( request.getInputStream() );
            servletInputStream = new DelegatingServletInputStream( inputStream );
        }


        /**
         * @throws IOException
         *
         */
        private void adapt() throws IOException {

            String path = origRequest.getRequestURI();
            String method = origRequest.getMethod();
            logger.debug( "Content path is '{}'", path );


            // first ensure that an Accept header is set

            @SuppressWarnings( "rawtypes" ) Enumeration acceptHeaders = origRequest.getHeaders( HttpHeaders.ACCEPT );
            if ( !acceptHeaders.hasMoreElements() ) {
                setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
            }

            // next, ensure that one and only one content-type is set

            int initial = inputStream.read();
            if ( initial == -1 ) {

                // request has no body, set type to application/json
                if ( ( HttpMethod.POST.equals( method ) || HttpMethod.PUT.equals( method ) )
                    && !MediaType.APPLICATION_FORM_URLENCODED.equals( getContentType() ) ) {
                    logger.debug("Setting content type to application/json " +
                            "for POST or PUT with no content at path '{}'", path );

                    setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
                    setHeader( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON );
                }

                return;
            }

            char firstChar = ( char ) initial;
            if ( ( firstChar == '{' || firstChar == '[' )
                 && !MediaType.APPLICATION_JSON.equals( getContentType() )) {

                // request appears to be JSON so set type to application/json
                logger.debug( "Setting content type to application/json " +
                        "for POST or PUT with json content at path '{}'", path );
                setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
                setHeader( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON );
            }

            inputStream.unread( initial );
        }


        public void setHeader( String name, String value ) {
            newHeaders.put( name.toLowerCase(), value );
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * javax.servlet.http.HttpServletRequestWrapper#getHeader(java.lang.
         * String)
         */
        @Override
        public String getHeader( String name ) {
            String header = newHeaders.get( name );

            if ( header != null ) {
                return header;
            }

            return super.getHeader( name );
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * javax.servlet.http.HttpServletRequestWrapper#getHeaders(java.lang
         * .String)
         */
        @Override
        public Enumeration getHeaders( String name ) {

            Set<String> headers = new LinkedHashSet<String>();
            String overridden = newHeaders.get( name );

            if ( overridden != null ) {
                headers.add( overridden );
                return Collections.enumeration( headers );
            }

            return super.getHeaders( name );
        }


        /*
         * (non-Javadoc)
         *
         * @see javax.servlet.http.HttpServletRequestWrapper#getHeaderNames()
         */
        @Override
        public Enumeration getHeaderNames() {
            Set<String> headers = new LinkedHashSet<String>();

            for ( Enumeration e = super.getHeaderNames(); e.hasMoreElements(); ) {
                headers.add( e.nextElement().toString() );
            }

            headers.addAll( newHeaders.keySet() );

            return Collections.enumeration( headers );
        }


        /*
         * (non-Javadoc)
         *
         * @see javax.servlet.ServletRequestWrapper#getInputStream()
         */
        @Override
        public ServletInputStream getInputStream() throws IOException {
            return servletInputStream;
        }


        /*
         * (non-Javadoc)
         *
         * @see javax.servlet.ServletRequestWrapper#getReader()
         */
        @Override
        public BufferedReader getReader() throws IOException {
            if ( reader != null ) {
                return reader;
            }

            reader = new BufferedReader( new InputStreamReader( servletInputStream ) );

            return reader;
        }

        // NOTE, for full override we need to implement the other getHeader
        // methods. We won't use it, so I'm not implementing it here
    }


    /**
     * Delegating implementation of {@link javax.servlet.ServletInputStream}.
     *
     * @author Juergen Hoeller, Todd Nine
     * @since 1.0.2
     */
    private static class DelegatingServletInputStream extends ServletInputStream {

        private final InputStream sourceStream;


        /**
         * Create a DelegatingServletInputStream for the given source stream.
         *
         * @param sourceStream the source stream (never <code>null</code>)
         */
        public DelegatingServletInputStream( InputStream sourceStream ) {
            Assert.notNull( sourceStream, "Source InputStream must not be null" );
            this.sourceStream = sourceStream;
        }


        /** Return the underlying source stream (never <code>null</code>). */
        public final InputStream getSourceStream() {
            return this.sourceStream;
        }


        public int read() throws IOException {
            return this.sourceStream.read();
        }


        public void close() throws IOException {
            super.close();
            this.sourceStream.close();
        }
    }
}
