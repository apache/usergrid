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

package org.apache.usergrid.corepersistence.pipeline.builder;


import org.apache.usergrid.corepersistence.pipeline.Pipeline;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.pipeline.read.collect.ResultsPageCollector;
import org.apache.usergrid.persistence.ConnectionRef;

import rx.Observable;


/**
 * A 1.0 compatibility state.  Should be removed as services are refactored
 */
@Deprecated
public class ConnectionRefBuilder {


    private final Pipeline<FilterResult<ConnectionRef>> connectionRefFilter;

    public ConnectionRefBuilder( final Pipeline<FilterResult<ConnectionRef>> connectionRefFilter ) {
       this.connectionRefFilter = connectionRefFilter;
    }


    /**
     * Build our connection refs observable
     * @return
     */
    public Observable<ResultsPage<ConnectionRef>> build(){
        return connectionRefFilter.withFilter( new ResultsPageCollector<>() ).execute();
    }
}
