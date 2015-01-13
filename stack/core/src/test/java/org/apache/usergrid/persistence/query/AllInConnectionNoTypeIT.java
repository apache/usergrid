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
package org.apache.usergrid.persistence.query;


import org.junit.Test;
import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;


/** @author tnine */
public class AllInConnectionNoTypeIT extends AbstractIteratingQueryIT {

    @Test
    public void allInConnectionNoType() throws Exception {
        allIn( new ConnectionNoTypeHelper(app) );
    }


    class ConnectionNoTypeHelper extends ConnectionHelper {

        public ConnectionNoTypeHelper( final CoreApplication app ) {
            super( app );
        }

       /**
        * (non-Javadoc) @see
        * org.apache.usergrid.persistence.query.SingleOrderByMaxLimitCollection.ConnectionHelper#getResults
        * (org.apache.usergrid.persistence.Query)
        */
        @Override
        public Results getResults( Query query ) throws Exception {
            query.setConnectionType( CONNECTION );
            // don't set it on purpose
            query.setEntityType( null );
            return app.getEntityManager().searchConnectedEntities( rootEntity, query );
        }
    }
}
