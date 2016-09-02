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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.usergrid.activityfeed.R;
import org.apache.usergrid.activityfeed.UsergridManager;
import org.apache.usergrid.activityfeed.helpers.AlertDialogHelpers;
import org.apache.usergrid.java.client.Usergrid;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UsergridManager.initializeSharedInstance(this);

        final EditText usernameEditText = (EditText) findViewById(R.id.usernameText);
        if( usernameEditText != null ) {
            usernameEditText.setSelection(usernameEditText.getText().length());
        }
        final EditText passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        if( passwordEditText != null ) {
            passwordEditText.setSelection(passwordEditText.getText().length());
        }

        final Button signInButton = (Button) findViewById(R.id.signInButton);
        if( signInButton != null ) {
            signInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if( usernameEditText != null && passwordEditText != null ) {
                        final String username = usernameEditText.getText().toString();
                        final String password = passwordEditText.getText().toString();
                        if( !username.isEmpty() && !password.isEmpty() ) {
                            UsergridManager.loginUser(MainActivity.this,username,password);
                        } else {
                            AlertDialogHelpers.showAlert(MainActivity.this,"Error Authenticating User","Username and password must not be empty.");
                        }
                    }
                }
            });
        }

        final TextView createAccountTextView = (TextView) findViewById(R.id.createAccountTextView);
        if( createAccountTextView != null ) {
            final Intent createAccountIntent = new Intent(this,CreateAccountActivity.class);
            createAccountTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.startActivity(createAccountIntent);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        if(Usergrid.getCurrentUser() != null) {
            this.startActivity(new Intent(this,FeedActivity.class));
        }
        super.onResume();
    }
}
