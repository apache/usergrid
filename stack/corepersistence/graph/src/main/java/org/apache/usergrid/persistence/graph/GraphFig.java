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

    public static final String READ_CL = "usergrid.graph.read.cl";

    public static final String WRITE_CL = "usergrid.graph.write.cl";

    @Default( "1000" )
    @Key( SCAN_PAGE_SIZE )
    int getScanPageSize();

    @Default( "CL_ONE" )
    @Key( READ_CL )
    String getReadCL();

    @Default( "CL_QUORUM" )
    @Key( WRITE_CL )
    String getWriteCL();


}

