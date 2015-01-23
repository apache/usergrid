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

package org.apache.usergrid.corepersistence.rx;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Injector;

import rx.Observable;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;


/**
 * Test we see our applications
 */
public class ApplicationObservableTestIT extends AbstractCoreIT {

    @Test
    public void getAllApplications() throws Exception {

        final Application createdApplication = app.getEntityManager().getApplication();


        //now our get all apps we expect.  There may be more, but we don't care about those.
        final Set<UUID> applicationIds = new HashSet<UUID>() {{
            add( CpNamingUtils.DEFAULT_APPLICATION_ID );
            add( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
            add( CpNamingUtils.SYSTEM_APP_ID );
            add( createdApplication.getUuid() );
        }};


        //this is hacky, but our context integration b/t guice and spring is a mess.  We need to clean this up when we
        //clean up our wiring
        ManagerCache managerCache = SpringResource.getInstance().getBean( Injector.class ).getInstance( ManagerCache.class );

        Observable<Id> appObservable = ApplicationObservable.getAllApplicationIds( managerCache );

        appObservable.doOnNext( new Action1<Id>() {
            @Override
            public void call( final Id id ) {
                applicationIds.remove( id.getUuid() );
                assertEquals("Correct application type expected" ,  Application.ENTITY_TYPE, id.getType() );
            }
        } ).toBlocking().lastOrDefault( null );


        assertEquals( "Every element should have been encountered" , 0, applicationIds.size() );
    }
}
