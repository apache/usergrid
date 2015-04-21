/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence;


import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.google.inject.Injector;


/**
 * This allows us to search guice for beans that are not in spring
 */
@Component
public class GuiceAdapterBeanFactory extends DefaultListableBeanFactory {

    /**
     * Wire our injector into this so we can use it go get beans
     */
    @Autowired
    private Injector injector;


    public <T> T getBean( Class<T> requiredType ) throws BeansException {
        final T bean = super.getBean( requiredType );

        // Comes from spring, return it
        if ( bean != null ) {
            return bean;
        }

        final T guiceBean = injector.getInstance( requiredType );

        if(guiceBean == null){
            throw new NoGuiceBean( "Could not find bean for class" + requiredType );
        }


        return guiceBean;
    }


    @Override
    public Object getBean( final String name ) throws BeansException {
        final Object springBean = super.getBean( name );

        return validateBean( springBean, name );
    }


    @Override
    public <T> T getBean( final String name, final Class<T> requiredType ) throws BeansException {
        final T springBean = super.getBean( name, requiredType );

        return validateBean( springBean, name );
    }


    @Override
    public <T> T getBean( final String name, final Class<T> requiredType, final Object... args ) throws BeansException {
        final T springBean = super.getBean( name, requiredType, args );

       return validateBean( springBean, name );
    }


    /**
     * If we can't find the spring bean, we should blow up
     * @param springBean
     * @param <T>
     * @return
     */
    private <T> T validateBean( T springBean, final String name ) {
        if ( springBean == null ) {
            throw new NoGuiceBean( String.format("Guice beans by name is unsupoported, and could not find a spring bean with the name '%s'", name) );
        }

        return springBean;
    }


    /**
     * Exception class to throw when we can't find a bean in spring, and can't find it in guice
     */
    public static final class NoGuiceBean extends BeansException{

        public NoGuiceBean( final String msg ) {
            super( msg );
        }
    }
}


