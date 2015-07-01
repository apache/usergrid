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
package org.apache.usergrid.services;


import java.util.UUID;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.utils.UUIDUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.apache.usergrid.services.simple.SimpleService;

import static org.apache.usergrid.TestHelper.uniqueApp;
import static org.apache.usergrid.TestHelper.uniqueOrg;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;



public class ServiceFactoryIT extends AbstractServiceIT {

    private static final Logger logger = LoggerFactory.getLogger( ServiceFactoryIT.class );


    @Test
    public void testPackagePrefixes() throws Exception {
        logger.info( "test package prefixes" );

        Entity appInfo = setup.getEmf().createApplicationV2(uniqueOrg(), uniqueApp());
        UUID applicationId = appInfo.getUuid();


        ServiceManager sm = setup.getSmf().getServiceManager( applicationId );
        Service service = sm.getService( "simple" );
        assertEquals( "/simple", service.getServiceType() );
        assertNotNull( service );
        assertEquals( SimpleService.class, service.getClass() );
    }
}
