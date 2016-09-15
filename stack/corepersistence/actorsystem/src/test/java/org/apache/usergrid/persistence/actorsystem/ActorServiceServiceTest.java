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
package org.apache.usergrid.persistence.actorsystem;

import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;


@RunWith( ITRunner.class )
@UseModules( ActorSystemModule.class )
public class ActorServiceServiceTest {
    private static final Logger logger = LoggerFactory.getLogger( ActorServiceServiceTest.class );

    @Inject
    ActorSystemFig actorSystemFig;

    @Inject
    ActorSystemManager actorSystemManager;


    private static AtomicBoolean startedAkka = new AtomicBoolean( false );


    @Test
    public void testBasicOperation() throws Exception {

        RouterProducer routerProducer = Mockito.mock( RouterProducer.class );
        actorSystemManager.registerRouterProducer( routerProducer );

        actorSystemManager.start( "localhost", 2770, "us-east" );
        actorSystemManager.waitForClientActor();

        verify( routerProducer ).addConfiguration( any() );
        verify( routerProducer ).produceRouter( any(), eq("io") );

    }

}
