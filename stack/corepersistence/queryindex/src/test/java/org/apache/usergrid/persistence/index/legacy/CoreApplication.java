/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.usergrid.persistence.index.legacy;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;


public class CoreApplication implements Application, TestRule {

    private final static Logger LOG = LoggerFactory.getLogger( CoreApplication.class );
    protected UUID id;
    protected String appName;
    protected String orgName;
    protected CoreITSetup setup;
    protected EntityManagerFacade em;
    protected Map<String, Object> properties = new LinkedHashMap<String, Object>();


    public CoreApplication( CoreITSetup setup ) {
        this.setup = setup;
    }

    public void setEntityManager( EntityManagerFacade em ) {
        this.em = em;
    }

    @Override
    public void putAll( Map<String, Object> properties ) {
        this.properties.putAll( properties );
    }


    @Override
    public Object get( String key ) {
        return properties.get( key );
    }


    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }


    @Override
    public UUID getId() {
        return id;
    }


    @Override
    public String getOrgName() {
        return orgName;
    }


    @Override
    public String getAppName() {
        return appName;
    }


    @Override
    public Entity create( String type ) throws Exception {
        Entity entity = em.create( type, properties );
        clear();
        return entity;
    }


    @Override
    public Object put( String property, Object value ) {
        return properties.put( property, value );
    }


    @Override
    public void clear() {
        properties.clear();
    }

    @Override
    public Entity get( Id id ) throws Exception {
        return em.get( id );
    }

    @Override
    public Statement apply( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before( description );

                try {
                    base.evaluate();
                }
                finally {
                    after( description );
                }
            }
        };
    }


    protected void after( Description description ) {
    }


    protected void before( Description description ) throws Exception {
    }


    public EntityManagerFacade getEm() {
        return em;
    }

    public void addToCollection( Entity user, String collection, Entity item ) throws Exception {
        em.addToCollection( user, collection, item );
    }

    public Results searchCollection( Entity user, String collection, Query query ) throws Exception {
        return em.searchCollection( user, collection, query );
    }

}
