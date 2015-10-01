/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.corepersistence;


import org.springframework.context.ApplicationContext;

import org.apache.usergrid.persistence.PersistenceModule;
import org.apache.usergrid.persistence.core.guice.TestModule;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;


public class TestIndexModule extends TestModule {


    @Override
    protected void configure() {


        //TODO, refactor to guice to get rid of this
        final ApplicationContext singleton = ConcurrentProcessSingleton.getInstance().getSpringResource().getAppContext();

        //this will break, we need to untagle this and move to guice in core completely
        install( new CoreModule() );
        install( new PersistenceModule( singleton ) );
    }
}
