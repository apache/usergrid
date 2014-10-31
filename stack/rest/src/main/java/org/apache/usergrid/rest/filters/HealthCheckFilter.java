/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 * Fail fast if connection to database or query index not healthy.
 */
public class HealthCheckFilter implements Filter {

    ServletContext sc;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        if ( sc == null ) {
            sc = fc.getServletContext();
        }
    }

    @Override
    public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) 
            throws IOException, ServletException {


        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
        EntityManagerFactory emf = (EntityManagerFactory)ctx.getBean("entityManagerFactory");

        if ( !emf.verifyCollectionsModuleHealthy() ) {
            throw new RuntimeException("Error connecting to datastore");
        }
        if ( !emf.verifyQueryIndexModuleHealthy() ) {
            throw new RuntimeException("Error connecting to query index");
        }
    }

    @Override
    public void destroy() {
        // no op
    }
    
}
