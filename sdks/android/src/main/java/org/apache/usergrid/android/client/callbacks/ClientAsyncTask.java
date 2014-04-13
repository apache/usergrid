/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.android.client.callbacks;

import android.os.AsyncTask;

public abstract class ClientAsyncTask<T> extends AsyncTask<Void, Exception, T> {

	ClientCallback<T> callback;

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
