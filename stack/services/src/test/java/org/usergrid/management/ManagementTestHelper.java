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
package org.usergrid.management;

import java.util.Properties;

import org.usergrid.persistence.EntityManagerFactory;

public interface ManagementTestHelper {

	public abstract javax.persistence.EntityManagerFactory getJpaEntityManagerFactory();

	public abstract void setJpaEntityManagerFactory(
			javax.persistence.EntityManagerFactory jpaEmf);

	public abstract EntityManagerFactory getEntityManagerFactory();

	public abstract void setEntityManagerFactory(EntityManagerFactory emf);

	public ManagementService getManagementService();

	public void setManagementService(ManagementService management);

	public abstract void setup() throws Exception;

	public abstract void teardown() throws Exception;

	public abstract Properties getProperties();

	public abstract void setProperties(Properties properties);

}
