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
import android.widget.EditText;
import android.widget.TextView;

import org.apache.usergrid.activityfeed.R;
import org.apache.usergrid.activityfeed.UsergridManager;
import org.apache.usergrid.activityfeed.helpers.ActionBarHelpers;
import org.apache.usergrid.activityfeed.helpers.AlertDialogHelpers;

public class CreateAccountActivity extends AppCompatActivity {

    private static final String actionBarTitle = "Create Account";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        ActionBarHelpers.setCustomViewForActionBarWithTitle(this,actionBarTitle);

        final EditText nameText = (EditText) findViewById(R.id.nameText);
        final EditText usernameEditText = (EditText) findViewById(R.id.usernameText);
        final EditText emailText = (EditText) findViewById(R.id.emailText);
        final EditText passwordEditText = (EditText) findViewById(R.id.passwordEditText);

        final TextView createAccountTextView = (TextView) findViewById(R.id.createAccountText);
        if( createAccountTextView != null ) {
            createAccountTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if( nameText != null && usernameEditText != null && emailText != null && passwordEditText != null ) {
                        String name = nameText.getText().toString();
                        String username = usernameEditText.getText().toString();
                        String email = emailText.getText().toString();
                        String password = passwordEditText.getText().toString();
                        if(!name.isEmpty() && !username.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                            UsergridManager.createUserAccount(CreateAccountActivity.this,name,username,email,password);
                        } else {
                            AlertDialogHelpers.showAlert(CreateAccountActivity.this,"Error Creating Account","All fields must not be empty.");
                        }
                    }

                }
            });
        }
    }
}
