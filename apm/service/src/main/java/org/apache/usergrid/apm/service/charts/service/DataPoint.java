package org.apache.usergrid.apm.service.charts.service;

import java.util.Date;

public interface DataPoint {
    public Date getTimestamp();
    
    public void setTimestamp(Date timestamp);
}
