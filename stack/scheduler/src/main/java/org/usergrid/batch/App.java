package org.usergrid.batch;

import com.google.common.base.CharMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.usergrid.batch.service.BulkJobScheduledService;

/**
 * Entry point for CLI functions of Usergrid batch framework
 *
 * To run this with the built-in examples, invoke it thusly from the top level of
 * the project directory:
 *
 * mvn -e exec:java -Dexec.mainClass="org.usergrid.batch.App" -Dexec.args="-appContext src/test/resources/appContext.xml"
 *
 * @author zznate
 */
public class App {

  private static Logger logger = LoggerFactory.getLogger(App.class);

  private ApplicationContext appContext;
  private final AppArgs appArgs;

  public static void main( String[] args ) {
    AppArgs appArgs = AppArgs.parseArgs(args);
    if (logger.isDebugEnabled()) {
      logger.debug("Invoked App with appArgs: {}", appArgs.toString());
    }

    App app = new App(appArgs);

    app.loadContext();

    logger.info("Context loaded, invoking execute() ...");
    app.doExecute();
  }

  App(AppArgs appArgs) {
    this.appArgs = appArgs;
  }

  private void loadContext() {
    logger.info("loading context");
    // spring context
    int index = CharMatcher.is(':').indexIn(appArgs.getAppContext());
    if ( index > 0 ) {
      appContext = new ClassPathXmlApplicationContext(appArgs.getAppContext().substring(++index));
    } else {
      appContext = new FileSystemXmlApplicationContext(appArgs.getAppContext());
    }
  }

  private void doExecute() {
    BulkJobScheduledService bjss = appContext.getBean("bulkJobScheduledService",BulkJobScheduledService.class);
    logger.info("starting scheduledService...");
    bjss.startAndWait();
    logger.info("scheduledService started.");
  }




}
