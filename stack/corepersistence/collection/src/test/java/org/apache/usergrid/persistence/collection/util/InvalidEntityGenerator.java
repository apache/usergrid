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

import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Generates a list of invalid entities for input verification.  To be used with Theory testing. In this case, we just
 * inject all invalid types of Ids from the Id generator into the Entity
 *
 * @author tnine
 */
public class InvalidEntityGenerator {


    @Retention(RetentionPolicy.RUNTIME)
    @ParametersSuppliedBy(NullFieldsSupplier.class)
    public @interface NullFields {

    }


    /** Supplies all possible combination of null fields on entities */
    public static class NullFieldsSupplier extends ParameterSupplier {


        @Override
        public List<PotentialAssignment> getValueSources( final ParameterSignature sig ) {


            final List<PotentialAssignment> result = new ArrayList<PotentialAssignment>();

            result.add( PotentialAssignment.forValue( "nullEntity", null ) );
            result.add( PotentialAssignment.forValue( "nullIdsEntity", new Entity() ) );


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
