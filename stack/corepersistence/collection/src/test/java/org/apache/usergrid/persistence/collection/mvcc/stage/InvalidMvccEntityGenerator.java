package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.PotentialAssignment;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
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

            when( entity.getVersion() ).thenReturn( UUIDGenerator.newTimeUUID() );

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
            result.add( PotentialAssignment.forValue( "invalidSubTypes", mock( MvccEntity.class )) );


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


    }


}
