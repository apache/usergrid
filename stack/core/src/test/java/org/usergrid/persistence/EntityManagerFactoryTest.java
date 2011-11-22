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
package org.usergrid.persistence;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityManagerFactoryTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(EntityManagerFactoryTest.class);

	public EntityManagerFactoryTest() {
		super();
	}

	@Test
	public void testEntityManagerFactory() throws Exception {
		logger.info("EntityManagerFactoryTest.testEntityManagerFactory");

		assertNotNull(emf);
		logger.info(emf.getImpementationDescription());

	}

}
