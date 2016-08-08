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
package org.apache.usergrid.activityfeed.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.apache.usergrid.activityfeed.R;
import org.apache.usergrid.activityfeed.UsergridManager;
import org.apache.usergrid.activityfeed.helpers.ActionBarHelpers;
import org.apache.usergrid.activityfeed.helpers.AlertDialogHelpers;

public class FollowActivity extends AppCompatActivity {

    private static final String actionBarTitle = "Follow";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        ActionBarHelpers.setCustomViewForActionBarWithTitle(this,actionBarTitle);

        final EditText usernameEditText = (EditText) findViewById(R.id.followUsernameText);
        final Button addFollowerButton = (Button) findViewById(R.id.addFollowerButton);
        if( addFollowerButton != null ) {
            addFollowerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if( usernameEditText != null ) {
                        final String username = usernameEditText.getText().toString();
                        if( !username.isEmpty() ) {
                            UsergridManager.followUser(FollowActivity.this,username);
                        } else {
                            AlertDialogHelpers.showAlert(FollowActivity.this,"Error Following User","Please enter a valid username.");
                        }
                    }
                }
            });
        }
    }
}
