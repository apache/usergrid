package org.apache.usergrid.apm.service.charts.service;

import java.util.List;

public interface ChartDTO<T extends DataPoint> {
    public String getChartGroupName();

    public void setChartGroupName(String chartGroupName);

    public List<T> getDatapoints();

    public void setDatapoints(List<T> datapoints);

    public void addDataPoint (T dataPoint);
}
