package org.apache.usergrid.persistence.collection.hystrix;


/**
 *
 *
 */
public class CommandUtils {

    /**
     * Get the name of the archiaus property for the core thread pool size
     * @param threadPoolName
     * @return
     */
    public static String getThreadPoolCoreSize(String threadPoolName){
        return "hystrix.threadpool."+ threadPoolName + ".coreSize";
    }

    /**
       * Get the name of the archiaus property for the max thread pool size
       * @param threadPoolName
       * @return
       */
      public static String getThreadPoolMaxQueueSize(String threadPoolName){
          return "hystrix.threadpool."+ threadPoolName + ".maxQueueSize";
      }

}
