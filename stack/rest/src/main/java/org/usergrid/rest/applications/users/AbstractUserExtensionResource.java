package org.usergrid.rest.applications.users;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.rest.AbstractContextResource;

@Component
@Scope("prototype")
public abstract class AbstractUserExtensionResource extends
		AbstractContextResource {

	UserResource userResource;

	public AbstractUserExtensionResource() {
	}

	@Override
	public void setParent(AbstractContextResource parent) {
		super.setParent(parent);
		if (parent instanceof UserResource) {
			this.userResource = (UserResource) parent;
		}
	}

	public UserResource getUserResource() {
		return userResource;
	}

}
