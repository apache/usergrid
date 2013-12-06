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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.perftest.settings.PropSettings;

import com.google.inject.*;


/**
 * Dynamically loads the Guice Module responsible for creating the Perftest.
 */
@Singleton
public class TestModuleLoader {
    private static final Logger LOG = LoggerFactory.getLogger( TestModuleLoader.class );
    private Injector childInjector;
    private String testModuleFqcn;
    private Module testModule;


    @Inject
    public TestModuleLoader( Injector injector ) throws Exception
    {
        testModuleFqcn = PropSettings.getTestModuleFqcn();

        if ( testModuleFqcn.equals( NoopPerftestModule.class.getCanonicalName() ) ) {
            testModule = new NoopPerftestModule();
        }
        else {
            testModule = loadTestModule();
        }

        childInjector = injector.createChildInjector( testModule );

        // testModuleFqcn.addCallback( this ); ==> this was for dynamically changing the test
        // maybe we can use this later but for now we turned this off
    }


    public Module loadTestModule() throws Exception {
        // This is a crappy mechanism now - we need to use OSGi for this
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try {
            Class clazz = cl.loadClass( testModuleFqcn );
            return ( Module ) clazz.newInstance();
        }
        catch ( ClassNotFoundException e ) {
            LOG.error( "Could not find class {}", testModuleFqcn, e );
            throw e;
        }
        catch ( InstantiationException e ) {
            LOG.error( "Could not instantiate class {}", testModuleFqcn, e );
            throw e;
        }
        catch ( IllegalAccessException e ) {
            LOG.error( "Access during instantiation of class {}", testModuleFqcn, e );
            throw e;
        }
    }


    public Module getTestModule()
    {
        return testModule;
    }


    public Injector getChildInjector()
    {
        return childInjector;
    }
}
