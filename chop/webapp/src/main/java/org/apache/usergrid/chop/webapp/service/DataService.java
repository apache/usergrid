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
package org.apache.usergrid.chop.webapp.service;

import com.google.inject.Inject;
import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.webapp.dao.CommitDao;
import org.apache.usergrid.chop.webapp.dao.RunDao;

import java.util.List;
import java.util.Set;

public class DataService {

    @Inject
    private RunDao runDao = null;

    @Inject
    private CommitDao commitDao = null;

    public Set<String> getTestNames(String moduleId) {
        List<Commit> commits = commitDao.getByModule(moduleId);
        return runDao.getTestNames(commits);
    }

}
