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
package org.usergrid.persistence;


import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.AbstractCoreTest;
import org.usergrid.cassandra.Concurrent;


@Concurrent()
public class EntityManagerFactoryIT extends AbstractCoreTest
{

	private static final Logger LOG = LoggerFactory.getLogger(EntityManagerFactoryIT.class);


	public EntityManagerFactoryIT()
    {
		super();
	}


	@Test
	public void testEntityManagerFactory() throws Exception
    {
		LOG.info( "EntityManagerFactoryIT.testEntityManagerFactory" );

		assertNotNull( emf );
		LOG.info( emf.getImpementationDescription() );
	}
}
