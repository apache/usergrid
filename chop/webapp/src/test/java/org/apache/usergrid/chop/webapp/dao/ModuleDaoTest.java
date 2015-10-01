/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.dao;

import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class ModuleDaoTest {

    private static Logger LOG = LoggerFactory.getLogger( ModuleDaoTest.class );


    @Test
    public void getAll() throws Exception {

        LOG.info( "\n===ModuleDaoTest.getAll===\n" );

        List<Module> modules = ESSuiteTest.moduleDao.getAll();

        for ( Module m : modules ) {
            LOG.info( m.toString() );
        }

        assertEquals( "Wrong number of modules in elasticsearch", 2, modules.size() );
    }


    @Test
    public void get() {

        LOG.info("\n===ModuleDaoTest.get===\n");

        Module module = ESSuiteTest.moduleDao.get( ESSuiteTest.MODULE_ID_1 );
        LOG.info( "Module by ID: {} is {}", ESSuiteTest.MODULE_ID_1, module.toString() );
        assertEquals( ESSuiteTest.MODULE_GROUPID, module.getGroupId() );
    }
}
