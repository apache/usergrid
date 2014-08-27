package org.apache.usergrid.android.sdk.callbacks;

import org.apache.usergrid.android.sdk.UGClient.Query;

/**
 * Callback for async requests using the Query interface
 * @see org.apache.usergrid.android.sdk.UGClient.Query
 * @see org.apache.usergrid.android.sdk.UGClient#queryActivityFeedForUserAsync(String, QueryResultsCallback)
 */
public interface QueryResultsCallback extends ClientCallback<Query> {

	public void onQueryResults(Query query);

}
