package org.usergrid.android.client.callbacks;

import org.usergrid.android.client.Client.Query;

public interface QueryResultsCallback extends ClientCallback<Query> {

	public void onQueryResults(Query query);

}
