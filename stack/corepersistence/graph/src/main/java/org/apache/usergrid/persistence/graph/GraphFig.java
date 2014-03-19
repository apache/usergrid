package org.apache.usergrid.persistence.graph;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 *
 *
 */
public interface GraphFig extends GuicyFig {


    public static final String SCAN_PAGE_SIZE = "usergrid.graph.scan.page.size";

    public static final String REPAIR_CONCURRENT_SIZE = "usergrid.graph.repair.concurrent.size";

    public static final String REPAIR_TIMEOUT = "usergrid.graph.repair.timeout";


    public static final String TIMEOUT_SIZE = "usergrid.graph.timeout.page.size";

    public static final String TIMEOUT_TASK_TIME = "usergrid.graph.timeout.task.time";

    public static final String READ_CL = "usergrid.graph.read.cl";

    public static final String WRITE_CL = "usergrid.graph.write.cl";

    public static final String WRITE_TIMEOUT = "usergrid.graph.write.timeout";

    public static final String READ_TIMEOUT = "usergrid.graph.read.timeout";

    @Default("1000")
    @Key(SCAN_PAGE_SIZE)
    int getScanPageSize();

    @Default("CL_ONE")
    @Key(READ_CL)
    String getReadCL();

    @Default("CL_QUORUM")
    @Key(WRITE_CL)
    String getWriteCL();

//    @Default("10000")
//    @Key(WRITE_TIMEOUT)
//    int getWriteTimeout();

    /**
     * Get the read timeout (in milliseconds) that we should allow when reading from the data source
     */
    @Default( "10000" )
    @Key( READ_TIMEOUT )
    int getReadTimeout();

    @Default( "100" )
    @Key( TIMEOUT_SIZE )
    int getTimeoutReadSize();

    @Default( "500" )
    @Key( TIMEOUT_TASK_TIME )
    long getTaskLoopTime();

    @Default( "10" )
    @Key( REPAIR_CONCURRENT_SIZE )
    int getRepairConcurrentSize();

    @Default("10000")
      @Key(WRITE_TIMEOUT)
      int getRepairTimeout();
}

