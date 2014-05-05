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
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.webapp.dao.RunDao;
import org.apache.usergrid.chop.webapp.service.chart.Chart;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.filter.FailureFilter;
import org.apache.usergrid.chop.webapp.service.chart.filter.PercentileFilter;
import org.apache.usergrid.chop.webapp.service.chart.group.GroupByRunNumber;
import org.apache.usergrid.chop.webapp.service.chart.series.Series;
import org.apache.usergrid.chop.webapp.service.chart.series.SeriesBuilder;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RunsChartBuilder extends ChartBuilder {

    private RunDao runDao;

    @Inject
    public RunsChartBuilder(RunDao runDao) {
        this.runDao = runDao;
    }

    public Chart getChart(Params params) {

        List<Run> runs = runDao.getList(params.getCommitId(), params.getTestName());

        Collection<Value> groupedRuns = new GroupByRunNumber(runs, params.getMetric()).get();

        Collection<Value> filteredValues = PercentileFilter.filter(groupedRuns, params.getPercentile());
        filteredValues = FailureFilter.filter(filteredValues, params.getFailureType());

        ArrayList<Series> series = new ArrayList<Series>();
        series.add(new Series(SeriesBuilder.toPoints(filteredValues, 1)));

        return new Chart(series);
    }
}
