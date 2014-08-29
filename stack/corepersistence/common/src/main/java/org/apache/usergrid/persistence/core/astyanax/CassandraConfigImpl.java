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
package org.apache.usergrid.persistence.core.astyanax;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


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


    @Inject
    public CassandraConfigImpl( final CassandraFig cassandraFig ) {

        this.readCl = ConsistencyLevel.valueOf( cassandraFig.getReadCL() );

        this.writeCl = ConsistencyLevel.valueOf( cassandraFig.getWriteCL() );


        //add the listeners to update the values
        cassandraFig.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                final String propName = evt.getPropertyName();

                if ( CassandraFig.READ_CL.equals( propName ) ) {
                    readCl = ConsistencyLevel.valueOf( evt.getNewValue().toString() );
                }

                else if ( CassandraFig.WRITE_CL.equals( propName ) ) {
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


}
