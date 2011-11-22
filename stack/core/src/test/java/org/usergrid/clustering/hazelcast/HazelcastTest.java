/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.usergrid.clustering.hazelcast;

import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Instance;
import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;
import com.hazelcast.core.Member;
import com.hazelcast.core.MessageListener;

@Ignore
public class HazelcastTest implements InstanceListener, MessageListener<Object> {

	private static final Logger logger = LoggerFactory.getLogger(HazelcastTest.class);

	ClassPathXmlApplicationContext ac;

	@Before
	public void setup() throws Exception {
		// assertNotNull(client);

		String maven_opts = System.getenv("MAVEN_OPTS");
		logger.info("Maven options: " + maven_opts);

		String[] locations = { "testApplicationContext.xml" };
		ac = new ClassPathXmlApplicationContext(locations);

		AutowireCapableBeanFactory acbf = ac.getAutowireCapableBeanFactory();
		acbf.autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		acbf.initializeBean(this, "testClient");

	}

	@Test
	public void doTest() {
		logger.info("do test");
		Hazelcast.addInstanceListener(this);

		ITopic<Object> topic = Hazelcast.getTopic("default");
		topic.addMessageListener(this);
		topic.publish("my-message-object");

		Collection<Instance> instances = Hazelcast.getInstances();
		for (Instance instance : instances) {
			logger.info("ID: [" + instance.getId() + "] Type: ["
					+ instance.getInstanceType() + "]");
		}

		Set<Member> setMembers = Hazelcast.getCluster().getMembers();
		for (Member member : setMembers) {
			logger.info("isLocalMember " + member.localMember());
			logger.info("member.inetsocketaddress "
					+ member.getInetSocketAddress());
		}

	}

	@Override
	public void instanceCreated(InstanceEvent event) {
		Instance instance = event.getInstance();
		logger.info("Created instance ID: [" + instance.getId() + "] Type: ["
				+ instance.getInstanceType() + "]");
	}

	@Override
	public void instanceDestroyed(InstanceEvent event) {
		Instance instance = event.getInstance();
		logger.info("Destroyed isntance ID: [" + instance.getId() + "] Type: ["
				+ instance.getInstanceType() + "]");

	}

	@After
	public void teardown() {
		logger.info("Stopping test");
		ac.close();
	}

	@Override
	public void onMessage(Object msg) {
		logger.info("Message received = " + msg);
	}

}
