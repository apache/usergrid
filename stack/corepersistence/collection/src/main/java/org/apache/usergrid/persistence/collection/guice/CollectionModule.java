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
package org.apache.usergrid.persistence.collection.guice;


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.cache.EntityCacheFig;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerFactoryImpl;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGenerator;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGeneratorImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;

import com.google.inject.AbstractModule;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public abstract class CollectionModule extends AbstractModule {


    @Override
    protected void configure() {

        // noinspection unchecked
        install( new GuicyFigModule( SerializationFig.class ) );
        install( new SerializationModule() );
        install( new ServiceModule() );

        // users of this module can add their own implemementations
        // create a guice factor for getting our collection manager
         bind(EntityCollectionManagerFactory.class).to(EntityCollectionManagerFactoryImpl.class);

        //bind this to our factory
        install( new GuicyFigModule( EntityCacheFig.class ) );

        bind( ChangeLogGenerator.class).to( ChangeLogGeneratorImpl.class);

        configureMigrationProvider();

    }


    /**
     * Gives callers the ability to to configure an instance of
     *
     * MigrationDataProvider<EntityIdScope> for providing data migrations
     */
    public abstract void configureMigrationProvider();




}


