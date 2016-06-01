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
package org.apache.usergrid.apm.model;


import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.Table;


@Entity
@Table(name = "SESSION_CHART_CRITERIA")
@org.hibernate.annotations.Table(appliesTo="SESSION_CHART_CRITERIA",
indexes = {
@Index(name="SessionChartCriteriaByApp", columnNames={"appId"} ) 
} )
public class SessionChartCriteria extends ChartCriteria
{


   boolean showSessionCount;   
   boolean showUserCount;   
   boolean showSessionTime;
   boolean showAll;
   public boolean isShowSessionCount()
   {
      return showSessionCount;
   }
   public void setShowSessionCount(boolean showSessionCount)
   {
      this.showSessionCount = showSessionCount;
   }
   public boolean isShowUsers()
   {
      return showUserCount;
   }
   public void setShowUsers(boolean showUsers)
   {
      this.showUserCount = showUsers;
   }
   public boolean isShowSessionTime()
   {
      return showSessionTime;
   }
   public void setShowSessionTime(boolean showSessionTime)
   {
      this.showSessionTime = showSessionTime;
   }
   public boolean isShowAll()
   {
      return showAll;
   }
   public void setShowAll(boolean showAll)
   {
      if(showAll) {
         this.showSessionCount = true;
         this.showSessionTime = true;
         this.showUserCount = true;
      }    

      this.showAll = showAll;
   }




}
