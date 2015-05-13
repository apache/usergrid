/*
 *
 *  *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  *
 *
 */

package org.apache.usergrid.persistence.collection.util;/*
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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.StringField;

import com.fasterxml.uuid.UUIDComparator;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;


/**
 * Utility for constructing representative log entries for mock serialziation from high to low
 */
public class UniqueValueEntryMock {

    public static final ReversedUUIDUniqueComparator COMPARATOR = new ReversedUUIDUniqueComparator();

    private final List<UniqueValue> entries = new ArrayList<>();

    private final Id entityId;


    /**
     * Create a mock list of versions of the specified size
     *
     * @param entityId The entity Id to use
     * @param versions The versions
     */
    private UniqueValueEntryMock( final Id entityId, final List<UUID> versions ) {

        this.entityId = entityId;

        for (UUID version: versions ) {


            entries.add( new UniqueValueImpl( new StringField( "fieldWithVersion", version.toString() ), entityId,
                    version ) );
        }

        Collections.sort(entries, COMPARATOR);
    }


    /**
     * Init the mock with the given data structure
     * @param uniqueValueSerializationStrategy The serializer to mock
     * @param scope
     * @throws com.netflix.astyanax.connectionpool.exceptions.ConnectionException
     */
    private void initMock(  final UniqueValueSerializationStrategy uniqueValueSerializationStrategy, final  ApplicationScope scope )

            throws ConnectionException {

        //wire up the mocks
        when(uniqueValueSerializationStrategy.getAllUniqueFields( same( scope ), same( entityId ))).thenReturn( entries.iterator() );
    }



    /**
     * Get the unique value at the index
     * @param index
     * @return
     */
    public UniqueValue getEntryAtIndex(final int index){
      return entries.get( index );
    }

    /**
     *
     * @param uniqueValueSerializationStrategy The mock to use
     * @param scope The scope to use
     * @param entityId The entityId to use
     * @param versions The version numbers to mock
     * @throws com.netflix.astyanax.connectionpool.exceptions.ConnectionException
     */
    public static UniqueValueEntryMock createUniqueMock(
            final UniqueValueSerializationStrategy uniqueValueSerializationStrategy, final ApplicationScope scope,
            final Id entityId, final List<UUID> versions ) throws ConnectionException {

        UniqueValueEntryMock mock = new UniqueValueEntryMock( entityId, versions );
        mock.initMock( uniqueValueSerializationStrategy, scope );

        return mock;
    }


    private static final class ReversedUUIDUniqueComparator implements Comparator<UniqueValue> {



        @Override
        public int compare( final UniqueValue o1, final UniqueValue o2 ) {
            return UUIDComparator.staticCompare( o1.getEntityVersion(), o2.getEntityVersion() ) * -1;
        }
    }
}
