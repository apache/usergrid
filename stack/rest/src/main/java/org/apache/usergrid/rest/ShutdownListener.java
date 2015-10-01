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


import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.batch.service.SchedulerService;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionBindingEvent;
import java.util.Properties;


/**
 * Shutdown job service when context is destroyed.
 * (Added for Arquillian testing purposes when we have to deploy, re-deploy, etc.)
 */
public class ShutdownListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownListener.class);
    JobSchedulerService schedulerService;
    Properties properties;

    public ShutdownListener() {
    }

    public void contextInitialized(ServletContextEvent sce) {

        ApplicationContext ctx = WebApplicationContextUtils
            .getWebApplicationContext(sce.getServletContext());

        schedulerService = ctx.getBean( JobSchedulerService.class );
        properties = (Properties)ctx.getBean("properties");

        logger.info("ShutdownListener initialized");
    }

    public void contextDestroyed(ServletContextEvent sce) {

        logger.info("ShutdownListener invoked");

        boolean started = Boolean.parseBoolean(
            properties.getProperty(JobServiceBoostrap.START_SCHEDULER_PROP, "true"));

        if ( started ) {
            schedulerService.stopAsync();
            schedulerService.awaitTerminated();
            logger.info( "Stopped Scheduler Service..." );
        }
    }
}
