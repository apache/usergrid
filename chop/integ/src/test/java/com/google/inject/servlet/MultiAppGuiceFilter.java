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
package com.google.inject.servlet;


import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Solves issues due to GuiceFilters of multiple web applications colliding.
 *
 * @see "https://groups.google.com/forum/#!topic/google-guice/wJBwzE5E7Y0"
 */
public class MultiAppGuiceFilter extends GuiceFilter {

    // lock to ensure that all webapps using this filter will not access to the static pipeline concurrently.
    // this lock will only work if all web apps use this filter. This lock is not safe if other app uses GuiceFilter.
    private static final Object lock = new Object();

    // local instance of the pipeline.
    volatile FilterPipeline localPipeline;


    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
            throws IOException, ServletException {
        // this method is a copy of the one in GuiceFilter, but using the local pipeline instead of the static one.
        Context previous = localContext.get();

        try {
            localContext.set( new Context( ( HttpServletRequest ) servletRequest,
                    ( HttpServletResponse ) servletResponse ) );

            //dispatch across the servlet pipeline, ensuring web.xml's filterchain is honored
            localPipeline.dispatch( servletRequest, servletResponse, filterChain );
        }
        finally {
            localContext.set( previous );
        }
    }


    @Override
    public void init( FilterConfig filterConfig ) throws ServletException {
        synchronized ( lock ) {
            //define the localPipeline with the injected pipeline.
            localPipeline = pipeline;
            // Store servlet context in a weakreference, for injection
            servletContext = new WeakReference<ServletContext>( filterConfig.getServletContext() );
            localPipeline.initPipeline( filterConfig.getServletContext() );
            //reset the static pipeline
            pipeline = new DefaultFilterPipeline();
        }
    }


    @Override
    public void destroy() {
        try {
            // destroy the local pipeline instead of the static one.
            localPipeline.destroyPipeline();
        }
        finally {
            reset();
            servletContext.clear();
        }
    }
}