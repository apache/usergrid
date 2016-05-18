package org.apache.usergrid.apm.service.charts.filter;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.ideawheel.portal.model.ChartCriteria;
import com.ideawheel.portal.model.ChartCriteria.PeriodType;
import org.apache.usergrid.apm.service.DeploymentConfig;

public class SpecialTimeFilter implements SimpleHibernateFilter
{
   private static final Log log = LogFactory.getLog(SpecialTimeFilter.class);

   String startPropName;
   Long from;

   String endPropName;
   Long to;
   
   

   public SpecialTimeFilter(ChartCriteria cq)
   {
      //both switch statements can be merged but the code has evolved like this so don't want to muck around more at this time
      if (cq.getPeriodType() == PeriodType.LAST_X) {
         Calendar start = Calendar.getInstance();
         Calendar end = Calendar.getInstance();

         switch (cq.getLastX())  {
         case LAST_HOUR:
            startPropName = "startMinute";
            endPropName = "endMinute";
            start.add(Calendar.MINUTE, -60);
            //Allowing some buffer so that we give enough time for data to be processed.
            end.add (Calendar.MINUTE, -1 * DeploymentConfig.geDeploymentConfig().getTimeBufferForChartData());
            from = start.getTimeInMillis()/1000/60;         
            to = end.getTimeInMillis()/1000/60;
            break;
         case LAST_3HOUR:
             startPropName = "startMinute";
             endPropName = "endMinute";
             start.add(Calendar.MINUTE, -60*3);
             //Allowing some time buffer so that we give enough time for data to be processed.
             end.add (Calendar.MINUTE, -1 * DeploymentConfig.geDeploymentConfig().getTimeBufferForChartData());
             from = start.getTimeInMillis()/1000/60;         
             to = end.getTimeInMillis()/1000/60;
             break;
         case LAST_6HOUR:
             startPropName = "startMinute";
             endPropName = "endMinute";
             start.add(Calendar.MINUTE, -60*6);
             //Allowing some time buffer so that we give enough time for data to be processed.
             end.add (Calendar.MINUTE, -1 * DeploymentConfig.geDeploymentConfig().getTimeBufferForChartData());
             from = start.getTimeInMillis()/1000/60;         
             to = end.getTimeInMillis()/1000/60;
             break;
         case LAST_12HOUR:
             startPropName = "startMinute";
             endPropName = "endMinute";
             start.add(Calendar.MINUTE, -60*12);
             //Allowing some buffer so that we give enough time for data to be processed.
             end.add (Calendar.MINUTE, -1 * DeploymentConfig.geDeploymentConfig().getTimeBufferForChartData());
             from = start.getTimeInMillis()/1000/60;         
             to = end.getTimeInMillis()/1000/60;
             break;             
         case LAST_DAY:
            startPropName = "startHour";
            endPropName = "endHour";
            start.add(Calendar.DATE, -1);
            from = start.getTimeInMillis()/1000/60/60;
            to = end.getTimeInMillis()/1000/60/60;
            break;
         case LAST_WEEK:
            startPropName = "startHour"; //changing it to startHour from startDay because it only gives 7 data points otherwise.
            endPropName = "endHour";
            start.add(Calendar.DATE, -7);
            from = start.getTimeInMillis()/1000/60/60;
            to = end.getTimeInMillis()/1000/60/60;
            break;
         case LAST_MONTH:
            startPropName = "startDay";
            endPropName = "endDay";
            start.add(Calendar.MONTH, -1);
            from = start.getTimeInMillis()/1000/60/60/24;
            to = end.getTimeInMillis()/1000/60/60/24;
            break;
         case LAST_YEAR:
            startPropName = "startMonth";
            endPropName = "endMonth";
            start.add(Calendar.YEAR, -1);
            from = start.getTimeInMillis()/1000/60/60/24/30; //Approximately
            to = end.getTimeInMillis()/1000/60/60/24/30;
            break;
         default:
            log.error("Invalid criteria for Speicial Time Filter");

         }
      }
      else {
         switch (cq.getSamplePeriod())  {
         case MINUTE:
            startPropName = "startMinute";
            endPropName = "endMinute";
            from = cq.getStartDate().getTime()/1000/60;        
            to = cq.getEndDate().getTime()/1000/60;
            break;
         case HOUR:
            startPropName = "startHour";
            endPropName = "endHour";
            from = cq.getStartDate().getTime()/1000/60/60;
            to = cq.getEndDate().getTime()/1000/60/60;
            break;
         case DAY_WEEK:
            startPropName = "startDay";
            endPropName = "endDay";
            from = cq.getStartDate().getTime()/1000/60/60/24;
            to =   cq.getEndDate().getTime()/1000/60/60/24;
            break;
         case DAY_MONTH:
            startPropName = "startDay";
            endPropName = "endDay";
            from = cq.getStartDate().getTime()/1000/60/60/24/7;
            to =   cq.getEndDate().getTime()/1000/60/60/24/7;
            break;
         case MONTH:
            startPropName = "startMonth";
            endPropName = "endMonth";
            from = cq.getStartDate().getTime()/1000/60/60/24/30; //Approximately
            to = cq.getEndDate().getTime()/1000/60/60/24/30;
            break;
         default:
            log.error("end time marker is not valid for Time Range Filter");
         }
      }

   }

   @Override
   public boolean getFilterEmpty()
   {
      return from == null && to == null;
   }

   /**
    * Active session for a time range is one with (start <= endTime AND end > startTime)
    */
   @Override
   public Criterion getCriteria()
   {
      if (getFilterEmpty()) 
         return null;
      //see http://stackoverflow.com/questions/84644/hibernate-query-by-example-and-projections on why this. is needed


      return Restrictions.and(Restrictions.le("this."+startPropName, to),Restrictions.gt("this."+endPropName, from));

   }

   public Long getFrom()
   {
      return from;
   }

   public Long getTo()
   {
      return to;
   }

   public String getStartPropName()
   {
      return startPropName;
   }


   public String getEndPropName()
   {
      return endPropName;
   }

   @Override
   public void setPropertyName(String propertyName)
   {
      throw new UnsupportedOperationException (); 

   }

   @Override
   public String getPropertyName()
   {
      throw new UnsupportedOperationException ();
   }

   @Override
   public Object getPropertyValue()
   {
      throw new UnsupportedOperationException ();
   }

}
