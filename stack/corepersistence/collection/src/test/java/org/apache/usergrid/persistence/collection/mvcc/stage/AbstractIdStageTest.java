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

import org.apache.usergrid.persistence.collection.util.InvalidIdGenerator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import static org.mockito.Mockito.mock;


/** @author tnine */
@RunWith( Theories.class )
public abstract class AbstractIdStageTest {


    /**
     * Tests all possible combinations that will result in a NullPointerException input fail the MvccEntity interface to be a
     * mockito mock impl
     */
    @Test( expected = NullPointerException.class )
    @Theory
    public void testNonNullable(@InvalidIdGenerator.NullFields final Id id ) throws Exception {
        testStage( id );
    }


    /**
     * Tests all possible combinations that will result in an invalid input Excepts the MvccEntity interface to be a
     * mockito mock impl
     */
    @Test( expected = IllegalArgumentException.class )
    @Theory
    public void testInvalidValue(@InvalidIdGenerator.IllegalFields final Id id ) throws Exception {
            testStage( id );
    }


    /**
     * Run the stage's invalid I/O tests
     * @param id
     * @throws Exception
     */
    public void testStage( final Id id ) throws Exception {

           final ApplicationScope context = mock( ApplicationScope.class );


           //run the stage
           validateStage( new CollectionIoEvent<Id>( context, id ) );
       }


    /** Get an instance of the Func1 That takes an CollectionIoEvent with an entity type for validation testing */
    protected abstract void validateStage(CollectionIoEvent<Id> event);
}
