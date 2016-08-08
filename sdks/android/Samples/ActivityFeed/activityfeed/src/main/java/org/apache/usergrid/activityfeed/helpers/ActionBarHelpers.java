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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import org.apache.usergrid.activityfeed.R;

public final class ActionBarHelpers {
    private ActionBarHelpers() {}

    public static void setCustomViewForActionBarWithTitle(@NonNull final AppCompatActivity activity, @Nullable final String title) {
        ActionBarHelpers.setCustomViewForActionBarWithTitle(activity,title,null,null);
    }

    public static void setCustomViewForActionBarWithTitle(@NonNull final AppCompatActivity activity, @Nullable final String title, @Nullable final String rightButtonTitle, @Nullable final View.OnClickListener rightButtonOnClick) {
        ActionBar actionBar = activity.getSupportActionBar();
        if( actionBar != null ) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            View actionBarView = View.inflate(activity, R.layout.action_bar_layout,null);
            TextView actionBarTitleText = (TextView) actionBarView.findViewById(R.id.actionBarTitle);
            actionBarTitleText.setText(title);
            if( rightButtonTitle != null ) {
                TextView rightTextView = (TextView) actionBarView.findViewById(R.id.buttonTitle);
                rightTextView.setText(rightButtonTitle);
                rightTextView.setOnClickListener(rightButtonOnClick);
            }
            final ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.MATCH_PARENT, Gravity.CENTER);
            actionBar.setCustomView(actionBarView,params);
        }
    }

}
