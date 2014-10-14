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

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Generates a list of invalid MvccEntities for input verification.  To be used with theory testing.
 *
 * @author tnine
 */
public class InvalidMvccEntityGenerator {

    private static final Logger LOG = LoggerFactory.getLogger( InvalidIdGenerator.class );

    @Retention( RetentionPolicy.RUNTIME )
    @ParametersSuppliedBy( NullFieldsSupplier.class )
    public @interface NullFields {

    }


    /** Supplies all possible combination of null fields on entities */
    public static class NullFieldsSupplier extends ParameterSupplier {


        @Override
        public List<PotentialAssignment> getValueSources( final ParameterSignature sig ) {

            final List<PotentialAssignment> result = new ArrayList<PotentialAssignment>();

            result.add( PotentialAssignment.forValue( "nullValue", null ) );
            result.add( PotentialAssignment.forValue( "nullSubTypes", nullSubElements() ) );

            return result;
        }


        /** Missing fields */
        private static MvccEntity nullSubElements() {

            final MvccEntity entity = mock( MvccEntity.class );
            return entity;
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

            result.add( PotentialAssignment.forValue( "wrongUuidType", wrongUuidType() ) );
            result.add( PotentialAssignment.forValue( "invalidSubTypes", invalidId() ) );

            return result;
        }


        /** Incorrect fields */


        private static MvccEntity wrongUuidType() {

            final Id id = mock( Id.class );

            when( id.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );
            when( id.getType() ).thenReturn( "test" );

            final MvccEntity entity = mock( MvccEntity.class );

            when( entity.getId() ).thenReturn( id );
            when( entity.getVersion() ).thenReturn( UUID.randomUUID() );

            return entity;
        }

        private static MvccEntity invalidId() {

            final Id id = mock( Id.class );

            when( id.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );
            when( id.getType() ).thenReturn( "" );

            final MvccEntity entity = mock( MvccEntity.class );

            when( entity.getId() ).thenReturn( id );
            when( entity.getVersion() ).thenReturn( UUID.randomUUID() );

            return entity;
        }

    }


}
