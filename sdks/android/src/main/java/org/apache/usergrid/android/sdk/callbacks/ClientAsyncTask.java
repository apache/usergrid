package org.apache.usergrid.android.sdk.callbacks;

import android.os.AsyncTask;

/**
 * Default async request handler
 */
public abstract class ClientAsyncTask<T> extends AsyncTask<Void, Exception, T> {

	ClientCallback<T> callback;

	/**
	 * Default constructor for starting an async request
	 */
	public ClientAsyncTask(ClientCallback<T> callback) {
		this.callback = callback;
	}

	@Override
	protected T doInBackground(Void... v) {
		try {
			return doTask();
		} catch (Exception e) {
			this.publishProgress(e);
		}
		return null;
	}

	/**
	 * Starts the async request
	 */
	public abstract T doTask();

	@Override
	protected void onPostExecute(T response) {
		if (callback != null) {
			callback.onResponse(response);
		}
	}

	@Override
	protected void onProgressUpdate(Exception... e) {
		if ((callback != null) && (e != null) && (e.length > 0)) {
			callback.onException(e[0]);
		}
	}
}
