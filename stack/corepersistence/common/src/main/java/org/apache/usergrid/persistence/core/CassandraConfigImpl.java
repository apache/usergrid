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
package org.apache.usergrid.persistence.core;


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
    private int[] shardSettings;
    private ConsistencyLevel consistentCl;

    // DataStax driver's CL
    private com.datastax.driver.core.ConsistencyLevel dataStaxReadCl;
    private com.datastax.driver.core.ConsistencyLevel dataStaxWriteCl;
    private com.datastax.driver.core.ConsistencyLevel dataStaxReadConsistentCl;



    @Inject
    public CassandraConfigImpl( final CassandraFig cassandraFig ) {

        this.readCl = ConsistencyLevel.valueOf( cassandraFig.getAstyanaxReadCL() );

        this.writeCl = ConsistencyLevel.valueOf( cassandraFig.getAstyanaxWriteCL() );

        this.shardSettings = parseShardSettings( cassandraFig.getShardValues() );

        this.consistentCl = ConsistencyLevel.valueOf(cassandraFig.getAstyanaxConsistentReadCL());

        this.dataStaxReadCl = com.datastax.driver.core.ConsistencyLevel.valueOf( cassandraFig.getReadCl());

        this.dataStaxReadConsistentCl = com.datastax.driver.core.ConsistencyLevel.valueOf( cassandraFig.getReadClConsistent());

        this.dataStaxWriteCl = com.datastax.driver.core.ConsistencyLevel.valueOf( cassandraFig.getWriteCl() );

        //add the listeners to update the values
        cassandraFig.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                final String propName = evt.getPropertyName();

                if ( CassandraFig.ASTYANAX_READ_CL.equals( propName ) ) {
                    readCl = ConsistencyLevel.valueOf( evt.getNewValue().toString() );
                }

                else if ( CassandraFig.ASTYANAX_WRITE_CL.equals( propName ) ) {
                    writeCl = ConsistencyLevel.valueOf( evt.getNewValue().toString() );
                }
                else if (CassandraFig.SHARD_VALUES.equals(propName)){
                    shardSettings = parseShardSettings( cassandraFig.getShardValues() );
                }
            }
        } );
    }


    @Override
    public ConsistencyLevel getReadCL() {
        return readCl;
    }

    @Override
    public ConsistencyLevel getConsistentReadCL() {
        return consistentCl;
    }

    @Override
    public ConsistencyLevel getWriteCL() {
        return writeCl;
    }

    @Override
    public com.datastax.driver.core.ConsistencyLevel getDataStaxReadCl() {
        return dataStaxReadCl;
    }

    @Override
    public com.datastax.driver.core.ConsistencyLevel getDataStaxWriteCl() {
        return dataStaxWriteCl;
    }

    @Override
    public com.datastax.driver.core.ConsistencyLevel getDataStaxReadConsistentCl() {
        return dataStaxReadConsistentCl;
    }


    @Override
    public int[] getShardSettings() {
      return shardSettings;
    }

    private int[] parseShardSettings(final String value){
        final String[] shardHistory = value.split( "," );

        int[] settings = new int [shardHistory.length];

        for(int i = 0; i < shardHistory.length; i ++){
            settings[i] = Integer.parseInt( shardHistory[i] );
        }

      return settings;
    }
}
