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

import java.util.*;
import java.util.logging.Logger;
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Schema;
import org.springframework.stereotype.Component;


/**
 * @author Sungju Jin
 */
@Component
public class ApiServerRunner implements QueryRunner {

    private Logger logger = Logger.getLogger(SqliteRunner.class.getName());
    private UsergridClient client;

    private String org;
    private String app;
    private String baseUri;
    private String email;
    private String password;
    private String collection;
    private List<Entity> entities;

    @Override
    public boolean setup() {

        client = new UsergridClient(getOrg(), getApp(), getBaseUri());
        UsergridUserAuth usergridUserAuth = new UsergridUserAuth(email, password, true);
        client.authenticateUser(usergridUserAuth);

        return insertDatas();
    }

    public boolean insertDatas() {
       List<UsergridEntity> clientEntities = getEntitiesForClient(getEntities());
       client.POST(clientEntities);
       return true;
    }

    private List<UsergridEntity> getEntitiesForClient(List<Entity> entities) {
        List<UsergridEntity> clientEntities = new ArrayList<>();
        for(Entity entity : entities) {
            UsergridEntity clientEntity = new UsergridEntity(entity.getType());

            Map<String, Object> properties = Schema.getDefaultSchema().getEntityProperties(entity);
            for(String key : properties.keySet()) {
                Object value = entity.getProperty(key);
                if( value instanceof String )
                    clientEntity.putProperty(key,(String)value );
                else if( value instanceof Long )
                    clientEntity.putProperty(key,(Long)value );
                else if( value instanceof Integer )
                    clientEntity.putProperty(key,(Integer)value );
                else if( value instanceof Float )
                    clientEntity.putProperty(key,(Float)value );
                else if( value instanceof Boolean )
                    clientEntity.putProperty(key,(Boolean)value );
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
        UsergridQuery usergridQuery = new UsergridQuery().ql(query).limit(limit).type(getCollection());
        UsergridResponse response = client.GET(usergridQuery);
        List<Entity> entities = new ArrayList<Entity>();
        if( response.getEntities() == null )
            return entities;

        for(UsergridEntity clientEntity : response.getEntities()) {
            Entity entity = new QueryEntity();
            entity.setUuid(UUID.fromString(clientEntity.getUuid()));
            entity.setType(clientEntity.getType());
            Map<String, ?> values = clientEntity.toMapValue();
            for( String key : values.keySet() ) {
                Object node = values.get(key);
                entity.setProperty(key, node);
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
