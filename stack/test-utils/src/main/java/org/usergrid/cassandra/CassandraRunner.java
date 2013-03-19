package org.usergrid.cassandra;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.thrift.CassandraDaemon;
import org.apache.commons.io.FileUtils;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CassandraRunner extends BlockJUnit4ClassRunner {

    private static final String TMP = "tmp";
    private static Logger logger = LoggerFactory.getLogger(CassandraRunner.class);
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static ContextHolder contextHolder;
    private SchemaManager schemaManager;

    // TODO maybe put a 'holder' object in the executor and provide access to application context thusly?
    // TODO assume a static 'usergrid-test-context.xml' primer/starter file

    public CassandraRunner(Class<?> klass) throws InitializationError {
        super(klass);
        logger.info("CassandraRunner started with class {}", klass.getName());
    }


    public static <T> T getBean(String name, Class<T> requiredType) {
        return contextHolder.applicationContext.getBean(name, requiredType);
    }

    public static <T> T getBean(Class<T> requiredType) {
        return contextHolder.applicationContext.getBean(requiredType);
    }

    /**
     * Class-level run. The order of events are as follows:
     * - load spring (if not loaded)
     * - init cassandra (if not started)
     * - look for DataControl annotation on class and delegate
     * @param notifier
     */
    @Override
    public void run(RunNotifier notifier) {
        DataControl control = preTest(notifier);
        super.run(notifier);
        postTest(notifier, control);
       
    }
    
    protected DataControl preTest(RunNotifier notifier){
      maybeInit();

      // check for SchemaManager annotation
      DataControl control = null;
      for(Annotation ann : getTestClass().getAnnotations() ) {
          logger.info("examining annotation " + ann);
          if ( ann instanceof DataControl ) {
              logger.info("founda dataCOntrol annotation");
              control = (DataControl)ann;
          }
      }
      loadDataControl(control);
      maybeCreateSchema();
      
      return control;
    }
    
    protected void postTest(RunNotifier notifier, DataControl control){
      if ( control == null || !control.skipTruncate() ) {
        schemaManager.destroy();
    }
    logger.info("shutting down context");
    contextHolder.applicationContext.stop();
      
    }

   
    private void loadDataControl(DataControl dataControl) {
        if ( !contextHolder.applicationContext.isActive() ) {
            logger.info("restarting context...");
            contextHolder.applicationContext.refresh();
        }

        if ( dataControl != null ) {
            // TODO check for classpath and go static?
            logger.info("dataControl found - looking upma SchemaManager impl");
            schemaManager = getBean(dataControl.schemaManager(), SchemaManager.class);
        } else {
            logger.info("dataControl not found - using default SchemaManager impl");
            schemaManager = getBean(SchemaManager.class);
        }


    }

    private boolean maybeCreateSchema() {
        if ( schemaManager.exists() ) {
            logger.info("Schema existed");
            return false;
        }
        schemaManager.create();
        schemaManager.populateBaseData();

        return true;
    }



    private static void maybeInit() {
        if ( contextHolder != null ) {
            return;
        }
        logger.info("Initing CassandraRunner...");

        // TODO these should all be passed in as config options ... via spring?
        // TODO make sure these match up with configs
        // TODO one cassandra config in this project!
        // TODO log4j.properties required to be present

        System.setProperty("cassandra-foreground", "true");
        System.setProperty("log4j.defaultInitOverride","true");
        System.setProperty("log4j.configuration", "log4j.properties");
        //System.setProperty("cassandra.load_ring_state", "false");so
        //System.setProperty("cassandra.join_ring","false");
        System.setProperty("cassandra.ring_delay_ms","100");

        FileUtils.deleteQuietly(new File(TMP));

        contextHolder = new ContextHolder();
        try {
            executor.schedule(contextHolder, 3, TimeUnit.SECONDS).get();
        } catch (Exception ex) {
            logger.error("Could not schedule cassandra runner",ex);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    logger.error("In shutdownHook");
                    stopCassandra();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private static void stopCassandra() throws Exception {
        if (contextHolder != null && contextHolder.cassandraDaemon != null) {
            contextHolder.cassandraDaemon.deactivate();
        }
        executor.shutdown();
        executor.shutdownNow();
    }

    static class ContextHolder implements Runnable {
        ConfigurableApplicationContext applicationContext;
        CassandraDaemon cassandraDaemon;

        @Override
        public void run() {
            cassandraDaemon = new CassandraDaemon();
            cassandraDaemon.activate();
            String[] locations = {"usergrid-test-context.xml"};
            applicationContext = new ClassPathXmlApplicationContext(locations);



                    //acbf.autowire(getTestClass().getJavaClass(),AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
        }
    }
}