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
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.cassandra.DataControl;
import org.usergrid.persistence.Entity;
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

    private Properties properties;
    private static QueryValidator validator;

    public QueryValidatorRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected DataControl preTest(RunNotifier notifier) {
        DataControl control = super.preTest(notifier);
        setup();
        return control;
    }

    public static QueryValidator getValidator() {
        return validator;
    }

    public void setup() {
        validator = CassandraRunner.getBean(QueryValidator.class);
        properties = CassandraRunner.getBean("properties",Properties.class);

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
}
