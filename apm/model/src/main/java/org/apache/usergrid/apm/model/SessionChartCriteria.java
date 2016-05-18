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
