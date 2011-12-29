package org.usergrid.rest.applications.users;

import org.usergrid.rest.AbstractContextResource;

public abstract class AbstractUserExtensionResource extends
		AbstractContextResource {

	UserResource userResource;

	public AbstractUserExtensionResource(UserResource userResource)
			throws Exception {
		super(userResource);
		this.userResource = userResource;
	}

	public UserResource getUserResource() {
		return userResource;
	}

}
