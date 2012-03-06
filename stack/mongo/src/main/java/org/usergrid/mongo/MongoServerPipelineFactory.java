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
package org.usergrid.mongo;

import org.apache.shiro.mgt.SessionsSecurityManager;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.services.ServiceManagerFactory;

public class MongoServerPipelineFactory implements ChannelPipelineFactory {

	private final ExecutionHandler executionHandler;
	private final EntityManagerFactory emf;
	private final ServiceManagerFactory smf;
	private final ManagementService management;
	private final SessionsSecurityManager securityManager;

	public MongoServerPipelineFactory(EntityManagerFactory emf,
			ServiceManagerFactory smf, ManagementService management,
			SessionsSecurityManager securityManager,
			ExecutionHandler executionHandler) {
		this.emf = emf;
		this.smf = smf;
		this.management = management;
		this.securityManager = securityManager;
		this.executionHandler = executionHandler;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		return Channels.pipeline(new MongoMessageEncoder(),
				new MongoMessageDecoder(), executionHandler,
				new MongoChannelHandler(emf, smf, management, securityManager));
	}

}
