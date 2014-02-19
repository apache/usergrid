package org.apache.usergrid.persistence.graph.serialization.impl;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.model.ConsistencyLevel;


/**
 *  Simple configuration to wrap GuicyFig until it supports enums.  Need to be removed once it does
 *
 */
@Singleton
public class CassandraConfigImpl implements CassandraConfig {


    private ConsistencyLevel readCl;
    private ConsistencyLevel writeCl;
    private int pageSize;


    @Inject
    public CassandraConfigImpl( final GraphFig graphFig ) {

        this.readCl = ConsistencyLevel.valueOf( graphFig.getReadCL() );

        this.writeCl = ConsistencyLevel.valueOf( graphFig.getWriteCL() );

        this.pageSize = graphFig.getScanPageSize();

        //add the listeners to update the values
        graphFig.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                final String propName = evt.getPropertyName();

                if ( GraphFig.SCAN_PAGE_SIZE.equals( propName ) ) {
                    pageSize = ( Integer ) evt.getNewValue();
                }

                else if ( GraphFig.READ_CL.equals( propName ) ) {
                    readCl = ConsistencyLevel.valueOf( evt.getNewValue().toString() );
                }

                else if ( GraphFig.WRITE_CL.equals( propName ) ) {
                    writeCl = ConsistencyLevel.valueOf( evt.getNewValue().toString() );
                }
            }
        } );
    }


    @Override
    public ConsistencyLevel getReadCL() {
        return readCl;
    }


    @Override
    public ConsistencyLevel getWriteCL() {
        return writeCl;
    }


    @Override
    public int getScanPageSize() {
        return pageSize;
    }
}
