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

import java.util.Properties;

import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.cassandra.CassandraService;

public interface PersistenceTestHelper {

	public abstract EntityManagerFactory getEntityManagerFactory();

	public abstract void setEntityManagerFactory(EntityManagerFactory emf);

	public abstract QueueManagerFactory getMessageManagerFactory();

	public abstract void setMessageManagerFactory(QueueManagerFactory mmf);

	public abstract Properties getProperties();

	public abstract void setProperties(Properties properties);

	public abstract void setup() throws Exception;

	public abstract void teardown() throws Exception;

	public abstract void setCassandraService(CassandraService cassandraService);

	public abstract CassandraService getCassandraService();

}
