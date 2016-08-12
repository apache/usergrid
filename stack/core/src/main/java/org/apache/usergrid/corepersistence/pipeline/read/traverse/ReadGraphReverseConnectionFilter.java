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

package org.apache.usergrid.corepersistence.pipeline.read.traverse;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.rx.impl.AsyncRepair;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getEdgeTypeFromConnectionType;

/**
 * Created by ayeshadastagiri on 8/9/16.
 */
public class ReadGraphReverseConnectionFilter extends AbstractReadReverseGraphFilter{
    private final String connectionName;

    /**
     * Create a new instance of our command
     */
    @Inject
    public ReadGraphReverseConnectionFilter( final GraphManagerFactory graphManagerFactory,
                                      @AsyncRepair final RxTaskScheduler rxTaskScheduler,
                                      final EventBuilder eventBuilder,
                                      final AsyncEventService asyncEventService,
                                      @Assisted final String connectionName ) {
        super( graphManagerFactory, rxTaskScheduler, eventBuilder, asyncEventService );
        this.connectionName = connectionName;
    }
    @Override
    protected String getEdgeTypeName() {
        return getEdgeTypeFromConnectionType( connectionName );    }
}
