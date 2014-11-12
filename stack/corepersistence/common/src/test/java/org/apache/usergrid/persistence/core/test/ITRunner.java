/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.core.test;


import java.lang.reflect.InvocationTargetException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;


/**
 * Run guice and inject it into our tests
 */
public class ITRunner extends BlockJUnit4ClassRunner {



    private Injector injector;




    public ITRunner( final Class<?> klass )
            throws InitializationError, InvocationTargetException, InstantiationException, IllegalAccessException {
        super( klass );
    }


    protected Statement methodInvoker(FrameworkMethod method, Object test)
    {

        inject(test);
        return super.methodInvoker( method, test );
    }


    private synchronized void inject(Object test){
       if(injector == null){
           injector = createInjector( test.getClass() );
       }

        injector.injectMembers( test );
    }

    private Injector createInjector(Class<?> testClass){
        UseModules useModules = testClass.getAnnotation( UseModules.class );

        if ( useModules == null ) {
            throw new RuntimeException(
                    String.format( "You must specify modules to including using the %s annotation", useModules ) );
        }

        final Class<? extends Module>[] moduleClasses = useModules.value();

        final AbstractModule testModule = new AbstractModule() {


            @Override
            protected void configure() {
                for ( Class<? extends Module> moduleClass : moduleClasses ) {
                    final Module module;

                    try {
                        module = moduleClass.newInstance();
                    }
                    catch ( Exception e ) {
                        throw new RuntimeException( "Unable to create instance of module", e );
                    }

                    install( module );
                }
            }
        };


        return Guice.createInjector( testModule );
    }
}
