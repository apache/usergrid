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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.apache.usergrid.activityfeed.ActivityEntity;
import org.apache.usergrid.activityfeed.R;
import org.apache.usergrid.activityfeed.UsergridManager;
import org.apache.usergrid.activityfeed.callbacks.GetFeedMessagesCallback;
import org.apache.usergrid.activityfeed.callbacks.PostFeedMessageCallback;
import org.apache.usergrid.activityfeed.helpers.ActionBarHelpers;
import org.apache.usergrid.activityfeed.helpers.FeedAdapter;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.model.UsergridUser;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private EditText messageET;
    private ListView messagesContainer;
    private FeedAdapter adapter;
    private ArrayList<ActivityEntity> feedMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        final UsergridUser currentUser = Usergrid.getCurrentUser();
        String username = "Unknown";
        if( currentUser != null ) {
            String currentUsername = currentUser.getUsername();
            if( currentUsername != null ) {
                username = currentUser.getUsername();
            }
        }
        final Intent followActivityIntent = new Intent(this,FollowActivity.class);
        ActionBarHelpers.setCustomViewForActionBarWithTitle(this, username + "'s feed", "Follow", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeedActivity.this.startActivity(followActivityIntent);
            }
        });

        initControls();
    }

    private void initControls() {
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        messageET = (EditText) findViewById(R.id.messageEdit);
        if(messageET != null) {
            messageET.setMaxWidth(messageET.getWidth());
        }
        final Button sendBtn = (Button) findViewById(R.id.chatSendButton);
        if( sendBtn != null ) {
            sendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String messageText = messageET.getText().toString();
                    if (TextUtils.isEmpty(messageText)) {
                        return;
                    }

                    UsergridManager.postFeedMessage(messageText, new PostFeedMessageCallback() {
                        @Override
                        public void onSuccess(@NonNull ActivityEntity activityEntity) {
                            displayMessage(activityEntity);
                        }
                    });
                    messageET.setText("");
                }
            });
        }
    }

    private void displayMessage(ActivityEntity message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    @SuppressWarnings("unchecked")
    private void loadMessages(){

        feedMessages = new ArrayList<>();
        adapter = new FeedAdapter(FeedActivity.this, new ArrayList<ActivityEntity>());
        messagesContainer.setAdapter(adapter);

        UsergridManager.getFeedMessages(new GetFeedMessagesCallback() {
            @Override
            public void onResponse(@NonNull List<ActivityEntity> feedMessages) {
                FeedActivity.this.feedMessages.addAll(feedMessages);
                for( ActivityEntity activityEntity : FeedActivity.this.feedMessages ) {
                    displayMessage(activityEntity);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        this.loadMessages();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if( Usergrid.getCurrentUser() != null  ) {
            UsergridManager.logoutCurrentUser(this);
        }
        super.onDestroy();
    }
}
