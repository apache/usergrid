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
module.exports = {
    serverUrl: "http://localhost:8080/",
    orgName: "test-organization", //must
    appName: "test-app", //must pre create app
    numberOfUsers: 5,
    numberOfEntitiesConsistent: 100,
    consistentChecks:3,
    numberOfEntities: 20,
    org: {
        clientId: "",
        clientSecret: ""
    },
    usersCollection: "users",
    entitiesTestCollection: "cats",
    genericTestCollection1: "dogs",
    genericTestCollection2: "horses",
    consumableTestCollection: "food",
    location: { // London
        latitude: 51.51279,
        longitude: -0.09184
    },
    notifierName: "noop-dev"
};
