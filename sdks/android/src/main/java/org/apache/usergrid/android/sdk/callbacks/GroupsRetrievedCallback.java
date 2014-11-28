package org.apache.usergrid.android.sdk.callbacks;

import java.util.Map;

import org.apache.usergrid.android.sdk.entities.Group;

/**
 * Callback for GET requests on groups entities
 * @see org.apache.usergrid.android.sdk.UGClient#getGroupsForUserAsync(String,GroupsRetrievedCallback)
 */
public interface GroupsRetrievedCallback extends
		ClientCallback<Map<String, Group>> {

	public void onGroupsRetrieved(Map<String, Group> groups);

}
