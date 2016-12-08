/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.uniquevalues;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Batch;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;


@Singleton
public class UniqueValuesTableImpl implements UniqueValuesTable {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValuesTableImpl.class );

    private final UniqueValueSerializationStrategy strat;
    private final UniqueValuesFig uniqueValuesFig;
    private final Session session;

    @Inject
    public UniqueValuesTableImpl( final UniqueValueSerializationStrategy strat,
                                  final UniqueValuesFig uniqueValuesFig,
                                  final Session session ) {
        this.strat = strat;
        this.uniqueValuesFig = uniqueValuesFig;
        this.session = session;
    }


    @Override
    public Id lookupOwner( ApplicationScope scope, String type, Field field) throws ConnectionException {

        UniqueValueSet set = strat.load( scope, type, Collections.singletonList( field ) );
        UniqueValue uv  = set.getValue( field.getName() );
        return uv == null ? null : uv.getEntityId();
    }

    @Override
    public void reserve( ApplicationScope scope, Id owner, UUID version, Field field ) throws ConnectionException {

        UniqueValue uv = new UniqueValueImpl( field, owner, version);
        final BatchStatement statement = strat.writeCQL( scope, uv, uniqueValuesFig.getUniqueValueReservationTtl() );
        session.execute(statement);
    }

    @Override
    public void confirm( ApplicationScope scope, Id owner, UUID version, Field field) throws ConnectionException {

        UniqueValue uv = new UniqueValueImpl( field, owner, version);
        final BatchStatement statement = strat.writeCQL( scope, uv, -1 );
        session.execute(statement);

    }

    @Override
    public void cancel( ApplicationScope scope, Id owner, UUID version, Field field) throws ConnectionException {

        UniqueValue uv = new UniqueValueImpl( field, owner, version );
        final BatchStatement statement = strat.deleteCQL( scope, uv );
        session.execute(statement);
    }

    @Override
    public Iterator<UniqueValue> getUniqueValues(ApplicationScope scope, Id entityId) {
        return strat.getAllUniqueFields( scope, entityId );
    }

}
