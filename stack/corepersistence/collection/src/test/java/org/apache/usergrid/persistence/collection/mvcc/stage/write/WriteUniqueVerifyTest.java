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

import com.google.inject.Inject;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractMvccEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.model.entity.Entity;

import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.fromEntity;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.generateEntity;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;


/** @author tnine */

public class WriteUniqueVerifyTest extends AbstractMvccEntityStageTest {

    @Inject
    private UniqueValueSerializationStrategy uniqueValueSerializiationStrategy;


    /** Standard flow */
    @Test
    public void testStartStage() throws Exception {

        final CollectionScope collectionScope = mock( CollectionScope.class );

        // set up the mock to return the entity from the start phase
        final Entity entity = generateEntity();

        final MvccEntity mvccEntity = fromEntity( entity );

        // run the stage
        WriteUniqueVerify newStage = new WriteUniqueVerify( uniqueValueSerializiationStrategy );

        CollectionIoEvent<MvccEntity>
            result = newStage.call( new CollectionIoEvent<MvccEntity>( collectionScope, mvccEntity ) );

        assertSame("Context was correct", collectionScope, result.getEntityCollection()) ;

        // verify the log entry is correct
        MvccEntity entry = result.getEvent();

        // verify uuid and version in both the MvccEntity and the entity itself
        // assertSame is used on purpose.  We want to make sure the same instance is used, not a copy.
        // this way the caller's runtime type is retained.
        assertSame( "id correct", entity.getId(), entry.getId() );
        assertSame( "version did not not match entityId", entity.getVersion(), entry.getVersion() );
        assertSame( "Entity correct", entity, entry.getEntity().get() );
    }


    @Override
    protected void validateStage( final CollectionIoEvent<MvccEntity> event ) {
        new WriteUniqueVerify( uniqueValueSerializiationStrategy ).call( event );
    }

}


