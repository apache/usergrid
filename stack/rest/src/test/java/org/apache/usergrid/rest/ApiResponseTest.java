/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.rest;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ApiResponseTest {

    @Test
    public void testIgnoreQP() {
        ApiResponse apiResponse = new ApiResponse();
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        params.put("access_token", Arrays.asList("YWMtL8AQ-ukcEeS2lHs-P-n8wQAAAU0GaCt_Y0cPWeXMJij4x_fW0w_dTMpUH7I"));
        params.put("name", Arrays.asList("test"));
        params.put("username", Arrays.asList("abc"));
        params.put("password", Arrays.asList("123"));
        apiResponse.setParams(params);
        assertNull(apiResponse.getParams().get("password"));
        assertEquals(apiResponse.getParams().size(), 1);
    }
}
