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
package org.apache.usergrid.corepersistence;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.apache.usergrid.corepersistence.events.EntityDeletedHandler;
import org.apache.usergrid.corepersistence.events.EntityVersionDeletedHandler;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.graph.guice.GraphModule;
import org.apache.usergrid.persistence.index.guice.IndexModule;
import org.apache.usergrid.persistence.map.guice.MapModule;
import org.apache.usergrid.persistence.queue.guice.QueueModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Guice Module that encapsulates Core Persistence.
 */
public class GuiceModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(GuiceModule.class);

    @Override
    protected void configure() {

        install(new CommonModule());
        install(new CollectionModule());
        install(new GraphModule());
        install(new IndexModule());
        install(new MapModule());
        install(new QueueModule());

        Multibinder<EntityDeleted> entityBinder
                = Multibinder.newSetBinder(binder(), EntityDeleted.class);
        entityBinder.addBinding().to(EntityDeletedHandler.class);

        Multibinder<EntityVersionDeleted> versionBinder
                = Multibinder.newSetBinder(binder(), EntityVersionDeleted.class);
        versionBinder.addBinding().to(EntityVersionDeletedHandler.class);
    }

}
