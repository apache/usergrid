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

package org.apache.usergrid.persistence.query;


import java.util.Map;

import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.index.query.Query;


/**
 * Helper for creating entities and writing them into collections
 */
public class CollectionIoHelper implements IoHelper {

    protected final CoreApplication app;


    public CollectionIoHelper( final CoreApplication app ) {
        this.app = app;
    }


    @Override
    public void doSetup() throws Exception {
    }


    @Override
    public Entity writeEntity( Map<String, Object> entity ) throws Exception {

        return app.getEntityManager().create( "test", entity );
    }


    @Override
    public Results getResults( Query query ) throws Exception {
        app.refreshIndex();
        return app.getEntityManager().searchCollection( app.getEntityManager().getApplicationRef(), "tests", query );
    }
}
