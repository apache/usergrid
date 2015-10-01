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

import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class UserDaoTest {

    private static Logger LOG = LoggerFactory.getLogger(UserDaoTest.class);


    @Test
    public void getAll() {

        LOG.info("\n===UserDaoTest.getAll===\n");

        List<User> users = ESSuiteTest.userDao.getList();

        for (User user : users) {
            LOG.info("User is: {}", user.toString());
        }

    }


    @Test
    public void get() {

        LOG.info("\n===UserDaoTest.get===\n");

        User user = ESSuiteTest.userDao.get(ESSuiteTest.USER_1);

        LOG.info("User is: {}", user.toString());

        assertEquals("password", user.getPassword());
    }


    @Test
    public void delete() {

        LOG.info("\n===UserDaoTest.delete===\n");

        LOG.info("Users before delete: ");

        List<User> users = ESSuiteTest.userDao.getList();

        for (User user : users) {
            LOG.info("    {}", user.toString());
        }

        ESSuiteTest.userDao.delete(ESSuiteTest.USER_2);

        LOG.info("Users after delete: ");

        users = ESSuiteTest.userDao.getList();

        for (User user : users) {
            LOG.info("    {}", user.toString());
        }

        assertEquals(1, users.size());
    }


}
