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
package org.apache.usergrid.android.tasks;

import android.os.AsyncTask;

import org.apache.usergrid.android.callbacks.UsergridResponseCallback;

import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;

public abstract class UsergridAsyncTask extends AsyncTask<Void, Void, UsergridResponse> {

    @NotNull private final UsergridResponseCallback responseCallback;

    public UsergridAsyncTask(@NotNull final UsergridResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
    }

    @Override @NotNull
    protected UsergridResponse doInBackground(final Void... v) {
        return doTask();
    }

    public abstract UsergridResponse doTask();

    @Override
    protected void onPostExecute(@NotNull final UsergridResponse response) {
        this.responseCallback.onResponse(response);
    }
}
