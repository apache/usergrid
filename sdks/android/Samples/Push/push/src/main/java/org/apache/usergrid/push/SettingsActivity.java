package org.apache.usergrid.push;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.apache.usergrid.java.client.Usergrid;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final EditText orgIdEditText = (EditText) findViewById(R.id.orgId);
        if( orgIdEditText != null ) {
            orgIdEditText.setText(Usergrid.getOrgId());
        }
        final EditText appIdEditText = (EditText) findViewById(R.id.appId);
        if( appIdEditText != null ) {
            appIdEditText.setText(Usergrid.getAppId());
        }
        final EditText urlEditText = (EditText) findViewById(R.id.url);
        if( urlEditText != null ) {
            urlEditText.setText(Usergrid.getBaseUrl());
        }
        final EditText notifierIdEditText = (EditText) findViewById(R.id.notifierId);
        if( notifierIdEditText != null ) {
            notifierIdEditText.setText(MainActivity.NOTIFIER_ID);
        }

        final Button saveButton = (Button) findViewById(R.id.saveButton);
        if( saveButton != null ) {
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if( orgIdEditText != null ) {
                        MainActivity.ORG_ID = orgIdEditText.getText().toString();
                    }
                    if( appIdEditText != null ) {
                        MainActivity.APP_ID = appIdEditText.getText().toString();
                    }
                    if( urlEditText != null ) {
                        MainActivity.BASE_URL = urlEditText.getText().toString();
                    }
                    if( notifierIdEditText != null ) {
                        MainActivity.NOTIFIER_ID = notifierIdEditText.getText().toString();
                    }
                    MainActivity.USERGRID_PREFS_NEEDS_REFRESH = true;
                    SettingsActivity.this.finish();
                }
            });
        }

        final Button cancelButton = (Button) findViewById(R.id.cancelButton);
        if( cancelButton != null ) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsActivity.this.finish();
                }
            });
        }
    }
}
