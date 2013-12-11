package org.apache.usergrid.persistence.collection.mvcc.entity;


/**
 * The different stages that can exist in the commit log
 */
public enum Stage {

    /**
     * These are bitmasks that represent the state's we've been through
     *
     * Active => 0000
     * RollBack => 1000
     * COMMITTED => 1100
     * POSTPROCESSOR => 1110
     * ACTIVE => 1111
     */

    /**
     * The entity has started writing but is not yet committed
     */
    ACTIVE( true, 0 ),

    /**
     * The entity has started writing but not yet committed.
     */
    ROLLBACK( true, 1 ),
    /**
     * We have applied enough writes to be able to recover via writeahead logging.  The system can recover from a crash
     * without data loss at this point
     */
    COMMITTED( false, 2 ),
    /**
     * The entity is going through post processing
     */
    POSTPROCESS( false, 6 ),

    /**
     * The entity has completed all post processing
     */
    COMPLETE( false, 14 );


    private final boolean transientStage;
    private final int id;


    private Stage( final boolean transientStage, final int id ) {
        this.transientStage = transientStage;
        this.id = id;
    }


    /**
     * Returns true if this stage is transient and should not be retained in the datastore permanently Stages such as
     * start and write don't need to be retained, but can be used to signal "in flight" updates
     */
    public boolean isTransient() {
        return transientStage;
    }


    public int getId() {
        return this.id;
    }

}
