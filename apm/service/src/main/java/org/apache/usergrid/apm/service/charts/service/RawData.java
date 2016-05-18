package org.apache.usergrid.apm.service.charts.service;

import java.io.Serializable;
import java.util.Date;

public abstract class RawData implements Serializable, Comparable<RawData> { 

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   Date timeStamp;

   public void setTimeStamp (Date timeStamp) {
      this.timeStamp = timeStamp;
   }
   public Date getTimeStamp () {
      return timeStamp;
   }

   public int compareTo(RawData o) {
      if (this.timeStamp.before(o.getTimeStamp()))
         return 1; //descending order by timestamp
      else if (this.timeStamp.after(o.getTimeStamp()))
         return -1;
      else return 0;

   }

}
