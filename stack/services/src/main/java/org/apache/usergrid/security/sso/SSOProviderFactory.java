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
package org.apache.usergrid.security.sso;

import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.tokens.impl.TokenServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by russo on 6/24/16.
 */
public class SSOProviderFactory {

    enum Provider {
        APIGEE, USERGRID
    }

    private EntityManagerFactory emf;
    protected Properties properties;


    public ExternalSSOProvider getProvider(){

        return getSpecificProvider(properties.getProperty(TokenServiceImpl.USERGRID_EXTERNAL_SSO_PROVIDER));

    }

    public ExternalSSOProvider getSpecificProvider(String providerName){

        final Provider specifiedProvider ;
        try{
            specifiedProvider = Provider.valueOf(providerName.toUpperCase());
        }
        catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Unsupported provider");
        }

        switch (specifiedProvider){
            case APIGEE:
                return ((CpEntityManagerFactory)emf).getApplicationContext().getBean( ApigeeSSO2Provider.class );
            case USERGRID:
                return ((CpEntityManagerFactory)emf).getApplicationContext().getBean( UsergridExternalProvider.class );
            default:
                throw new RuntimeException("Unknown SSO provider");
        }
    }


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public List<String> getProvidersList() {
        return Stream.of(Provider.values())
            .map(Enum::name)
            .collect(Collectors.toList());
    }
}
