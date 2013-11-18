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

import org.apache.commons.lang.StringUtils;
import org.usergrid.persistence.Entity;

import java.util.List;

/**
 * @author Sung-ju Jin(realbeast)
 */
public class QueryResponse {

    boolean result;
    List<Entity> expacted;
    List<Entity> actually;
    String description;
    QueryRequest request;

    public boolean result() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public List<Entity> getExpacted() {
        return expacted;
    }

    public void setExpacted(List<Entity> expacted) {
        this.expacted = expacted;
    }

    public List<Entity> getActually() {
        return actually;
    }

    public void setActually(List<Entity> actually) {
        this.actually = actually;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QueryRequest getRequest() {
        return request;
    }

    public void setRequest(QueryRequest request) {
        this.request = request;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append(StringUtils.rightPad("", 20, "-"));
        builder.append("\n");
        builder.append(StringUtils.rightPad("* Expected", 20, " "));
        builder.append("\n");
        builder.append("list size : " + expacted.size());
        builder.append("\n");
        for(Entity entity : expacted) {
            builder.append(entity.toString());
            builder.append("\n");
        }

        builder.append("\n");
        builder.append(StringUtils.rightPad("* Actually", 20, " "));
        builder.append("\n");
        builder.append("list size : " + actually.size());
        builder.append("\n");
        for(Entity entity : actually) {
            builder.append(entity.toString());
            builder.append("\n");
        }

        builder.append(StringUtils.rightPad("", 20, "-"));
        builder.append("\n");
        return builder.toString();
    }
}
