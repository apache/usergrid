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

import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Generates a list of invalid entities for input verification. To be used with Theory testing.
 *
 * @author tnine
 */
public class InvalidIdGenerator {

    private static final Logger LOG = LoggerFactory.getLogger( InvalidIdGenerator.class );

    @Retention( RetentionPolicy.RUNTIME )
    @ParametersSuppliedBy( NullFieldsSupplier.class )
    public @interface NullFields {

    }

    /**
     * Supplies all possible combination of null fields on ids
     */
    public static class NullFieldsSupplier extends ParameterSupplier {

        @Override
        public List<PotentialAssignment> getValueSources( final ParameterSignature sig ) {
            final List<PotentialAssignment> result = new ArrayList<PotentialAssignment>();

            result.add( PotentialAssignment.forValue( "nullId", null ) );
            result.add( PotentialAssignment.forValue( "nullEntityId", nullEntityId() ) );
            result.add( PotentialAssignment.forValue( "nullEntityType", nullEntityType() ) );

            return result;
        }

        /**
         * Missing fields
         */
        private static Id nullEntityId() {

            final Id entityId = mock( Id.class );

            when( entityId.getUuid() ).thenReturn( null );
            when( entityId.getType() ).thenReturn( "test" );

            return entityId;
        }

        /**
         * Null entity type
         */
        private static Id nullEntityType() {

            final Id entityId = mock( Id.class );

            when( entityId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );
            when( entityId.getType() ).thenReturn( null );

            return entityId;
        }
    }

    @Retention( RetentionPolicy.RUNTIME )
    @ParametersSuppliedBy( IllegalFieldsSupplier.class )
    public @interface IllegalFields {

    }

    /**
     * Supplies all possible combination of null fields on ids
     */
    public static class IllegalFieldsSupplier extends ParameterSupplier {

        @Override
        public List<PotentialAssignment> getValueSources( final ParameterSignature sig ) {

            final List<PotentialAssignment> result = new ArrayList<PotentialAssignment>();
            result.add( PotentialAssignment.forValue( "wrongEntityTypeLength", wrongEntityTypeLength() ) );

            return result;
        }


        private static Id wrongEntityTypeLength() {

            final Id entityId = mock( Id.class );

            //set this to a non time uuid
            when( entityId.getUuid() ).thenReturn( UUID.randomUUID() );
            when( entityId.getType() ).thenReturn( "" );

            return entityId;
        }
    }
}
