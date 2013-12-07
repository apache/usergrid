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
import org.apache.usergrid.persistence.model.entity.Entity;
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


            result.add( PotentialAssignment.forValue( "nullEntityId", nullEntityId() ) );
            result.add( PotentialAssignment.forValue( "nullEntityType", nullEntityType() ) );

            //copy null Id info
            for ( PotentialAssignment assignment : new InvalidIdGenerator.NullFieldsSupplier()
                    .getValueSources( sig ) ) {


                try {
                    result.add( PotentialAssignment
                            .forValue( assignment.getDescription(), invalidEntityId( ( Id ) assignment.getValue() ) ) );
                }
                catch ( PotentialAssignment.CouldNotGenerateValueException e ) {
                    throw new RuntimeException( e );
                }
            }


            return result;
        }


        /** Missing fields */
        private static MvccEntity nullEntityId() {


            final MvccEntity entity = mock( MvccEntity.class );

            when( entity.getId() ).thenReturn( null );
            when( entity.getVersion() ).thenReturn( UUIDGenerator.newTimeUUID() );

            return entity;
        }


        /** Null entity type */
        private static MvccEntity nullEntityType() {

            final Id id = mock( Id.class );

            when( id.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );
            when( id.getType() ).thenReturn( "test" );

            final MvccEntity entity = mock( MvccEntity.class );

            when( entity.getId() ).thenReturn( id );
            when( entity.getVersion() ).thenReturn( null );

            return entity;
        }


        /** Generate and MVccEntity that is correct with the id (which can be invalid) */
        private static MvccEntity invalidEntityId( Id id ) {

            final MvccEntity entity = mock( MvccEntity.class );

            when( entity.getId() ).thenReturn( id );
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

            //copy invalid id info
            for ( PotentialAssignment assignment : new InvalidIdGenerator.IllegalFieldsSupplier()
                    .getValueSources( sig ) ) {


                try {
                    result.add( PotentialAssignment
                            .forValue( assignment.getDescription(), new Entity( ( Id ) assignment.getValue() ) ) );
                }
                catch ( PotentialAssignment.CouldNotGenerateValueException e ) {
                    throw new RuntimeException( e );
                }
            }


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
