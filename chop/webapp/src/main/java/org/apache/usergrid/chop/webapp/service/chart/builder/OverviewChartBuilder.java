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
package org.apache.usergrid.chop.webapp.service.chart.builder;

import com.google.inject.Inject;
import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.webapp.dao.CommitDao;
import org.apache.usergrid.chop.webapp.dao.RunDao;
import org.apache.usergrid.chop.webapp.service.chart.Chart;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.builder.average.OverviewAverage;
import org.apache.usergrid.chop.webapp.service.chart.filter.FailureFilter;
import org.apache.usergrid.chop.webapp.service.chart.filter.PercentileFilter;
import org.apache.usergrid.chop.webapp.service.chart.group.GroupByCommit;
import org.apache.usergrid.chop.webapp.service.chart.group.GroupByRunNumber;
import org.apache.usergrid.chop.webapp.service.chart.series.Series;
import org.apache.usergrid.chop.webapp.service.chart.series.SeriesBuilder;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OverviewChartBuilder extends ChartBuilder {

    private CommitDao commitDao;
    private RunDao runDao;

    @Inject
    public OverviewChartBuilder(CommitDao commitDao, RunDao runDao) {
        this.commitDao = commitDao;
        this.runDao = runDao;
    }

    public Chart getChart(Params params) {
        LOG.info(params.toString());

        List<Commit> commits = commitDao.getByModule(params.getModuleId());
        List<Run> runs = runDao.getList(commits, params.getTestName());

        Map<String, List<Run>> commitRuns = new GroupByCommit(commits, runs).get();
        Map<String, Collection<Value>> groupedByRunNumber = groupByRunNumber(commitRuns, params.getMetric());

        Map<String, Collection<Value>> resultMap = PercentileFilter.filter(groupedByRunNumber, params.getPercentile());
        resultMap = FailureFilter.filter(resultMap, params.getFailureType());

        List<Series> series = SeriesBuilder.toSeries(resultMap);
        series.add(new Series("Average", SeriesBuilder.toPoints(OverviewAverage.calc(resultMap), 0)));

        return new Chart(series, resultMap.keySet());
    }

    private static Map<String, Collection<Value>> groupByRunNumber(Map<String, List<Run>> commitRuns, Params.Metric metric) {

        Map<String, Collection<Value>> grouped = new LinkedHashMap<String, Collection<Value>>();

        for (String commitId : commitRuns.keySet()) {
            List<Run> runs = commitRuns.get(commitId);
            grouped.put(commitId, new GroupByRunNumber(runs, metric).get());
        }

        return grouped;
    }

}
