/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.query.validator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.utils.JsonUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

import static org.junit.Assert.assertNotNull;

/**
 * @author Sungju Jin
 */
public class AbstractQueryIT {

    protected static QueryValidator validator;
    private static Properties properties;
    private static String fullEndpoint;
    private static String orgName;
    private static String appName;
    private static String email;
    private static String password;
    private static int port;

    @BeforeClass
    public static void tearsup() throws Exception {
        validator = QueryITSuite.cassandraResource.getBean(QueryValidator.class);
        properties = QueryITSuite.cassandraResource.getBean("properties",Properties.class);
        if( isDisableLocalServer()) {
            return;
        }
        setProperties();
        createOrganizationWithApplication();
    }

    private static void createOrganizationWithApplication() throws Exception {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        orgName = orgName + uuid;
        appName = appName + uuid;
        email = orgName + "@usergrid.com";
        ManagementService managementService = QueryITSuite.serverResource.getMgmtSvc();
        UserInfo user = managementService.createAdminUser(orgName, "Query Test", email, password, false, false);
        OrganizationInfo org = managementService.createOrganization(orgName, user, false );
        assertNotNull( org );
        ApplicationInfo app = managementService.createApplication( org.getUuid(), appName);
        assertNotNull( app );
    }

    private static void setProperties() {
        port = QueryITSuite.serverResource.getTomcatPort();
        fullEndpoint = (String)properties.get("usergrid.query.validator.api.endpoint") + ":" + port;
        orgName = (String)properties.get("usergrid.query.validator.api.organization");
        appName = (String)properties.get("usergrid.query.validator.api.app");
        email = (String)properties.get("usergrid.query.validator.api.authorize.email");
        password = (String)properties.get("usergrid.query.validator.api.authorize.password");
    }

    protected static void createInitializationDatas(String collection) {
        List<Entity> entities = loadEntitiesFromResource(collection);
        QueryValidationConfiguration configuration = new QueryValidationConfiguration();
        configuration.setEndpointUri(fullEndpoint);
        configuration.setOrg(orgName);
        configuration.setEmail(email);
        configuration.setPassword(password);
        configuration.setApp(appName);
        configuration.setCollection(collection);
        configuration.setEntities(entities);
        validator.setConfiguration(configuration);
        validator.setup();
    }

    private static List<Entity> loadEntitiesFromResource(String collection) {
        String json = null;
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(collection + ".json");
            json = FileUtils.readFileToString(FileUtils.toFile(url), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Entity> entities = new ArrayList<Entity>();
        List<Map<String, Object>> datas = (List<Map<String, Object>>) JsonUtils.parse(json);
        int index = 0;
        for(Map<String, Object> data : datas) {
            long created = System.currentTimeMillis() + (index*1000);

            QueryEntity entity = new QueryEntity();
            entity.setType(collection);
            for ( Map.Entry<String, Object> property : data.entrySet() ) {
                if(StringUtils.equals("name", property.getKey()))
                    entity.setName((String)property.getValue());
                else
                    entity.setProperty( property.getKey(), property.getValue() );
            }
            entity.setProperties(data);
            entity.setCreated(created);
            entity.setModified(created);
            entities.add(entity);
            index++;
        }
        return entities;
    }

    private static boolean isDisableLocalServer() {
        return !StringUtils.equalsIgnoreCase("true", (String) properties.get("usergrid.query.validator.api.enablelocal"));
    }
}
