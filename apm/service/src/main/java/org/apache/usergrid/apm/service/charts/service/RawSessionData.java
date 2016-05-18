package org.apache.usergrid.apm.service.charts.service;

public class RawSessionData extends RawData
{

   
   private Long numSessions;


   private double percentageOfTotal;

   public Long getNumSessions()
   {
      return numSessions;
   }

   public void setNumSessions(Long numSessions)
   {
      this.numSessions = numSessions;
   }

   public double getPercentageOfTotal()
   {
      return percentageOfTotal;
   }

   public void setPercentageOfTotal(double percentageOfTotal)
   {
      this.percentageOfTotal = percentageOfTotal;  
   }



}
