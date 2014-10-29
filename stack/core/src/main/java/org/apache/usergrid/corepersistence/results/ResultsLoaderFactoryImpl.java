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

package org.apache.usergrid.corepersistence.results;


import org.apache.usergrid.corepersistence.CpManagerCache;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.query.Query;

import com.google.inject.Inject;


/**
 * Factory for creating results
 */
public class ResultsLoaderFactoryImpl implements ResultsLoaderFactory {

    private final CpManagerCache managerCache;


    @Inject
    public ResultsLoaderFactoryImpl( final CpManagerCache managerCache ) {
        this.managerCache = managerCache;
    }


    @Override
    public ResultsLoader getLoader( final ApplicationScope applicationScope, 
            final EntityRef ownerId, final Query.Level resultsLevel ) {

        ResultsVerifier verifier;

        if ( resultsLevel == Query.Level.REFS ) {
            verifier = new RefsVerifier();
        }
        else if ( resultsLevel == Query.Level.IDS ) {
            verifier = new RefsVerifier();
        }
        else {
            verifier = new EntityVerifier(Query.MAX_LIMIT);
        }

        return new FilteringLoader( managerCache, verifier, ownerId, applicationScope );
    }
}
