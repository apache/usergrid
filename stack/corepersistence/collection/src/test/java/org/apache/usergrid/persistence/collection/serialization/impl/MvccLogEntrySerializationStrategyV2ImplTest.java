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

package org.apache.usergrid.persistence.collection.serialization.impl;


import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;


@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class MvccLogEntrySerializationStrategyV2ImplTest extends MvccLogEntrySerializationStrategyImplTest  {


    @Inject
    private MvccLogEntrySerializationStrategyV2Impl logEntryStrategy;

    @Override
    protected MvccLogEntrySerializationStrategy getLogEntryStrategy() {
        return logEntryStrategy;
    }

    @Test
    public void checkVersion(){
        assertEquals(CollectionDataVersions.LOG_REMOVAL.getVersion(), logEntryStrategy.getImplementationVersion());
    }
}
