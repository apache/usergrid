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
package org.apache.usergrid.apm.service.charts.filter;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import org.apache.usergrid.apm.model.ChartCriteria;


public class StartPeriodFilter implements SimpleHibernateFilter {

   private static final Log log = LogFactory.getLog(StartPeriodFilter.class);
   private String propertyName;
   private Long from;
   private Long to;


   public StartPeriodFilter(ChartCriteria cq) {
      //both switch statements can be merged but the code has evolved like this so don't want to muck around more at this time
      if (cq.getLastX() != null) {
         Calendar start = Calendar.getInstance();
         Calendar end = Calendar.getInstance();

         switch (cq.getLastX())  {
         case LAST_HOUR:
            propertyName = "startMinute";
            start.add(Calendar.MINUTE, -60);
            //Allowing 3 min buffer so that we give enough time for data to be processed.
            end.add (Calendar.MINUTE, -3);
            from = start.getTimeInMillis()/1000/60;         
            to = end.getTimeInMillis()/1000/60;
            break;
         case LAST_DAY:
            propertyName = "startHour";
            start.add(Calendar.DATE, -1);
            from = start.getTimeInMillis()/1000/60/60;
            to = end.getTimeInMillis()/1000/60/60;
            break;
         case LAST_WEEK:
            propertyName = "startDay";
            start.add(Calendar.DATE, -7);
            from = start.getTimeInMillis()/1000/60/60/24;
            to = end.getTimeInMillis()/1000/60/60/24;
            break;
         case LAST_MONTH:
            propertyName = "startDay";
            start.add(Calendar.MONTH, -1);
            from = start.getTimeInMillis()/1000/60/60/24;
            to = end.getTimeInMillis()/1000/60/60/24;
            break;
         case LAST_YEAR:
            propertyName = "startMonth";
            start.add(Calendar.YEAR, -1);
            from = start.getTimeInMillis()/1000/60/60/24/30; //Approximately
            to = end.getTimeInMillis()/1000/60/60/24/30;
            break;
         default:
            log.error("time marker is not valid for Start Period Filter");

         }
      }
      else {
         switch (cq.getSamplePeriod())  {
         case MINUTE:
            propertyName = "startMinute";
            from = cq.getStartDate().getTime()/1000/60;        
            to = cq.getEndDate().getTime()/1000/60;
            break;
         case HOUR:
            propertyName = "startHour";
            from = cq.getStartDate().getTime()/1000/60/60;
            to = cq.getEndDate().getTime()/1000/60/60;
            break;
         case DAY_WEEK:
            propertyName = "startDay";
            from = cq.getStartDate().getTime()/1000/60/60/24;
            to =   cq.getEndDate().getTime()/1000/60/60/24;
            break;
         case DAY_MONTH:
            propertyName = "startWeek";
            from = cq.getStartDate().getTime()/1000/60/60/24/7;
            to =   cq.getEndDate().getTime()/1000/60/60/24/7;
            break;
         case MONTH:
            propertyName = "startMonth";
            from = cq.getStartDate().getTime()/1000/60/60/24/30; //Approximately
            to = cq.getEndDate().getTime()/1000/60/60/24/30;
            break;
         default:
            log.error("time marker is not valid for start period Filter");
         }
      }
   }
      @Override
   public Criterion getCriteria() {

      if (getFilterEmpty()) 
         return null;
      //see http://stackoverflow.com/questions/84644/hibernate-query-by-example-and-projections on why this. is needed
      //log.info("Time Range Filter Criteria between " + from + " & " + to);

      return Restrictions.and(Restrictions.gt("this."+propertyName, from), 
            Restrictions.le("this."+propertyName, to));

      //return Restrictions.between("this."+propertyName, from, to);    
   }

   @Override
   public boolean getFilterEmpty() {
      return from == null || to == null;
   }

   @Override
   public String getPropertyName() {
      return propertyName;
   }

   @Override
   public Object getPropertyValue() {
      return propertyName;
   }

   @Override
   public void setPropertyName(String propertyName) {
      this.propertyName = propertyName;

   }

   public Long getFrom() {
      return from;
   }

   public void setFrom(Long from) {
      this.from = from;
   }

   public Long getTo() {
      return to;
   }

   public void setTo(Long to) {
      this.to = to;
   }

}
