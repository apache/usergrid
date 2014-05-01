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
package org.apache.usergrid.launcher;


@javax.ws.rs.ext.Provider
public class CustomJSPTemplateProcessor implements com.sun.jersey.spi.template.ViewProcessor<String> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger( CustomJSPTemplateProcessor.class );

    private
    @javax.ws.rs.core.Context
    com.sun.jersey.api.core.HttpContext hc;

    private
    @javax.ws.rs.core.Context
    javax.servlet.ServletContext servletContext;

    private
    @javax.ws.rs.core.Context
    ThreadLocal<javax.servlet.http.HttpServletRequest> requestInvoker;

    private
    @javax.ws.rs.core.Context
    ThreadLocal<javax.servlet.http.HttpServletResponse> responseInvoker;

    private final String basePath;


    public CustomJSPTemplateProcessor( @javax.ws.rs.core.Context com.sun.jersey.api.core.ResourceConfig resourceConfig ) {
        logger.info( "CustomJSPTemplateProcessor installed" );

        String path = ( String ) resourceConfig.getProperties().get( com.sun.jersey.spi.container.servlet.ServletContainer.JSP_TEMPLATES_BASE_PATH );
        if ( path == null ) {
            basePath = "";
        }
        else if ( path.charAt( 0 ) == '/' ) {
            basePath = path;
        }
        else {
            basePath = "/" + path;
        }
    }


    public String findJsp( String path ) throws java.net.MalformedURLException {
        if ( servletContext.getResource( path ) != null ) {
            return path;
        }
        else {
            // check if the entry exists in web.xml through the
            // RequestDispatcher
            javax.servlet.ServletContext jspContext = servletContext.getContext( path );
            if ( jspContext != null ) {
                javax.servlet.RequestDispatcher jspReqDispatcher = servletContext.getRequestDispatcher( path );
                if ( jspReqDispatcher != null ) {
                    return path;
                }
            }
            javax.servlet.RequestDispatcher reqDispatcher = servletContext.getRequestDispatcher( path );
            if ( reqDispatcher != null ) {
                return path;
            }
        }
        return null;
    }


    @Override
    public String resolve( String path ) {
        if ( servletContext == null ) {
            return null;
        }

        if (!basePath.equals("")) {
            path = basePath + path;
        }

        try {
            if ( findJsp( path ) != null ) {
                return path;
            }

            if ( !path.endsWith( ".jsp" ) ) {
                path = path + ".jsp";
                if ( findJsp( path ) != null ) {
                    return path;
                }
            }
        }
        catch ( java.net.MalformedURLException ex ) {
            // TODO log
        }

        return null;
    }


    @Override
    public void writeTo( String resolvedPath, com.sun.jersey.api.view.Viewable viewable, java.io.OutputStream out ) throws java.io.IOException {
        if ( hc.isTracingEnabled() ) {
            hc.trace( String.format( "forwarding view to JSP page: \"%s\", it = %s", resolvedPath,
                    com.sun.jersey.core.reflection.ReflectionHelper.objectToString( viewable.getModel() ) ) );
        }

        // Commit the status and headers to the HttpServletResponse
        out.flush();

        javax.servlet.RequestDispatcher d = servletContext.getRequestDispatcher( resolvedPath );
        if ( d == null ) {
            throw new com.sun.jersey.api.container.ContainerException( "No request dispatcher for: " + resolvedPath );
        }

        d = new com.sun.jersey.server.impl.container.servlet.RequestDispatcherWrapper( d, basePath, hc, viewable );

        try {
            d.forward( requestInvoker.get(), responseInvoker.get() );
        }
        catch ( Exception e ) {
            throw new com.sun.jersey.api.container.ContainerException( e );
        }
    }
}
