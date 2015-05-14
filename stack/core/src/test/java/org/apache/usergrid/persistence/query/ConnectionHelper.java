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


import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Query;


/**
 * Helper class for creating entiites, then writing them to a connection
 */
public class ConnectionHelper extends CollectionIoHelper {

    /**
     *
     */
    protected static final String CONNECTION = "connection";
    protected Entity rootEntity;


    public ConnectionHelper( final CoreApplication app ) {
        super( app );
    }


    @Override
    public void doSetup() throws Exception {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put( "name", "rootentity" );
        rootEntity = app.getEntityManager().create( "root", data );
    }


    @Override
    public Entity writeEntity( Map<String, Object> entity ) throws Exception {

        // write to the collection
        Entity created = super.writeEntity( entity );
        app.getEntityManager().createConnection( rootEntity, CONNECTION, created );


        return created;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.persistence.query.SingleOrderByMaxLimitCollection.CollectionIoHelper#
     * getResults(org.apache.usergrid.persistence.Query)
     */
    @Override
    public Results getResults( Query query ) throws Exception {

        app.refreshIndex();
        query.setConnectionType( CONNECTION );
        query.setEntityType( "test" );

        return app.getEntityManager().searchTargetEntities(rootEntity, query);
    }
}
