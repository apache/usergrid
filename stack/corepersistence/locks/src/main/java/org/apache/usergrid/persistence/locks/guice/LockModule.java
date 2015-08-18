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

package org.apache.usergrid.persistence.locks.guice;


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.locks.LockManager;
import org.apache.usergrid.persistence.locks.impl.CassandraLockFig;
import org.apache.usergrid.persistence.locks.impl.CassandraLockManager;
import org.apache.usergrid.persistence.locks.impl.LockConsistency;
import org.apache.usergrid.persistence.locks.impl.LockConsistencyImpl;
import org.apache.usergrid.persistence.locks.impl.LockProposalSerialization;
import org.apache.usergrid.persistence.locks.impl.LockProposalSerializationImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;


/**
 * Guice configuration of the lock module
 */
public class LockModule extends AbstractModule {

    @Override
    protected void configure() {

        //install the lock config
        install( new GuicyFigModule( CassandraLockFig.class ) );

        //bind our lockManager
        bind( LockManager.class ).to( CassandraLockManager.class );

        bind( LockConsistency.class ).to( LockConsistencyImpl.class );

        bind( LockProposalSerialization.class ).to( LockProposalSerializationImpl.class );

        Multibinder<Migration> migrationBinder = Multibinder.newSetBinder( binder(), Migration.class );
        //entity serialization versions
        migrationBinder.addBinding().to( Key.get( LockProposalSerialization.class ) );
    }
}
