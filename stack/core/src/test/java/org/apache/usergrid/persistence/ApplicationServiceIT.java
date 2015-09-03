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
package org.apache.usergrid.persistence;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.HashMap;
import java.util.Map;

/**
 * testy test
 */
public class ApplicationServiceIT extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationServiceIT.class);


    public ApplicationServiceIT() {
        super();
    }

    @Test
    public void testDeletes() throws Exception{
        EntityManager entityManager = this.app.getEntityManager();
        Map<String,Object> map = new HashMap<>();
        Entity entity = entityManager.create("tests", map);
        this.app.refreshIndex();
        Thread.sleep(500);
        Observable<Id> ids =
            this.app.getApplicationService().deleteAllEntities(CpNamingUtils.getApplicationScope(entityManager.getApplicationId()));
        ids.toBlocking().last();
        this.app.refreshIndex();
        Thread.sleep(500);
        entity = entityManager.get(entity);
        Assert.assertNull(entity);
    }

}
