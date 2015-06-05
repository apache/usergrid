/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.corepersistence.index;

import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;

/**
 * Parse app id and get the correct strategy
 */
public class IndexLocationStrategyFactoryImpl implements IndexLocationStrategyFactory {
    private final CassandraFig cassandraFig;
    private final IndexFig indexFig;

    @Inject
    public IndexLocationStrategyFactoryImpl(final CassandraFig cassandraFig, final IndexFig indexFig){

        this.cassandraFig = cassandraFig;
        this.indexFig = indexFig;
    }
    public IndexLocationStrategy getIndexLocationStrategy(ApplicationScope applicationScope){
        if(CpNamingUtils.getManagementApplicationId().equals(applicationScope.getApplication())){
            return new ManagementIndexLocationStrategy(indexFig);
        }
        return new ApplicationIndexLocationStrategy(cassandraFig,indexFig,applicationScope);
    }

}
