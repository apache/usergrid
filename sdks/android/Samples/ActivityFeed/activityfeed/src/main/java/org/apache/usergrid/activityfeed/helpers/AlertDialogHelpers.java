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
package org.apache.usergrid.activityfeed.helpers;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import org.apache.usergrid.activityfeed.R;

@SuppressWarnings("unused")
public final class AlertDialogHelpers {

    private AlertDialogHelpers() {}

    public static void showAlert(@NonNull final Activity activity, @Nullable final String title, @Nullable final String message) {
        AlertDialogHelpers.showAlert(activity,title,message,null);
    }

    public static void showAlert(@NonNull final Activity activity, @Nullable final String title, @Nullable final String message, @Nullable final DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .show();
    }

    public static void showScrollableAlert(@NonNull final Activity activity, @Nullable final String title, @Nullable final String message) {
        AlertDialogHelpers.showScrollableAlert(activity, title, message,null);
    }

    public static void showScrollableAlert(@NonNull final Activity activity, @Nullable final String title, @Nullable final String message, @Nullable final DialogInterface.OnClickListener onClickListener) {
        final View scrollableAlertView = View.inflate(activity, R.layout.scrollable_alert_view, null);
        final TextView titleTextView = (TextView) scrollableAlertView.findViewById(R.id.scrollableAlertTitle);
        titleTextView.setText(title);
        final TextView messageTextView = (TextView) scrollableAlertView.findViewById(R.id.scrollableAlertMessage);
        messageTextView.setText(message);
        new AlertDialog.Builder(activity)
                .setView(scrollableAlertView)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .show();
    }
}
