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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Schema;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.apache.usergrid.java.client.Client;
import org.apache.usergrid.java.client.response.ApiResponse;
import static org.apache.usergrid.java.client.utils.ObjectUtils.isEmpty;


/**
 * @author Sungju Jin
 */
@Component
public class ApiServerRunner implements QueryRunner {

    private Logger logger = Logger.getLogger(SqliteRunner.class.getName());
    private Client client;

    private String org;
    private String app;
    private String baseUri;
    private String email;
    private String password;
    private String collection;
    private List<Entity> entities;

    @Override
    public boolean setup() {
        client = new Client(getOrg(), getApp()).withApiUrl(getBaseUri());
        String accessToken = authorize(email, password);
        if(!StringUtils.isEmpty(accessToken))
            client.setAccessToken(accessToken);
        return insertDatas();
    }

    public String authorize(String email, String password) {
        String accessToken = null;
        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("grant_type", "password");
        formData.put("username", email);
        formData.put("password", password);
        ApiResponse response = client.apiRequest(HttpMethod.POST, null, formData,
                "management", "token");
        if (!isEmpty(response.getAccessToken())) {
            accessToken = response.getAccessToken();
            logger.info("Access token: " + accessToken);
        } else {
            logger.info("Response: " + response);
        }
        return accessToken;
    }

    public boolean insertDatas() {
       List<org.apache.usergrid.java.client.entities.Entity> clientEntities = getEntitiesForClient(getEntities());
       for(org.apache.usergrid.java.client.entities.Entity entity : clientEntities) {
           ApiResponse response = client.createEntity(entity);
           if( response == null || !StringUtils.isEmpty(response.getError()) ) {
               logger.log(Level.SEVERE, response.getErrorDescription());
               //throw new RuntimeException(response.getErrorDescription());
           } else {
               logger.log(Level.INFO, response.toString());
           }
       }
       return true;
    }

    private List<org.apache.usergrid.java.client.entities.Entity> getEntitiesForClient(List<Entity> entities) {
        List<org.apache.usergrid.java.client.entities.Entity> clientEntities = new ArrayList<org.apache.usergrid.java.client.entities.Entity>();
        for(Entity entity : entities) {
            org.apache.usergrid.java.client.entities.Entity clientEntity = new org.apache.usergrid.java.client.entities.Entity();
            clientEntity.setType(entity.getType());
            Map<String, Object> properties = Schema.getDefaultSchema().getEntityProperties(entity);
            for(String key : properties.keySet()) {
                Object value = entity.getProperty(key);
                if( value instanceof String )
                    clientEntity.setProperty(key,(String)value );
                else if( value instanceof Long )
                    clientEntity.setProperty(key,(Long)value );
                else if( value instanceof Integer )
                    clientEntity.setProperty(key,(Integer)value );
                else if( value instanceof Float )
                    clientEntity.setProperty(key,(Float)value );
                else if( value instanceof Boolean )
                    clientEntity.setProperty(key,(Boolean)value );
            }
            clientEntities.add(clientEntity);
        }
        return clientEntities;
    }

    @Override
    public List<Entity> execute(String query) {
        return execute(query, 10);
    }

    @Override
    public List<Entity> execute(String query, int limit) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ql", query);
        params.put("limit", limit);
        ApiResponse response = client.apiRequest(HttpMethod.GET, params, null, getOrg(), getApp(), getCollection());
        List<Entity> entities = new ArrayList<Entity>();
        if( response.getEntities() == null )
            return entities;

        for(org.apache.usergrid.java.client.entities.Entity clientEntitity : response.getEntities()) {
            Entity entity = new QueryEntity();
            entity.setUuid(clientEntitity.getUuid());
            entity.setType(clientEntitity.getType());
            Map<String, JsonNode> values = clientEntitity.getProperties();
            for( String key : values.keySet() ) {
                JsonNode node = values.get(key);
                if( node.isBoolean() ) {
                    entity.setProperty(key, node.asBoolean());
                } else if( node.isInt() ) {
                    entity.setProperty(key, node.asInt());
                } else if( node.isLong() ) {
                    entity.setProperty(key, node.asLong());
                } else if( node.isDouble() ) {
                    entity.setProperty(key, node.asDouble());
                } else {
                    entity.setProperty(key, node.asText());
                }
            }
            entities.add(entity);
        }
        return entities;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
