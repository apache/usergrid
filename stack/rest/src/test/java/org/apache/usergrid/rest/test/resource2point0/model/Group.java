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
package org.apache.usergrid.rest.test.resource2point0.model;

import java.util.List;
import java.util.Map;

/**
 * Created by rockerston on 12/16/14.
 */
public class Group extends Entity{

    public Group(){}

    public Group(String name, String path) {

        this.put("name", name);
        this.put("path", path);
    }

    public Group (ApiResponse<Entity> response){
        super(response);
    }

    public String getName(){
        return (String) this.get("name");
    }

    public String getPath(){
        return (String) this.get("path");
    }



}
