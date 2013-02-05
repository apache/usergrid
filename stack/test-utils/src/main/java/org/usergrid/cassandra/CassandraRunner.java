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
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CassandraRunner extends BlockJUnit4ClassRunner {

    private static final String TMP = "tmp";
    private static Logger logger = LoggerFactory.getLogger(CassandraRunner.class);
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static ContextHolder contextHolder;

    // TODO maybe put a 'holder' object in the executor and provide access to application context thusly?
    // TODO assume a static 'usergrid-test-context.xml' primer/starter file

    public CassandraRunner(Class<?> klass) throws InitializationError {
        super(klass);
        logger.info("CassandraRunner started with class {}", klass.getName());
    }


    public static <T> T getBean(String name, Class<T> requiredType) {
        return contextHolder.applicationContext.getBean(name, requiredType);
    }
    //TODO get by class type

    /**
     * Class-level run. The order of events are as follows:
     * - start IntravertDeamon if not started already
     * - create any keyspaces defined as class level annotations
     * - create any column families defined as class level annotations
     * @param notifier
     */
    @Override
    public void run(RunNotifier notifier) {
        maybeInit();
        AutowireCapableBeanFactory acbf = contextHolder.applicationContext.getAutowireCapableBeanFactory();
        acbf.autowire(getTestClass().getJavaClass(),AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
        super.run(notifier);
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        // TODO should scan for:
        // - DataControl: dropSchemaOnExit=true, dataLoader=[a class which implements load()]
        // dataControl.loadMyData(cassandraService)
        super.runChild(method, notifier);
    }

    private void maybeCreateSchema() {

        // TODO check for schema -
        // Setup.setup() if not present
        // - schema creation, baseline data population, test data population
        // - SchemaManager iface
        // - DataLoader iface
        // 1. SchemaManager.loadSchema()
        // 2. SchemaManager.populateBaseData()
        // 3. DataLoader.execute()
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
        System.setProperty("cassandra.load_ring_state", "false");
        System.setProperty("cassandra.join_ring","false");
        // System.setProperty("cassandra.ring_delay_ms","100");

        FileUtils.deleteQuietly(new File(TMP));

        contextHolder = new ContextHolder();
        try {
            executor.schedule(contextHolder ,3, TimeUnit.SECONDS).get();
        } catch (Exception ex) {
            logger.error("Could not schedule cassandra runner");
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
        if (contextHolder != null) {
            contextHolder.cassandraDaemon.deactivate();
        }
        executor.shutdown();
        executor.shutdownNow();
    }

    static class ContextHolder implements Runnable {
        final ApplicationContext applicationContext;
        CassandraDaemon cassandraDaemon;

        ContextHolder() {
            String[] locations = {"usergrid-test-context.xml"};
            applicationContext = new ClassPathXmlApplicationContext(locations);

        }
        @Override
        public void run() {
            cassandraDaemon = new CassandraDaemon();
            cassandraDaemon.activate();
        }
    }
}