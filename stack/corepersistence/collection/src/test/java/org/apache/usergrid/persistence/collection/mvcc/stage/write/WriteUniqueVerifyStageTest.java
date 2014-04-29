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
package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.jukito.UseModules;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractMvccEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;

import static org.mockito.Mockito.mock;


/**
 * TODO: Update the test to correctly test for detecting more than 1 duplicate and exception handling correctly
 *
 * @author tnine
 */
@UseModules( TestCollectionModule.class )
public class WriteUniqueVerifyStageTest extends AbstractMvccEntityStageTest {

    @Override
    protected void validateStage( final CollectionIoEvent<MvccEntity> event ) {
        UniqueValueSerializationStrategy uvstrat = mock( UniqueValueSerializationStrategy.class );
        SerializationFig fig = mock( SerializationFig.class );
        new WriteUniqueVerify( uvstrat, fig ).call( event );
    }
}


