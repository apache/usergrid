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
package org.apache.usergrid.persistence.query.ir.result;


import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Query.Level;
import static org.apache.usergrid.persistence.index.query.Query.Level.IDS;
import static org.apache.usergrid.persistence.index.query.Query.Level.REFS;


/** Implementation for loading connectionResults results */
public class ConnectionResultsLoaderFactory implements ResultsLoaderFactory {

    private final ConnectionRef connection;


    public ConnectionResultsLoaderFactory( ConnectionRef connection ) {
        this.connection = connection;
    }


    @Override
    public ResultsLoader getResultsLoader( EntityManager em, Query query, Level level ) {
        switch ( level ) {
            case IDS://Note that this is technically wrong.  However, to support backwards compatibility with the
                // existing apis and usage, both ids and refs return a connection ref when dealing with connections
            case REFS:
                return new ConnectionRefLoader( connection );
            default:
                return new EntityResultsLoader( em );
        }
    }
}
