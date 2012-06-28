/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.filters;

import java.io.IOException;
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
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * Filter for setting default accept and Content-Type as application/json when
 * undefined by client
 * 
 * @author tnine
 * 
 */
public class ContentTypeFilter implements Filter {

    private static final String WILDCARD = "*/*";

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest origRequest = (HttpServletRequest) request;

        HeaderWrapperRequest newRequest = new HeaderWrapperRequest(origRequest);

        String accept = origRequest.getHeader(HttpHeaders.ACCEPT);
        String contentType = origRequest.getHeader(HttpHeaders.CONTENT_TYPE);

        if (accept == null || accept.contains(WILDCARD)) {
            newRequest
                    .setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        }

        if (contentType == null || contentType.contains(WILDCARD)
                || contentType.contains(MediaType.TEXT_PLAIN)) {
            newRequest.setHeader(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_JSON);
        }

        chain.doFilter(newRequest, response);
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

        private final Map<String, String> newHeaders = new HashMap<String, String>();

        /**
         * @param request
         */
        public HeaderWrapperRequest(HttpServletRequest request) {
            super(request);
        }

        /**
         * Override a header value
         * 
         * @param name
         * @param value
         */
        public void setHeader(String name, String value) {
            newHeaders.put(name, value);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.servlet.http.HttpServletRequestWrapper#getHeader(java.lang.
         * String)
         */
        @Override
        public String getHeader(String name) {
            String header = newHeaders.get(name);

            if (header != null) {
                return header;
            }

            return super.getHeader(name);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.servlet.http.HttpServletRequestWrapper#getHeaders(java.lang
         * .String)
         */
        @Override
        public Enumeration getHeaders(String name) {
            Set<String> headers = new LinkedHashSet<String>();

            String overridden = newHeaders.get(name);

            if (overridden != null) {
                headers.add(overridden);
            } else {
                for (Enumeration e = super.getHeaders(name); e
                        .hasMoreElements();) {
                    headers.add(e.nextElement().toString());
                }
            }

            return Collections.enumeration(headers);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.servlet.http.HttpServletRequestWrapper#getHeaderNames()
         */
        @Override
        public Enumeration getHeaderNames() {
            Set<String> headers = new LinkedHashSet<String>();

            for (Enumeration e = super.getHeaderNames(); e.hasMoreElements();) {
                headers.add(e.nextElement().toString());
            }

            headers.addAll(newHeaders.keySet());

            return Collections.enumeration(headers);
        }

        // NOTE, for full override we need to implement the other getHeader*
        // methods. We won't use it, so I'm not implementing it here

    }

}
