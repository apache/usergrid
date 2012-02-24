package org.usergrid.rest.applications.users;

import org.usergrid.rest.AbstractContextResource;

public abstract class AbstractUserExtensionResource extends
		AbstractContextResource {

	UserResource userResource;

	public AbstractUserExtensionResource() {
	}

	public AbstractUserExtensionResource init(UserResource userResource) {
		this.userResource = userResource;
		return this;
	}

	public UserResource getUserResource() {
		return userResource;
	}

}
