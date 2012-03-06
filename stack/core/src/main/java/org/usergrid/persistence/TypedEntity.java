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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * TypedEntity is the abstract superclass for all typed entities. A typed entity
 * refers to an entity that has a concrete Java class mapped to it. Entities do
 * not need to have concrete typed classes, the service interacts with entities
 * in an entirely dynamic fashion and uses the Schema class to determine
 * relationships and property types, however using the typed entity classes can
 * be more convenient.
 * 
 * @author edanuff
 */
@XmlRootElement
public abstract class TypedEntity extends AbstractEntity {

	@Override
	public Entity toTypedEntity() {
		return this;
	}
}
