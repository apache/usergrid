package org.apache.usergrid.android.client.callbacks;

import org.usergrid.java.client.Client.Query;

public interface QueryResultsCallback extends ClientCallback<Query> {

	public void onQueryResults(Query query);

}
