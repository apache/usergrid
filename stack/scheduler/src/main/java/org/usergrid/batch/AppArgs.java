package org.usergrid.batch;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * @author zznate
 */
public class AppArgs {

  @Parameter(names = "-host", description = "The Cassandra host to which we will connect")
  private String host = "127.0.0.1";

  @Parameter(names = "-port", description="The port which we will connect")
  private int port = 9160;

  @Parameter(names = "-workerThreads", description = "The number of worker threads")
  private int workerThreads = 4;

  @Parameter(names = "-sleepFor", description = "Number of seconds to sleep between checks of the work queue")
  private int sleepFor = 2;

  @Parameter(names = "-appContext", description = "Location of Spring Application context files")
  private String appContext;


  public static AppArgs parseArgs(String[] args) {
    AppArgs appArgs = new AppArgs();
    JCommander jcommander = new JCommander(appArgs, args);
    return appArgs;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public int getWorkerThreads() {
    return workerThreads;
  }

  public int getSleepFor() {
    return sleepFor;
  }

  public String getAppContext() {
    return appContext;
  }
}
