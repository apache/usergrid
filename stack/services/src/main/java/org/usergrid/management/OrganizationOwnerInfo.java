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

public class OrganizationOwnerInfo {

	UserInfo owner;
	OrganizationInfo organization;

	public OrganizationOwnerInfo() {
	}

	public OrganizationOwnerInfo(UserInfo owner, OrganizationInfo organization) {
		this.owner = owner;
		this.organization = organization;
	}

	public UserInfo getOwner() {
		return owner;
	}

	public void setOwner(UserInfo owner) {
		this.owner = owner;
	}

	public OrganizationInfo getOrganization() {
		return organization;
	}

	public void setOrganization(OrganizationInfo organization) {
		this.organization = organization;
	}

}
