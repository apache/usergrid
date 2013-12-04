/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.perftest;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.netflix.blitz4j.LoggingConfiguration;
import org.apache.usergrid.perftest.amazon.AmazonS3Module;
import org.apache.usergrid.perftest.amazon.AmazonS3Service;

import javax.servlet.ServletContextEvent;

/**
 * ...
 */
public class PerftestServletConfig extends GuiceServletContextListener {
    private Injector injector;
    private AmazonS3Service s3Service;


    @Override
    protected Injector getInjector() {
        if ( injector != null )
        {
            return injector;
        }

        injector = Guice.createInjector( new PerftestModule(), new AmazonS3Module() );
        return injector;
    }


    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {
        super.contextInitialized( servletContextEvent );
        LoggingConfiguration.getInstance().configure();
        s3Service = getInjector().getInstance( AmazonS3Service.class );
        s3Service.setServletContext( servletContextEvent.getServletContext() );
        s3Service.start();
    }

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {
        LoggingConfiguration.getInstance().stop();

        if ( s3Service != null )
        {
            s3Service.stop();
        }
        super.contextDestroyed( servletContextEvent );
    }
}
