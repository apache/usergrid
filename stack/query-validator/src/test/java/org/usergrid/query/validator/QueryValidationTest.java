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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert;

/**
 * @author Sung-ju Jin(realbeast)
 */
@RunWith(QueryValidatorRunner.class)
public class QueryValidationTest {

    private QueryValidator validator;

    @Before
    public void setup() {
        validator = QueryValidatorRunner.getValidator();
    }

    @Test
    public void sexEqualAndNameEqual() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and name = 'judekim' LIMIT 10";
        String api = "SELECT * WHERE sex = 'male' AND name = 'judekim'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }
}
