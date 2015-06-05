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
import net.jcip.annotations.NotThreadSafe;
import org.apache.usergrid.corepersistence.TestIndexModule;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.index.impl.EsRunner;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * Classy class class.
 */
@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
@NotThreadSafe
public class IndexNamingTest {
    @Inject
    public CassandraFig cassandraFig;

    @Inject
    public IndexFig indexFig;

    @Inject
    public IndexLocationStrategyFactory indexLocationStrategyFactory;

    private ApplicationScope applicationScope;
    private ApplicationScope managementApplicationScope;
    private ApplicationIndexLocationStrategy applicationLocationStrategy;
    private ManagementIndexLocationStrategy managementLocationStrategy;

    @Before
    public void setup(){
        this.applicationScope = CpNamingUtils.getApplicationScope(UUID.randomUUID());
        this.managementApplicationScope = CpNamingUtils.getApplicationScope(CpNamingUtils.getManagementApplicationId().getUuid());
        this.managementLocationStrategy = new ManagementIndexLocationStrategy(indexFig);
        this.applicationLocationStrategy = new ApplicationIndexLocationStrategy(cassandraFig,indexFig,applicationScope);
    }

    @Test
    public void managementNaming(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(managementApplicationScope);
        assertTrue(indexLocationStrategy.getIndex(null).equals(managementLocationStrategy.getIndex(null)));

    }
    @Test
    public void applicationNaming(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope);
        assertTrue(indexLocationStrategy.getIndex(null).equals(applicationLocationStrategy.getIndex(null)));
    }

}
