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
package org.apache.usergrid.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.utils.MapUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Simple test verifies if REST test infrastructure is functioning.
 */
public class SimplestTest extends org.apache.usergrid.rest.test.resource2point0.AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger(SimplestTest.class);

    @Test
    public void getGetToken() {
        assertNotNull( getAdminToken() );
    }

    @Test
    public void testEntityPost() {

        Entity cat = new Entity();
        cat.put("name", "Bertha");
        cat.put("property1", "value1");
        Entity savedCat = this.app().collection("cats").post(cat);

        assertEquals( cat.get("property1"), savedCat.get("property1"));
    }

    @Test
    public void testEntityPostAndGet() {

        Entity dog = new Entity();
        dog.put("name", "Pokey");
        dog.put("property1", "value1");
        this.app().collection("dogs").post(dog);
        refreshIndex();

        Collection savedDogs = this.app().collection("dogs").get();
        Entity savedDog = (Entity)savedDogs.iterator().next();
        assertEquals( dog.get("property1"), savedDog.get("property1"));
    }
}
