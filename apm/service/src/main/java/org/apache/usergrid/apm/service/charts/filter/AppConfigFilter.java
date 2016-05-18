package org.apache.usergrid.apm.service.charts.filter;

public class AppConfigFilter extends EqualFilter
{

   public AppConfigFilter (String appConfigType) {
      propertyName = "appConfigType";
      propertyValue = appConfigType;    
   }
}
