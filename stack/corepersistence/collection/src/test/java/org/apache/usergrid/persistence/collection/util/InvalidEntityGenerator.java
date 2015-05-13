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
package org.apache.usergrid.persistence.collection.util;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.PotentialAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.EntityUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Generates a list of invalid entities for input verification.  To be used with Theory testing. In this case, we just
 * inject all invalid types of Ids from the Id generator into the Entity
 *
 * @author tnine
 */
public class InvalidEntityGenerator {

    private static final Logger LOG = LoggerFactory.getLogger( InvalidIdGenerator.class );

    @Retention(RetentionPolicy.RUNTIME)
    @ParametersSuppliedBy(NullFieldsSupplier.class)
    public @interface NullFields {

    }


    /** Supplies all possible combination of null fields on entities */
    public static class NullFieldsSupplier extends ParameterSupplier {


        @Override
        public List<PotentialAssignment> getValueSources( final ParameterSignature sig ) {

            final List<PotentialAssignment> result = new ArrayList<PotentialAssignment>();

            final Entity entity = mock( Entity.class );
            when( entity.getId() ).thenReturn( null );
            when( entity.getVersion() ).thenReturn( null );

            result.add( PotentialAssignment.forValue( "nullEntity", null ) );
            result.add( PotentialAssignment.forValue( "nullIdsEntity", entity ) );

            return result;
        }
    }


    @Retention( RetentionPolicy.RUNTIME )
    @ParametersSuppliedBy( IllegalFieldsSupplier.class )
    public @interface IllegalFields {

    }


    /** Supplies all possible combination of null fields on entities */
    public static class IllegalFieldsSupplier extends ParameterSupplier {


        @Override
        public List<PotentialAssignment> getValueSources( final ParameterSignature sig ) {

            final List<PotentialAssignment> result = new ArrayList<PotentialAssignment>();

            result.add( PotentialAssignment
                    .forValue( "invalidVersion", invalidVersion() ) );

            return result;
        }

        private Entity invalidVersion(){
            Entity entity = new Entity("test");
            EntityUtils.setVersion( entity, UUID.randomUUID() );
            return entity;
        }
    }
}
