package org.usergrid.android.client.callbacks;

import java.util.Map;

import org.usergrid.android.client.entities.Group;

public interface GroupsRetrievedCallback extends
		ClientCallback<Map<String, Group>> {

	public void onGroupsRetrieved(Map<String, Group> groups);

}
