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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.usergrid.activityfeed.ActivityEntity;
import org.apache.usergrid.activityfeed.R;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.model.UsergridUser;

import java.util.List;

public class FeedAdapter extends BaseAdapter {
    private final List<ActivityEntity> feedMessages;
    private final Activity context;

    public FeedAdapter(Activity context, List<ActivityEntity> feedMessages) {
        this.context = context;
        this.feedMessages = feedMessages;
    }

    @Override
    public int getCount() {
        if (feedMessages != null) {
            return feedMessages.size();
        } else {
            return 0;
        }
    }

    @Override
    public ActivityEntity getItem(int position) {
        if (feedMessages != null) {
            return feedMessages.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        ActivityEntity messageEntity = getItem(position);
        if (convertView == null) {
            convertView = View.inflate(context,R.layout.message_layout,null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.txtMessage.setText(messageEntity.getContent());

        boolean isMe = false;
        String displayName = messageEntity.getDisplayName();
        if( displayName != null ) {
            final UsergridUser currentUser = Usergrid.getCurrentUser();
            if( currentUser != null ) {
                final String currentUserUsername = currentUser.getUsername();
                if( currentUserUsername != null && displayName.equalsIgnoreCase(currentUserUsername) ) {
                    isMe = true;
                }
            }
            holder.txtInfo.setText(displayName);
        }
        setAlignment(holder,isMe);
        return convertView;
    }

    public void add(ActivityEntity message) {
        feedMessages.add(message);
    }

    private void setAlignment(ViewHolder holder, boolean isMe) {
        int gravity;
        int drawableResourceId;
        if( !isMe ) {
            gravity = Gravity.END;
            drawableResourceId = R.drawable.in_message_bg;
        } else {
            gravity = Gravity.START;
            drawableResourceId = R.drawable.out_message_bg;
        }

        holder.contentWithBG.setBackgroundResource(drawableResourceId);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
        layoutParams.gravity = gravity;
        holder.contentWithBG.setLayoutParams(layoutParams);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
        if( !isMe ) {
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else {
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        }

        holder.content.setLayoutParams(lp);
        layoutParams = (LinearLayout.LayoutParams) holder.txtMessage.getLayoutParams();
        layoutParams.gravity = gravity;
        holder.txtMessage.setLayoutParams(layoutParams);

        layoutParams = (LinearLayout.LayoutParams) holder.txtInfo.getLayoutParams();
        layoutParams.gravity = gravity;
        holder.txtInfo.setLayoutParams(layoutParams);
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.txtMessage = (TextView) v.findViewById(R.id.txtMessage);
        holder.content = (LinearLayout) v.findViewById(R.id.content);
        holder.contentWithBG = (LinearLayout) v.findViewById(R.id.contentWithBackground);
        holder.txtInfo = (TextView) v.findViewById(R.id.txtInfo);
        return holder;
    }


    private static class ViewHolder {
        public TextView txtMessage;
        public TextView txtInfo;
        public LinearLayout content;
        public LinearLayout contentWithBG;
    }
}
