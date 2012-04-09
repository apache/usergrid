/*******************************************************************************
 * Copyright 2012 Apigee Corporation
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
package org.usergrid.rest.applications.users;

import static org.usergrid.utils.MapUtils.hashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.rest.applications.utils.UserRepo;
import org.usergrid.utils.UUIDUtils;

/**
 * @author tnine
 */
public class ActivityResourceTest extends AbstractRestTest {
    private static Logger log = LoggerFactory
            .getLogger(ActivityResourceTest.class);

    public ActivityResourceTest() throws Exception {

    }

}
