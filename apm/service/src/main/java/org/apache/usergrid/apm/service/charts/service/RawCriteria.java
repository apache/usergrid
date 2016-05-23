package org.apache.usergrid.apm.service.charts.service;

import org.apache.usergrid.apm.model.ChartCriteria;

public class RawCriteria<T extends ChartCriteria>
{

   T chartCriteria;
   int startRow;
   int rowCount;

   public RawCriteria (T cq) {
      this.chartCriteria = cq;
   }

   public T getChartCriteria()
   {
      return chartCriteria;
   }

   public void setChartCriteria(T cq)
   {
      this.chartCriteria = cq;
   }

   public int getStartRow()
   {
      return startRow;
   }
   public void setStartRow(int startRow)
   {
      this.startRow = startRow;
   }
   public int getRowCount()
   {
      return rowCount;
   }
   public void setRowCount(int rowCount)
   {
      this.rowCount = rowCount;
   }

}
