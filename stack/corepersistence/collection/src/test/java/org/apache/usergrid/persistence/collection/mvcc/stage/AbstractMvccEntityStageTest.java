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
package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.util.InvalidEntityGenerator;
import org.apache.usergrid.persistence.collection.util.InvalidIdGenerator;
import org.apache.usergrid.persistence.collection.util.InvalidMvccEntityGenerator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.EntityUtils;

import com.google.common.base.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author tnine
 */
@RunWith( Theories.class )
public abstract class AbstractMvccEntityStageTest {

    private static final Logger LOG = LoggerFactory.getLogger( AbstractMvccEntityStageTest.class );

    /**
     * Tests all possible combinations that will result in a NullPointerException input fail the
     * MvccEntity interface to be a mockito mock impl
     */
    @Test( expected = NullPointerException.class )
    @Theory
    public void testNonNullable(
            @InvalidMvccEntityGenerator.NullFields final MvccEntity mvccEntity,
            @InvalidEntityGenerator.NullFields final Entity entity,
            @InvalidIdGenerator.NullFields final Id nullValidationFailId ) throws Exception {

        testStage( mvccEntity, entity, nullValidationFailId );
    }

    /**
     * Tests all possible combinations that will result in an invalid input Excepts the MvccEntity
     * interface to be a mockito mock impl
     */
    @Test( expected = IllegalArgumentException.class )
    @Theory
    public void testInvalidValue(
            @InvalidMvccEntityGenerator.IllegalFields final MvccEntity mvccEntity,
            @InvalidEntityGenerator.IllegalFields final Entity entity,
            @InvalidIdGenerator.IllegalFields final Id invalidValueId ) throws Exception {

        testStage( mvccEntity, entity, invalidValueId );
    }

    public void testStage(
            final MvccEntity mvccEntity, final Entity entity, final Id id ) throws Exception {

        if ( entity != null ) {
            EntityUtils.setId( entity, id );
        }

        final ApplicationScope context = mock( ApplicationScope.class );

        if ( mvccEntity != null ) {
            when( mvccEntity.getEntity() ).thenReturn( Optional.fromNullable( entity ) );
            when( mvccEntity.getId() ).thenReturn( id );
        }

        validateStage( new CollectionIoEvent<MvccEntity>( context, mvccEntity ) );
    }

    /**
     * Get an instance of the Func1 That takes an CollectionIoEvent with an entity type for
     * validation testing
     */
    protected abstract void validateStage( CollectionIoEvent<MvccEntity> event );
}
