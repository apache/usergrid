package org.usergrid.cassandra;

import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.thrift.CassandraDaemon;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CassandraRunner extends BlockJUnit4ClassRunner {

    private static final String TMP = "tmp";
    private static CassandraDaemon cassandraDaemon;
    private static Logger logger = LoggerFactory.getLogger(CassandraRunner.class);
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    public CassandraRunner(Class<?> klass) throws InitializationError {
        super(klass);
        logger.info("CassandraRunner started with class {}", klass.getName());
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {

        super.runChild(method, notifier);
    }

    /**
     * The order of events are as follows:
     * - start IntravertDeamon if not started already
     * - create any keyspaces defined as class level annotations
     * - create any column families defined as class level annotations
     * @param notifier
     */
    @Override
    public void run(RunNotifier notifier) {

        super.run(notifier);
    }

    private void maybeCreateSchema() {

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

    private void startCassandra() {
        if ( cassandraDaemon != null ) {
            return;
        }
        deleteRecursive(new File("tmp/cache"));
        deleteRecursive(new File ("tmp/data"));
        deleteRecursive(new File ("tmp/log"));
        System.setProperty("cassandra-foreground", "true");
        System.setProperty("log4j.defaultInitOverride","true");
        System.setProperty("log4j.configuration", "log4j.properties");
        System.setProperty("cassandra.ring_delay_ms","1000");
        //System.setProperty("cassandra.start_rpc","false");
        //System.setProperty("cassandra.start_native_transport","false");

        executor.execute(new Runnable() {
            public void run() {
                cassandraDaemon = new CassandraDaemon();
                cassandraDaemon.activate();
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

    private void stopCassandra() throws Exception {
        if (cassandraDaemon != null) {
            cassandraDaemon.deactivate();
            StorageService.instance.stopClient();

        }
        executor.shutdown();
        executor.shutdownNow();
    }

}