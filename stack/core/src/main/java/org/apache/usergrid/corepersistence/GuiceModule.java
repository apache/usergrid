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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.core.consistency.LocalTimeoutQueueFactory;
import org.apache.usergrid.persistence.core.consistency.TimeoutQueueFactory;
import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.graph.guice.GraphModule;
import org.apache.usergrid.persistence.index.guice.IndexModule;

import com.google.inject.AbstractModule;


/**
 * Guice Module that encapsulates Core Persistence.
 */
public class GuiceModule  extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger( GuiceModule.class );

    @Override
    protected void configure() {

        install(new CommonModule() {
            @Override
            protected void bindTimeoutQueueFactory() {
                bind(TimeoutQueueFactory.class).to( LocalTimeoutQueueFactory.class );
            }
        });
        install(new CollectionModule());
        install(new GraphModule());
        install(new IndexModule());

    }   

}
