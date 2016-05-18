package org.apache.usergrid.apm.model;

import java.io.Serializable;


/**
 * 
 * @author prabhat
 *
 */

public class AppConfigOverrideFilter implements Serializable{

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

  
   private String filterValue;

  
   protected String filterType;

   public AppConfigOverrideFilter () {

   }


   public AppConfigOverrideFilter (String filterValue, String filterType) {
      this.filterValue = filterValue;
      this.filterType = filterType;
      
   }   

   public String getFilterValue()
   {
      return filterValue;
   }

   public void setFilterValue(String filterValue)
   {
      this.filterValue = filterValue;
   }

   
   public String getFilterType() {
      return filterType;
   }

   public void setFilterType(String filterType) {
      this.filterType = filterType;
   }

   @Override
   public String toString() {
      return "Filter for " + filterType+ " is " + filterValue;
   }

}

