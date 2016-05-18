package org.apache.usergrid.apm.service.charts.filter;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.ideawheel.portal.model.ChartCriteria;


public class EndPeriodFilter implements SimpleHibernateFilter {

   private static final Log log = LogFactory.getLog(EndPeriodFilter.class);
   private String propertyName;
   private Long from;
   private Long to;


   public EndPeriodFilter(ChartCriteria cq) {
      //both switch statements can be merged but the code has evolved like this so don't want to muck around more at this time
      if (cq.getLastX() != null) {
         Calendar start = Calendar.getInstance();
         Calendar end = Calendar.getInstance();

         switch (cq.getLastX())  {
         case LAST_HOUR:
            propertyName = "endMinute";
            start.add(Calendar.MINUTE, -60);         
            from = start.getTimeInMillis()/1000/60;         
            to = end.getTimeInMillis()/1000/60;
            break;
         case LAST_DAY:
            propertyName = "endHour";
            start.add(Calendar.DATE, -1);
            from = start.getTimeInMillis()/1000/60/60;
            to = end.getTimeInMillis()/1000/60/60;
            break;
         case LAST_WEEK:
            propertyName = "endDay";
            start.add(Calendar.DATE, -7);
            from = start.getTimeInMillis()/1000/60/60/24;
            to = end.getTimeInMillis()/1000/60/60/24;
            break;
         case LAST_MONTH:
            propertyName = "endDay";
            start.add(Calendar.MONTH, -1);
            from = start.getTimeInMillis()/1000/60/60/24;
            to = end.getTimeInMillis()/1000/60/60/24;
            break;
         case LAST_YEAR:
            propertyName = "endMonth";
            start.add(Calendar.YEAR, -1);
            from = start.getTimeInMillis()/1000/60/60/24/30; //Approximately
            to = end.getTimeInMillis()/1000/60/60/24/30;
            break;
         default:
            log.error("time marker is not valid for End Period Filter");

         }
      }
      else {
         switch (cq.getSamplePeriod())  {
         case MINUTE:
            propertyName = "endMinute";
            from = cq.getStartDate().getTime()/1000/60;        
            to = cq.getEndDate().getTime()/1000/60;
            break;
         case HOUR:
            propertyName = "endHour";
            from = cq.getStartDate().getTime()/1000/60/60;
            to = cq.getEndDate().getTime()/1000/60/60;
            break;
         case DAY_WEEK:
            propertyName = "endDay";
            from = cq.getStartDate().getTime()/1000/60/60/24;
            to =   cq.getEndDate().getTime()/1000/60/60/24;
            break;
         case DAY_MONTH:
            propertyName = "endDay";
            from = cq.getStartDate().getTime()/1000/60/60/24/7;
            to =   cq.getEndDate().getTime()/1000/60/60/24/7;
            break;
         case MONTH:
            propertyName = "endMonth";
            from = cq.getStartDate().getTime()/1000/60/60/24/30; //Approximately
            to = cq.getEndDate().getTime()/1000/60/60/24/30;
            break;
         default:
            log.error("time marker is not valid for end period Filter");
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
