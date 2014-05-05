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
package org.apache.usergrid.chop.webapp.service.chart.group;

import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Run;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GroupByCommit {

    // <commitId, List<Run>>
    private Map<String, List<Run>> commitRuns = new LinkedHashMap<String, List<Run>>();

    public GroupByCommit(List<Commit> commits, List<Run> runs) {
        putCommits(commits);
        putRuns(runs);
    }

    private void putCommits(List<Commit> commits) {
        for (Commit commit : commits) {
            commitRuns.put(commit.getId(), new ArrayList<Run>());
        }
    }

    private void putRuns(List<Run> runs) {
        for (Run run : runs) {
            putRun(run);
        }
    }

    private void putRun(Run run) {
        List<Run> runs = commitRuns.get(run.getCommitId());

        if (runs != null) {
            runs.add(run);
        }
    }

    public Map<String, List<Run>> get() {
        return commitRuns;
    }
}
