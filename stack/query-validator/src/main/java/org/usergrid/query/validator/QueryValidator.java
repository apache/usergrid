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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.Entity;

import java.util.List;

/**
 * @author Sung-ju Jin(realbeast)
 */
@Component
public class QueryValidator {

    QueryValidationConfiguration configuration;
    @Autowired
    ApiServerRunner api;
    @Autowired
    SqliteRunner sql;

    public QueryValidator() {
    }

    public QueryValidator(QueryValidationConfiguration configuration) {
        this.configuration = configuration;
    }

    public boolean setup() {
        return api.setup() && sql.setup();
    }

    public QueryResponse execute(QueryRequest request) {
        List<Entity> sqlEntities = sql.execute(request.getDbQuery());
        List<Entity> apiEntities = api.execute(request.getApiQuery().getQuery(), request.getApiQuery().getLimit());
        boolean equals = false;
        if( request.isIgnoreOrdering() ) {
            equals = sqlEntities.size() == apiEntities.size();
        } else {
            equals = sqlEntities.equals(apiEntities);
        }

        QueryResponse response = new QueryResponse();
        response.setResult(equals);
        response.setExpacted(sqlEntities);
        response.setActually(apiEntities);
        return response;
    }

    public void setConfiguration(QueryValidationConfiguration configuration) {
        this.configuration = configuration;
        sql.setCollection(configuration.getCollection());
        sql.setEntities(configuration.getEntities());

        api.setOrg(configuration.getOrg());
        api.setApp(configuration.getApp());
        api.setBaseUri(configuration.getEndpointUri());
        api.setCollection(configuration.getCollection());
        api.setEntities(configuration.getEntities());
        api.setEmail(configuration.getEmail());
        api.setPassword(configuration.getPassword());
    }
}
