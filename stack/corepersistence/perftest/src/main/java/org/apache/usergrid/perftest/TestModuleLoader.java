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


import com.google.inject.*;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;


/**
 * Dynamically loads the Guice Module responsible for creating the Perftest.
 */
@Singleton
public class TestModuleLoader implements Runnable {
    public static final String MOCK_TEST_MODULE = "org.apache.usergrid.perftest.NoopPerftestModule";

    private final Injector injector;
    private Injector childInjector;
    private DynamicStringProperty testModuleFqcn;
    private Module testModule;


    @Inject
    public TestModuleLoader( Injector injector )
    {
        this.injector = injector;
        testModuleFqcn = DynamicPropertyFactory.getInstance().getStringProperty( "test.module.fqcn", MOCK_TEST_MODULE );

        if ( testModuleFqcn.get().equals( MOCK_TEST_MODULE ) ) {
            testModule = new NoopPerftestModule();
        }
        else {
            testModule = loadTestModule();
        }

        childInjector = injector.createChildInjector( testModule );
        testModuleFqcn.addCallback( this );
    }


    public Module loadTestModule() {
        // This is a crappy mechanism now - we need to use OSGi for this
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try {
            Class clazz = cl.loadClass( testModuleFqcn.get() );
            return ( Module ) clazz.newInstance();
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch ( InstantiationException e ) {
            e.printStackTrace();
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        }

        return null;
    }


    public Module getTestModule()
    {
        return testModule;
    }


    public Injector getChildInjector()
    {
        return childInjector;
    }


    @Override
    public void run() {
        testModule = loadTestModule();
        childInjector = injector.createChildInjector( testModule );
    }
}
