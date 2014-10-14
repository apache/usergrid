package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;

import static org.junit.Assert.assertTrue;


/**
 *
 *
 */
public class UUIDComparatorTest {

    @Test
    public void validateComparatorValues(){

        //smaller
        UUID first = UUIDGenerator.newTimeUUID();

        //larger
        UUID second = UUIDGenerator.newTimeUUID();


        /**
         * Returns < 0 when the first is smaller
         */
        assertTrue( UUIDComparator.staticCompare(first, second) < 0);

        /**
         * Returns > 0 when the first argument is larger
         */
        assertTrue( UUIDComparator.staticCompare( second, first ) > 0);


        UUID equal = UUID.fromString( first.toString() );

        /**
         * Returns 0 when they're equal
         */
        assertTrue(UUIDComparator.staticCompare( first, equal ) == 0);

    }

}
