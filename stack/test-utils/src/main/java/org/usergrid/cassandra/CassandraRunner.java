package org.usergrid.cassandra;

import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.thrift.CassandraDaemon;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.Resource;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CassandraRunner extends BlockJUnit4ClassRunner {

    private static final String TMP = "tmp";
    private static Logger logger = LoggerFactory.getLogger(CassandraRunner.class);
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
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
        // - DataControl: truncateOnExit=true, dataLoader=[a class which implements load()]
        super.runChild(method, notifier);
    }

    private void maybeCreateSchema() {

        // TODO check for schema -
        // Setup.setup() if not present
    }

    private static boolean deleteRecursive(File path) {
        if (!path.exists())
            return false;
        boolean ret = true;
        if (path.isDirectory()){
            for (File f : path.listFiles()){
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

    private static void loadSpringContext() {
        // TODO check for appContext, create if not present
        String[] locations = {"usergrid-test-context.xml"};
        contextHolder.applicationContext = new ClassPathXmlApplicationContext(locations);
    }

    private static void maybeInit() {
        if ( contextHolder != null ) {
            return;
        }
        logger.info("Initing...");
        contextHolder = new ContextHolder();
        loadSpringContext();
        // TODO make sure these match up with configs
        // TODO one cassandra config in this project!
        // TODO log4j.properties required to be present
        deleteRecursive(new File("tmp/saved_caches"));
        deleteRecursive(new File ("tmp/data"));
        deleteRecursive(new File ("tmp/commitlog"));
        System.setProperty("cassandra-foreground", "true");
        System.setProperty("log4j.defaultInitOverride","true");
        System.setProperty("log4j.configuration", "log4j.properties");
        System.setProperty("cassandra.ring_delay_ms","100");
        //System.setProperty("cassandra.start_rpc","false");
        //System.setProperty("cassandra.start_native_transport","false");

        executor.execute(new Runnable() {
            public void run() {
                contextHolder.cassandraDaemon = new CassandraDaemon();
                contextHolder.cassandraDaemon.activate();
            }
        });
        try {
            TimeUnit.SECONDS.sleep(3);
        }
        catch (InterruptedException e) {
            throw new AssertionError(e);
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
            StorageService.instance.stopClient();

        }
        executor.shutdown();
        executor.shutdownNow();
    }

    static class ContextHolder {
        ApplicationContext applicationContext;
        CassandraDaemon cassandraDaemon;
    }
}