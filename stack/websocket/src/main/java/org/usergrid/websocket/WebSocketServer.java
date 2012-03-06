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
/*
 * Copyright 2010 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.usergrid.websocket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.realm.Realm;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.usergrid.services.ServiceManagerFactory;

/**
 * An HTTP server which serves Web Socket requests at:
 * 
 * http://localhost:8080/websocket
 * 
 * Open your browser at http://localhost:8080/, then the demo page will be
 * loaded and a Web Socket connection will be made automatically.
 * 
 */
public class WebSocketServer {

	private static final Logger logger = LoggerFactory
			.getLogger(WebSocketServer.class);

	EntityManagerFactory emf;
	ServiceManagerFactory smf;
	ManagementService management;
	Realm realm;
	SessionsSecurityManager securityManager;
	boolean ssl = false;
	Channel channel;
	Properties properties;

	public static void main(String[] args) throws Exception {
		WebSocketServer server = new WebSocketServer();
		server.startSpring();
		server.startServer();
	}

	public WebSocketServer() {
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Autowired
	public void setServiceManagerFactory(ServiceManagerFactory smf) {
		this.smf = smf;
	}

	@Autowired
	public void setManagementService(ManagementService management) {
		this.management = management;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	@Autowired
	public void setRealm(Realm realm) {
		this.realm = realm;
	}

	public Properties getProperties() {
		return properties;
	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public String[] getApplicationContextLocations() {
		String[] locations = { "applicationContext.xml" };
		return locations;
	}

	public void startSpring() {

		String[] locations = getApplicationContextLocations();
		ApplicationContext ac = new ClassPathXmlApplicationContext(locations);

		AutowireCapableBeanFactory acbf = ac.getAutowireCapableBeanFactory();
		acbf.autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		acbf.initializeBean(this, "webSocketServer");

		assertNotNull(emf);
		assertTrue(
				"EntityManagerFactory is instance of EntityManagerFactoryImpl",
				emf instanceof EntityManagerFactoryImpl);

	}

	public void startServer() {
		if ((properties != null)
				&& (Boolean.parseBoolean(properties.getProperty(
						"usergrid.websocket.disable", "false")))) {
			logger.info("Usergrid WebSocket Server Disabled");
			return;
		}

		logger.info("Starting Usergrid WebSocket Server");

		if (realm != null) {
			securityManager = new DefaultSecurityManager(realm);
		}

		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		ExecutionHandler executionHandler = new ExecutionHandler(
				new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576));

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new WebSocketServerPipelineFactory(emf,
				smf, management, securityManager, executionHandler, ssl));

		// Bind and start to accept incoming connections.
		channel = bootstrap.bind(new InetSocketAddress(8088));

		logger.info("Usergrid WebSocket Server started...");
	}

	public void stopServer() {
		logger.info("Stopping WebSocket Server");
		if (channel != null) {
			channel.close();
			channel = null;
		}
		logger.info("Usergrid WebSocket Server stopped...");
	}
}
