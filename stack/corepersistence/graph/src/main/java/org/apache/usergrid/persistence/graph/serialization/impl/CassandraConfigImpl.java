/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
