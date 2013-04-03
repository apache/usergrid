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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Schema;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Sung-ju Jin(realbeast)
 */
@Component
public class SqliteRunner implements QueryRunner {

    private Logger logger = Logger.getLogger(SqliteRunner.class.getName());

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private String collection;
    private List<Entity> entities;

    @Override
    public boolean setup() {
        createTable();
        insertDatas();

        return true;
    }

    private void insertDatas() {
        for( Entity entity : entities ) {
            insertData(entity);
        }
    }

    private void insertData(Entity entity) {
        StringBuilder feilds = new StringBuilder();
        StringBuilder values = new StringBuilder();
        Map<String, Object> properties = Schema.getDefaultSchema().getEntityProperties(entity);
        for(String key : properties.keySet()) {
            feilds.append(key);
            feilds.append(",");

            Object value = entity.getProperty(key);
            Class type = value.getClass();
            String sqlType = getSqlDatatype(type);
            if( StringUtils.equals("TEXT", sqlType) ) {
                values.append("'");
                values.append(value);
                values.append("'");
            } else {
                values.append(value);
            }
            values.append(",");
        }
        feilds.deleteCharAt(feilds.length()-1);
        values.deleteCharAt(values.length()-1);

        StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO ");
        builder.append(collection);
        builder.append("(");
        builder.append(feilds);
        builder.append(")");
        builder.append(" VALUES(");
        builder.append(values);
        builder.append(")");
        logger.info(builder.toString());
        jdbcTemplate.execute(builder.toString());
    }

    private void createTable() {
        Entity entity = entities.get(0);
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ");
        builder.append(collection);
        builder.append("(");
        Map<String, Object> properties = Schema.getDefaultSchema().getEntityProperties(entity);
        for(String key : properties.keySet()) {
            builder.append(key);
            builder.append(" ");

            Class type = entity.getProperty(key).getClass();
            String sqlType = getSqlDatatype(type);
            builder.append(sqlType);
            builder.append(",");
        }
        builder.deleteCharAt(builder.length()-1);
        builder.append(")");

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + collection);
        logger.info(builder.toString());
        jdbcTemplate.execute(builder.toString());
    }

    @Override
    public List<Entity> execute(String query) {
        List<Entity> entities = jdbcTemplate.query(query, new RowMapper<Entity>() {
            @Override
            public Entity mapRow(ResultSet rs, int i) throws SQLException {
                ResultSetMetaData rsmd = rs.getMetaData();
                Entity entity = new QueryEntity();
                for( int j = 1 ; j <= rsmd.getColumnCount() ; j++) {
                    entity.setProperty(rsmd.getColumnName(j), rs.getObject(j));
                }
                return entity;
            }
        });
        return entities;
    }

    @Override
    public List<Entity> execute(String query, int limit) {
        return execute(query);
    }

    // based on sqlite (http://www.sqlite.org/datatype3.html)
    private String getSqlDatatype(Class clz) {
        if( clz == Integer.class || clz == Long.class || clz == Boolean.class ) {
            return "INTEGER";
        } else if( clz == Double.class ) {
                return "REAL";
        } else if( clz == Byte.class ) {
            return "BLOB";
        }
        return "TEXT";
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities;
    }
}
