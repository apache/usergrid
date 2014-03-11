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
package org.apache.usergrid.query.validator.users;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.query.validator.AbstractQueryIT;
import org.apache.usergrid.query.validator.QueryRequest;
import org.apache.usergrid.query.validator.QueryResponse;
import org.apache.usergrid.query.validator.QueryResultsMatcher;
import org.apache.usergrid.utils.StringUtils;

import java.util.List;

/**
 * @author Sungju Jin
 */
public class UserQueryIT extends AbstractQueryIT {

    @BeforeClass
    public static void setDatas() {
        createInitializationDatas("user");
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
        request.getApiQuery().setLimit(4);
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
        request.getApiQuery().setLimit(4);
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
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    String sex = (String)entity.getProperty("sex");

                    if((StringUtils.equals("male",sex)) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
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
    public void sexEqualAndAgeGreaterthanequal_sortAgeDesc_limitL20() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 ORDER BY age desc LIMIT 20";
        String api = "select * where sex = 'male' and age >= 35 order by age desc";
        int limit = 20;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlSexEqualAndAgeGreaterthanequal_sortNameDesc_limitL20() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 ORDER BY name desc LIMIT 20";
        String api = "select * where sex = 'male' and age >= 35 order by name desc";
        int limit = 20;

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
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    int age = (Integer)entity.getProperty("age");
                    String sex = (String)entity.getProperty("sex");

                    if(((StringUtils.equals("male",sex) && age >= 35) || StringUtils.equals("female",sex)) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
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
    public void sexEqualAndAgeGreaterthanequalOrSexEqual_sortNameDesc_limitL20() {
        String sqlite = "SELECT * FROM users WHERE sex = 'male' and age >= 35 or sex = 'female' ORDER BY name desc LIMIT 20";
        String api = "select * where sex = 'male' and age >= 35 or sex = 'female' order by name desc";
        int limit = 20;

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

    @Test
    public void qlNameEqual() {
        String sqlite = "SELECT * FROM users WHERE name = 'judekim' LIMIT 10";
        String api = "select * where name = 'judekim'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAgeEqual() {
        String sqlite = "SELECT * FROM users WHERE age = 16 LIMIT 10";
        String api = "select * where age = 16";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlNameEqualAndAgeEqual() {
        String sqlite = "SELECT * FROM users WHERE name = 'askagirl' and age = 16 LIMIT 10";
        String api = "select * where name = 'askagirl' and age = 16";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    String name = entity.getName();
                    int age = (Integer)entity.getProperty("age");
                    if ((StringUtils.equals("askagirl", name) && age == 16) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAgeLessthan() {
        String sqlite = "SELECT * FROM users WHERE age < 16 LIMIT 10";
        String api = "select * where age < 16";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    int age = (Integer)entity.getProperty("age");
                    if((age < 16) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAgeLessthanequal() {
        String sqlite = "SELECT * FROM users WHERE age <= 16 LIMIT 20";
        String api = "select * where age <= 16";
        int limit = 20;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    int age = (Integer)entity.getProperty("age");
                    if((age <= 16) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAgeGreaterthan() {
        String sqlite = "SELECT * FROM users WHERE age > 16 LIMIT 10";
        String api = "select * where age > 16";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    int age = (Integer)entity.getProperty("age");
                    if((age > 16) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAgeGreaterthanequal() {
        String sqlite = "SELECT * FROM users WHERE age >= 16 LIMIT 10";
        String api = "select * where age >= 16";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    int age = (Integer)entity.getProperty("age");
                    if((age >= 16) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAgeGreaterthanequalAndAgeLessthan() {
        String sqlite = "SELECT * FROM users WHERE age >= 32 and age < 40 LIMIT 10";
        String api = "select * where age >= 32 and age < 40";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    int age = (Integer)entity.getProperty("age");
                    if((age >= 32 && age < 40) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAgeGreaterthanequalAndAgeLessthanAndSexEqual() {
        String sqlite = "SELECT * FROM users WHERE age >= 32 and age < 40 and sex = 'female' LIMIT 10";
        String api = "select * where age >= 32 and age < 40 and sex = 'female'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    int age = (Integer)entity.getProperty("age");
                    String sex = (String)entity.getProperty("sex");

                    if((age >= 32 && age < 40 && StringUtils.equals("female",sex)) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAddressFulltext() {
        String sqlite = "SELECT * FROM users WHERE address LIKE '%서울시%' LIMIT 10";
        String api = "select * where address contains '서울시'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    String address = (String)entity.getProperty("address");

                    if((StringUtils.contains(address,"서울시")) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAddressFulltextstartswith() {
        String sqlite = " SELECT * FROM users WHERE address LIKE 'A%' LIMIT 10";
        String api = "select * where address contains 'A*'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    String address = (String)entity.getProperty("address");

                    if((StringUtils.startsWith(address,"A")) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAddressBeginswith() {
        String sqlite = "SELECT * FROM users WHERE address LIKE 'B%' LIMIT 10";
        String api = "select * where address = 'B*'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    String address = (String)entity.getProperty("address");

                    if((StringUtils.startsWith(address,"B")) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAddressBeginswithAndSexFulltextstartswith() {
        String sqlite = "SELECT * FROM users WHERE address LIKE 'C%' and sex LIKE 'ma%' LIMIT 10";
        String api = "select * where address = 'C*' and sex contains 'ma*'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    String address = (String)entity.getProperty("address");

                    if((StringUtils.startsWith(address,"C")) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlAddressBeginswithAndSexFulltext() {
        String sqlite = "SELECT * FROM users WHERE (address LIKE 'D%' and sex LIKE '%male%') LIMIT 10";
        String api = "select * where address = 'D*' and sex contains 'male'";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    String address = (String)entity.getProperty("address");

                    if((StringUtils.startsWith(address,"D")) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void qlSexEqualOrAgeGreaterthanequalAndAgeLessthan_limitL20() {
        String sqlite = "SELECT * FROM users WHERE sex = 'female' or age >= 12 and age < 20 LIMIT 20";
        String api = "select * where sex = 'female' or age >= 12 and age < 20";
        int limit = 20;

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        request.getApiQuery().setLimit(limit);
        QueryResponse response = validator.execute(request, new QueryResultsMatcher() {
            @Override
            public boolean equals(List<Entity> expectedEntities, List<Entity> actuallyEntities) {
                boolean equals = expectedEntities.size() == expectedEntities.size();
                if( !equals )
                    return false;

                for(Entity entity : actuallyEntities) {
                    int age = (Integer)entity.getProperty("age");
                    String sex = (String)entity.getProperty("sex");

                    if(((StringUtils.equals("female",sex) || age >= 12) && age < 20) == false) {
                        return false;
                    }
                }
                return equals;
            }
        });
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortNameAsc() {
        String sqlite = "SELECT * FROM users ORDER BY name asc LIMIT 10";
        String api = "select * order by name asc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortNameDesc() {
        String sqlite = "SELECT * FROM users ORDER BY name desc LIMIT 10";
        String api = "select * order by name desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortNameAscAgeDesc() {
        String sqlite = "SELECT * FROM users ORDER BY name asc, age desc LIMIT 10";
        String api = "select * order by name asc, age desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortNameAscAddressAsc() {
        String sqlite = "SELECT * FROM users ORDER BY name asc, address asc LIMIT 10";
        String api = "select * order by name asc, address asc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortCreatedAsc() {
        String sqlite = "SELECT * FROM users ORDER BY created asc LIMIT 10";
        String api = "select * order by created asc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortCreatedDesc() {
        String sqlite = "SELECT * FROM users ORDER BY created desc LIMIT 10";
        String api = "select * order by created desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortModifiedAsc() {
        String sqlite = "SELECT * FROM users ORDER BY modified asc LIMIT 10";
        String api = "select * order by modified asc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortCreatedDescNameAsc() {
        String sqlite = "SELECT * FROM users ORDER BY created desc, name asc LIMIT 10";
        String api = "select * order by created desc, name asc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }

    @Test
    public void sortNameAscCreatedDesc() {
        String sqlite = "SELECT * FROM users ORDER BY name asc, created desc LIMIT 10";
        String api = "select * order by name asc, created desc";

        QueryRequest request = new QueryRequest();
        request.setDbQuery(sqlite);
        request.getApiQuery().setQuery(api);
        QueryResponse response = validator.execute(request);
        Assert.assertTrue(response.toString(), response.result());
    }
}
