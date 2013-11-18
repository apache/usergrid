/*******************************************************************************
 * Copyright 2013 baas.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.query.validator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.cassandra.DataControl;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.Entity;
import org.usergrid.standalone.Server;
import org.usergrid.utils.JsonUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Sung-ju Jin(realbeast)
 */
public class QueryValidatorRunner extends CassandraRunner {

    private static Logger logger = LoggerFactory.getLogger(QueryValidatorRunner.class);
    private boolean enableLocalServer;
    private static StandaloneServer standaloneServer;
    private Properties properties;
    private static QueryValidator validator;
    private static boolean initialize = false;

    public QueryValidatorRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected DataControl preTest(RunNotifier notifier) {
        DataControl control = super.preTest(notifier);
        try {
            setup();
        } catch (InitializationError initializationError) {
            initializationError.printStackTrace();
        }
        return control;
    }

    public static QueryValidator getValidator() {
        return validator;
    }

    public synchronized void setup() throws InitializationError {
        if(initialize)
            return;

        try {
            logger.info("Loading initialize...");
            Thread.sleep(20*1000);
        } catch (InterruptedException e) {}

        validator = CassandraRunner.getBean(QueryValidator.class);
        properties = CassandraRunner.getBean("properties",Properties.class);

        if( validator == null || properties == null) {
            throw new InitializationError("Application context not loaded.");
        }

        String enableString = (String)properties.get("usergrid.query.validator.api.enablelocal");
        enableLocalServer = StringUtils.endsWithIgnoreCase("true", enableString);
        if( enableLocalServer ) {
            startStandaloneServer();
            try {
                logger.info("Loading standalone server...");
                Thread.sleep(20*1000);
            } catch (InterruptedException e) {}
        }
        initialize = true;

        String endpoint = (String)properties.get("usergrid.query.validator.api.endpoint");
        String organization = (String)properties.get("usergrid.query.validator.api.organization");
        String app = (String)properties.get("usergrid.query.validator.api.app");
        String email = (String)properties.get("usergrid.query.validator.api.authorize.email");
        String password = (String)properties.get("usergrid.query.validator.api.authorize.password");

        String collection = "user";
        List<Entity> entities = loadEntities(collection);
        QueryValidationConfiguration configuration = new QueryValidationConfiguration();
        configuration.setEndpointUri(endpoint);
        configuration.setOrg(organization);
        configuration.setEmail(email);
        configuration.setPassword(password);
        configuration.setApp(app);
        configuration.setCollection(collection);
        configuration.setEntities(entities);
        validator.setConfiguration(configuration);
        validator.setup();

    }

    private List<Entity> loadEntities(String collection) {
        String json = null;
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(collection + ".json");
            json = FileUtils.readFileToString(FileUtils.toFile(url), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Entity> entities = new ArrayList<Entity>();
        List<Map<String, Object>> datas = (List<Map<String, Object>>) JsonUtils.parse(json);
        for(Map<String, Object> data : datas) {
            Entity entity = new QueryEntity();
            entity.setType(collection);
            entity.setProperties(data);
            entity.setCreated(System.currentTimeMillis());
            entity.setModified(entity.getCreated());
            entities.add(entity);
        }
        return entities;
    }

    private void startStandaloneServer() {
        standaloneServer = new StandaloneServer();
        try {
            Thread thread = new Thread(standaloneServer);
            thread.start();
        } catch (Exception ex) {
            logger.error("Could not schedule standalone server runner", ex);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    logger.error("In shutdownHook");
                    stopServer();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        boolean loaded = false;
        while (!loaded) {
            if( standaloneServer.server.isRunning() ) {
                if( standaloneServer.server.getManagementService() != null ) {
                    loaded = true;
                    continue;
                }
            }
            try {
                logger.info("Loding application context...");
                Thread.sleep(2*1000);
            } catch (InterruptedException e) {
                continue;
            }
        }

        if(loaded) {
            ManagementService managementService = standaloneServer.server.getManagementService();
            try {
                managementService.setup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        logger.info("Finish start standalone server.");
    }

    static class StandaloneServer implements Runnable {
        public static Server server;

        public StandaloneServer() {
            server = new Server();
        }

        @Override
        public void run() {
            server.setInitializeDatabaseOnStart(true);
            server.setDaemon(false);
            server.startServer();
        }

        public void stop() {
            server.stopServer();
        }
    }

    private void stopServer() {
        standaloneServer.stop();
    }
}
