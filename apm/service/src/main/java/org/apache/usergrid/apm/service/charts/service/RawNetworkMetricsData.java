package org.apache.usergrid.apm.service.charts.service;

public class RawNetworkMetricsData extends RawData
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private String url;
   
   private Long numRequests;
   
   private Long avgLatency;
   
   private Long errorCount;
   
   private String devicePlatform;
   
   private String networkCarrier;
   
   private String networkType;

   public String getUrl()
   {
      return url;
   }

   public void setUrl(String url)
   {
      this.url = url;
   }

   public Long getNumRequests()
   {
      return numRequests;
   }

   public void setNumRequests(Long numRequests)
   {
      this.numRequests = numRequests;
   }

   public Long getAvgLatency()
   {
      return avgLatency;
   }

   public void setAvgLatency(Long avgLatency)
   {
      this.avgLatency = avgLatency;
   }

   public Long getErrorCount()
   {
      return errorCount;
   }

   public void setErrorCount(Long errorCount)
   {
      this.errorCount = errorCount;
   }

   public String getDevicePlatform()
   {
      return devicePlatform;
   }

   public void setDevicePlatform(String devicePlatform)
   {
      this.devicePlatform = devicePlatform;
   }

   public String getNetworkCarrier()
   {
      return networkCarrier;
   }

   public void setNetworkCarrier(String networkCarrier)
   {
      this.networkCarrier = networkCarrier;
   }

   public String getNetworkType()
   {
      return networkType;
   }

   public void setNetworkType(String networkType)
   {
      this.networkType = networkType;
   }   

}
