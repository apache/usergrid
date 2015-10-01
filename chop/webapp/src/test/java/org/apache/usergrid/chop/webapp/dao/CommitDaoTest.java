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

import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class CommitDaoTest {

    private static Logger LOG = LoggerFactory.getLogger( CommitDaoTest.class );


    @Test
    public void testGetByModule() {

        LOG.info( "\n===CommitDaoTest.testGetByModule===\n" );

        List<Commit> list = ESSuiteTest.commitDao.getByModule( ESSuiteTest.MODULE_ID_2 );

        for ( Commit commit : list ) {
            LOG.info( commit.toString() );
        }

        assertEquals( 2, list.size() );
    }


    @Test
    public void testGet() {

        LOG.info( "\n===CommitDaoTest.testGet===\n" );

        Commit commit = ESSuiteTest.commitDao.getByModule( ESSuiteTest.MODULE_ID_1 ).get( 0 );

        LOG.info( commit.toString() );

        assertEquals( ESSuiteTest.COMMIT_ID_1, commit.getId() );
    }
}
