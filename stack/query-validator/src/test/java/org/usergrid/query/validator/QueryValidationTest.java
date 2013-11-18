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

    @Test
    public void nameEqualAndSexEqual() {
        String sqlite = "SELECT * FROM users WHERE name = 'judekim' and sex = 'male' LIMIT 10";
        String api = "select * where name = 'judekim' and sex = 'male'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void nameEqualAndSexEqual_limitL20() {
        String sqlite = "SELECT * FROM users WHERE name = 'judekim' and sex = 'male' LIMIT 20";
        String api = "select * where name = 'judekim' and sex = 'male'";
        int limit = 20;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndNameEqualExtra1() {
        String sqlite = "SELECT * FROM users WHERE sex = 'female' and name = 'curioe' LIMIT 10";
        String api = "select * where sex = 'female' and name = 'curioe'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualOrNameEqual() {
        String sqlite = "SELECT * FROM users WHERE sex = 'female' or name = 'curioe' LIMIT 10";
        String api = "select * where sex = 'female' or name = 'curioe'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void nameBeginswithAndSexEqualAndAgeGreaterthanequalOrSexEqual_sortNameDesc() {
        String sqlite = "SELECT * FROM users WHERE name LIKE 'a%' and sex = 'male' and age >= 35 or sex = 'female' ORDER BY name desc LIMIT 10";
        String api = "select * where name = 'a*' and sex = 'male' and age >= 35 or sex = 'female' order by name desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void nameBeginswithAndSexEqualAndAgeGreaterthanequalOrSexEqual_sortAddressAscNameDesc() {
        String sqlite = "SELECT * FROM users WHERE name LIKE 'a%' and sex = 'male' and age >= 35 or sex = 'female' ORDER BY address asc, name desc LIMIT 4";
        String api = "select * where name = 'a*' and sex = 'male' and age >= 35 or sex = 'female' order by address asc, name desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void nameBeginswithAndSexEqualAndAgeGreaterthanequalOrSexEqual_sortAddressAscNameDesc_limitL4() {
        String sqlite = "SELECT * FROM users WHERE name LIKE 'a%' and sex = 'male' and age >= 35 or sex = 'female' ORDER BY address asc, name desc LIMIT 4";
        String api = "select * where name = 'a*' and sex = 'male' and age >= 35 or sex = 'female' order by address asc, name desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqual_sortAgeDescExtra1() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' ORDER BY age desc LIMIT 10";
        String api = "select * where sex = 'male' order by age desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqual_sortAgeDescExtra2() {
        String sqlite = " SELECT * FROM users WHERE sex = 'female' ORDER BY age desc LIMIT 10";
        String api = "select * where sex = 'female' order by age desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequal() {
        String sqlite = " SELECT * FROM users WHERE sex = 'male' and age >= 35 LIMIT 10";
        String api = "select * where sex = 'male' and age >= 35";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequal_sortAgeDesc() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 ORDER BY age desc LIMIT 10";
        String api = "select * where sex = 'male' and age >= 35 order by age desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequal_sortNameDesc() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 ORDER BY name desc LIMIT 10";
        String api = "select * where sex = 'male' and age >= 35 order by name desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequal_sortAgeDesc_limitL100() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 ORDER BY age desc LIMIT 100";
        String api = "select * where sex = 'male' and age >= 35 order by age desc";
        int limit = 100;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlSexEqualAndAgeGreaterthanequal_sortNameDesc_limitL100() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 ORDER BY name desc LIMIT 100";
        String api = "select * where sex = 'male' and age >= 35 order by name desc";
        int limit = 100;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequalOrSexEqual() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 or sex = 'female' LIMIT 10";
        String api = "select * where sex = 'male' and age >= 35 or sex = 'female'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequalOrSexEqual_sortAgeDesc() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 or sex = 'female' ORDER BY age desc LIMIT 10";
        String api = "select * where sex = 'male' and age >= 35 or sex = 'female' order by age desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void limitL12() {
        String sqlite = "SELECT * FROM users LIMIT 12";
        String api = null;
        int limit = 12;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequalOrSexEqual_sortNameDesc() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 or sex = 'female' ORDER BY name desc LIMIT 10";
        String api = "select * where sex = 'male' and age >= 35 or sex = 'female' order by name desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequalOrSexEqual_sortNameDesc_limitL100() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 or sex = 'female' ORDER BY name desc LIMIT 100";
        String api = "select * where sex = 'male' and age >= 35 or sex = 'female' order by name desc";
        int limit = 100;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void limitL11() {
        String sqlite = "SELECT * FROM users LIMIT 11";
        String api = null;
        int limit = 11;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void nameBeginswithAndSexEqualAndAgeGreaterthanequalOrSexEqual() {
        String sqlite = "SELECT * FROM users WHERE name LIKE 'a%' and sex = 'male' and age >= 20 or sex = 'female' LIMIT 10";
        String api = "select * where name = 'a*' and sex = 'male' and age >= 20 or sex = 'female'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void nameBeginswithAndSexEqualAndAgeGreaterthanequalOrSexEqual_limitL20() {
        String sqlite = "SELECT * FROM users WHERE name LIKE 'a%' and sex = 'male' and age >= 20 or sex = 'female' LIMIT 20";
        String api = "select * where name = 'a*' and sex = 'male' and age >= 20 or sex = 'female'";
        int limit = 20;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sexEqualAndAgeGreaterthanequal_sortAddressDesc_limitL100() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 ORDER BY address desc LIMIT 100";
        String api = "select * where sex = 'male' and age >= 35 order by address desc";
        int limit = 100;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }
}
