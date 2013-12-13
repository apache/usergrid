package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/** @author tnine */
public class StageTest {

    @Test
    public void active() {

        assertTrue( Stage.ACTIVE.isTransient() );

        assertEquals(  0, Stage.ACTIVE.getId() );

        testUnique( Stage.ACTIVE );
    }


    @Test
    public void rollback() {

        assertTrue( Stage.ROLLBACK.isTransient() );

        assertEquals( 1, Stage.ROLLBACK.getId() );

        testUnique( Stage.ROLLBACK );
    }


    @Test
    public void comitted() {

        assertFalse( Stage.COMMITTED.isTransient() );

        assertEquals( 2, Stage.COMMITTED.getId() );

        testUnique( Stage.COMMITTED );
    }


    @Test
    public void postProcess() {

        assertFalse( Stage.POSTPROCESS.isTransient() );

        assertEquals( 6, Stage.POSTPROCESS.getId() );

        testUnique( Stage.POSTPROCESS );
    }


    @Test
    public void complete() {

        assertFalse( Stage.COMPLETE.isTransient() );

        assertEquals( 14, Stage.COMPLETE.getId() );

        testUnique( Stage.COMPLETE );
    }


    /** Test we don't have dups in the byte value */
    private void testUnique( Stage test ) {

        for ( Stage stage : Stage.values() ) {

            //skip self
            if ( stage == test ) {
                continue;
            }

            assertFalse( stage.getId() == test.getId() );
        }
    }
}
