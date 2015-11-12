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
package org.apache.usergrid.services.notifications.wns;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.AbstractServiceIT;
import org.junit.Test;

import java.util.UUID;

/**
 * test windows phone.
 */
public class WNSAdapterTest extends AbstractServiceIT{
    @Test
    public void windows() throws Exception {


        EntityManager em = setup.getEmf().getEntityManager(app.getId());

        Notifier notifier = new Notifier();
        notifier.setLogging(true);
        notifier.setSid("ms-app://s-1-15-2-2411381248-444863693-3819932088-4077691928-1194867744-112853457-373132695");
        notifier.setApiKey("QAHlxzMXhg5eP5q9bf/W3komtUXegf7B");
        WNSAdapter adapter = new WNSAdapter(em, notifier);
        adapter.testConnection();
    }
}
