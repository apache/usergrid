/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.count;


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.count.common.Count;

import me.prettyprint.cassandra.model.HCounterColumnImpl;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;


/**
 * Encapsulate counter writes to Cassandra
 *
 * @author zznate
 */
public class CassandraCounterStore implements CounterStore {
    private static final Logger log = LoggerFactory.getLogger( CassandraCounterStore.class );

    // keep track of exceptions thrown in scheduler so we can reduce noise in logs
    private Map<String, Integer> counterInsertFailures = new HashMap<String, Integer>();

    private final Keyspace keyspace;


    public CassandraCounterStore( Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    public void save( Count count ) {
        this.save( Arrays.asList( count ) );
    }


    public void save( Collection<Count> counts ) {
        Map<String, Count> countHolder = new HashMap<String, Count>();
        for ( Count count : counts ) {
            Count c = countHolder.get( count.getCounterName() );
            if ( c != null ) {
                c.apply( count );
            }
            else {
                countHolder.put( count.getCounterName(), count );
            }
        }
        Mutator<ByteBuffer> mutator = HFactory.createMutator( keyspace, be );
        for ( Count count : countHolder.values() ) {
            mutator.addCounter( count.getKeyNameBytes(), count.getTableName(),
                    new HCounterColumnImpl( count.getColumnName(), count.getValue(),
                            count.getColumnNameSerializer() ) );
        }
        try {
            mutator.execute();
        }
        catch ( Exception e ) {

            // errors here happen a lot on shutdown, don't fill the logs with them
            String error = e.getClass().getCanonicalName();
            if (counterInsertFailures.get( error ) == null) {
                log.error( "CounterStore insert failed, first instance", e);
                counterInsertFailures.put( error, 1);

            } else {
                int count = counterInsertFailures.get(error) + 1;
                counterInsertFailures.put(error, count);

                log.error(error + " caused CounterStore insert failure, count =  " + count, e);
            }
        }
    }
}
