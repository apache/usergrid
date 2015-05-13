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


import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.util.InvalidEntityGenerator;
import org.apache.usergrid.persistence.collection.util.InvalidIdGenerator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.EntityUtils;

import static org.mockito.Mockito.mock;


/** @author tnine */
@RunWith(Theories.class)
public abstract class AbstractEntityStageTest {

    /** Test every input with NonNull validation */
    @Test( expected = NullPointerException.class )
    @Theory
    public void testNoEntityId(@InvalidEntityGenerator.NullFields final Entity entity, @InvalidIdGenerator.NullFields final Id id) throws Exception {
        testStage(entity, id );
    }


    /** Test every Entity with */
    @Ignore("Why is this ignored?")
    @Test( expected = IllegalArgumentException.class )
    @Theory
    public void testWrongEntityType(@InvalidEntityGenerator.IllegalFields final Entity entity, @InvalidIdGenerator.IllegalFields final Id id) throws Exception {
         testStage(entity, id);
    }


    /**
     * Test the stage, should throw an exception
     * @param id
     */
    private void testStage(final Entity entity, final Id id){


        final ApplicationScope context = mock( ApplicationScope.class );

        if(entity != null){
            EntityUtils.setId( entity, id );
        }


        //run the stage
        validateStage( new CollectionIoEvent<Entity>( context, entity ) );
    }


    /** Get an instance of the Func1 That takes an CollectionIoEvent with an entity type for validation testing */
    protected abstract void validateStage(CollectionIoEvent<Entity> event);

}
