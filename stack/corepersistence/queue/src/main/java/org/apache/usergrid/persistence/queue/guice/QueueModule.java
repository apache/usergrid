/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.queue.guice;


import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.queue.MapManager;
import org.apache.usergrid.persistence.queue.MapManagerFactory;
import org.apache.usergrid.persistence.queue.impl.MapManagerImpl;
import org.apache.usergrid.persistence.queue.impl.MapSerialization;
import org.apache.usergrid.persistence.queue.impl.MapSerializationImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class QueueModule extends AbstractModule {


    @Override
    protected void configure() {

        // create a guice factory for getting our collection manager
        install( new FactoryModuleBuilder().implement( MapManager.class, MapManagerImpl.class )
                                           .build( MapManagerFactory.class ) );


        bind( MapSerialization.class).to( MapSerializationImpl.class );

        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
        migrationBinding.addBinding().to(  Key.get( MapSerialization.class ) );

    }



}


