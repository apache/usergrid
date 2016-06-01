/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
